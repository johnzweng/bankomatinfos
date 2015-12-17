package at.zweng.bankomatinfos.iso7816emv;

import static at.zweng.bankomatinfos.iso7816emv.EmvUtils.APPLICATION_ID_EMV_MAESTRO_BANKOMAT;
import static at.zweng.bankomatinfos.iso7816emv.EmvUtils.APPLICATION_ID_EMV_MASTERCARD;
import static at.zweng.bankomatinfos.iso7816emv.EmvUtils.APPLICATION_ID_EMV_VISA_CREDITCARD;
import static at.zweng.bankomatinfos.iso7816emv.EmvUtils.APPLICATION_ID_QUICK;
import static at.zweng.bankomatinfos.iso7816emv.EmvUtils.*;
import static at.zweng.bankomatinfos.iso7816emv.EmvUtils.EMV_COMMAND_GET_DATA_ALL_COMMON_SIMPLE_TLV;
import static at.zweng.bankomatinfos.iso7816emv.EmvUtils.EMV_COMMAND_GET_DATA_APP_TX_COUNTER;
import static at.zweng.bankomatinfos.iso7816emv.EmvUtils.EMV_COMMAND_GET_DATA_CRM_COUNTRY;
import static at.zweng.bankomatinfos.iso7816emv.EmvUtils.EMV_COMMAND_GET_DATA_CRM_CURRENCY;
import static at.zweng.bankomatinfos.iso7816emv.EmvUtils.EMV_COMMAND_GET_DATA_LAST_ONLINE_APP_TX_COUNTER;
import static at.zweng.bankomatinfos.iso7816emv.EmvUtils.EMV_COMMAND_GET_DATA_LOG_FORMAT;
import static at.zweng.bankomatinfos.iso7816emv.EmvUtils.EMV_COMMAND_GET_DATA_LOWER_CONSECUTIVE_OFFLINE_LIMIT;
import static at.zweng.bankomatinfos.iso7816emv.EmvUtils.EMV_COMMAND_GET_DATA_LOWER_CUMULATIVE_TX_AMOUNT;
import static at.zweng.bankomatinfos.iso7816emv.EmvUtils.EMV_COMMAND_GET_DATA_PIN_RETRY_COUNTER;
import static at.zweng.bankomatinfos.iso7816emv.EmvUtils.EMV_COMMAND_GET_DATA_UPPER_CONSECUTIVE_OFFLINE_LIMIT;
import static at.zweng.bankomatinfos.iso7816emv.EmvUtils.EMV_COMMAND_GET_DATA_UPPER_CUMULATIVE_TX_AMOUNT;
import static at.zweng.bankomatinfos.iso7816emv.EmvUtils.ISO_COMMAND_QUICK_READ_BALANCE;
import static at.zweng.bankomatinfos.iso7816emv.EmvUtils.ISO_COMMAND_QUICK_READ_CURRENCY;
import static at.zweng.bankomatinfos.iso7816emv.EmvUtils.createApduVerifyPIN;
import static at.zweng.bankomatinfos.iso7816emv.EmvUtils.createReadRecordApdu;
import static at.zweng.bankomatinfos.iso7816emv.EmvUtils.createSelectAid;
import static at.zweng.bankomatinfos.iso7816emv.EmvUtils.createSelectMasterFile;
import static at.zweng.bankomatinfos.iso7816emv.EmvUtils.createSelectParentDfFile;
import static at.zweng.bankomatinfos.iso7816emv.EmvUtils.filterTagsForResult;
import static at.zweng.bankomatinfos.iso7816emv.EmvUtils.getAmountFromBcdBytes;
import static at.zweng.bankomatinfos.iso7816emv.EmvUtils.getAmountFromBytes;
import static at.zweng.bankomatinfos.iso7816emv.EmvUtils.getDateFromBcdBytes;
import static at.zweng.bankomatinfos.iso7816emv.EmvUtils.getNextTLV;
import static at.zweng.bankomatinfos.iso7816emv.EmvUtils.getTagsFromBerTlvAPDUResponse;
import static at.zweng.bankomatinfos.iso7816emv.EmvUtils.getTimeStampFromBcdBytes;
import static at.zweng.bankomatinfos.iso7816emv.EmvUtils.getTimeStampFromQuickLog;
import static at.zweng.bankomatinfos.iso7816emv.EmvUtils.isStatusSuccess;
import static at.zweng.bankomatinfos.iso7816emv.EmvUtils.prettyPrintBerTlvAPDUResponse;
import static at.zweng.bankomatinfos.iso7816emv.EmvUtils.statusToString;
import static at.zweng.bankomatinfos.util.Utils.TAG;
import static at.zweng.bankomatinfos.util.Utils.byteArrayToInt;
import static at.zweng.bankomatinfos.util.Utils.bytesToHex;
import static at.zweng.bankomatinfos.util.Utils.cutoffLast2Bytes;
import static at.zweng.bankomatinfos.util.Utils.fromHexString;
import static at.zweng.bankomatinfos.util.Utils.getByteArrayPart;
import static at.zweng.bankomatinfos.util.Utils.getLast2Bytes;
import static at.zweng.bankomatinfos.util.Utils.prettyPrintString;
import static at.zweng.bankomatinfos.util.Utils.readBcdIntegerFromBytes;
import static at.zweng.bankomatinfos.util.Utils.readLongFromBytes;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import android.content.Context;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.util.Log;
import at.zweng.bankomatinfos.AppController;
import at.zweng.bankomatinfos.exceptions.NoSmartCardException;
import at.zweng.bankomatinfos.exceptions.TlvParsingException;
import at.zweng.bankomatinfos.model.CardInfo;
import at.zweng.bankomatinfos.model.EmvTransactionLogEntry;
import at.zweng.bankomatinfos.model.InfoKeyValuePair;
import at.zweng.bankomatinfos.model.QuickTransactionLogEntry;
import at.zweng.bankomatinfos2.R;

/**
 * Performs all the reading operations on a card.
 * 
 * @author Johannes Zweng <johannes@zweng.at>
 */
public class NfcBankomatCardReader {
	private ITag _tag;
	private AppController _ctl;
	private List<TagAndValue> _tagList;
	private Context _ctx;

	// 9F 4F - 18 bytes: Log Format
	// 9F 36 (02 bytes) -> Application Transaction Counter (ATC)
	// 9F 02 (06 bytes) -> Amount, Authorised (Numeric)
	// 9F 03 (06 bytes) -> Amount, Other (Numeric)
	// 9F 1A (02 bytes) -> Terminal Country Code
	// 95 (05 bytes) -> Terminal Verification Results (TVR)
	// 5F 2A (02 bytes) -> Transaction Currency Code
	// 9A (03 bytes) -> Transaction Date
	// 9C (01 bytes) -> Transaction Type
	// 9F 80 04 (04 bytes) -> [UNHANDLED TAG]
	private static final String LOG_FORMAT_VISA = "9F4F189F36029F02069F03069F1A0295055F2A029A039C019F80049000";
	private static final int LOG_LENGTH_VISA = 33;

	// 9F 4F - 11 bytes: Log Format
	// 9F 27 (01 bytes) -> Cryptogram Information Data
	// 9F 02 (06 bytes) -> Amount, Authorised (Numeric)
	// 5F 2A (02 bytes) -> Transaction Currency Code
	// 9A (03 bytes) -> Transaction Date
	// 9F 36 (02 bytes) -> Application Transaction Counter (ATC)
	// 9F 52 (06 bytes) -> Application Default Action (ADA)
	// total length: 20 bytes
	private static final String LOG_FORMAT_MASTERCARD = "9F4F119F27019F02065F2A029A039F36029F52069000";
	private static final int LOG_LENGTH_MASTERCARD = 22;

	// 9F 4F - 1A bytes: Log Format
	// 9F 27 (01 bytes) -> Cryptogram Information Data
	// 9F 02 (06 bytes) -> Amount, Authorised (Numeric)
	// 5F 2A (02 bytes) -> Transaction Currency Code
	// 9A (03 bytes) -> Transaction Date
	// 9F 36 (02 bytes) -> Application Transaction Counter (ATC)
	// 9F 52 (06 bytes) -> Application Default Action (ADA)
	// DF 3E (01 bytes) -> [UNHANDLED TAG]
	// 9F 21 (03 bytes) -> Transaction Time (HHMMSS)
	// 9F 7C (0x14 bytes) -> Customer Exclusive Data
	// total length: 44 bytes
	private static final String LOG_FORMAT_BANKOMAT_AUSTRIA = "9F4F1A9F27019F02065F2A029A039F36029F5206DF3E019F21039F7C149000";
	private static final int LOG_LENGTH_BANKOMAT_AUSTRIA = 46;

	private static final int LOG_LENGTH_QUICK = 35;

	// until now on all cards I've seen which head a tx log, they were stored on
	// EF11
	// we also cannot rely on cards Log Entry tag, as some cards don't contain
	// this tag
	// but still have logs in EF11
	private static final int LOG_RECORD_EF = 11;

	// FIXME: dynamic parsing of log entries, not static pattern comparison
	private String _logFormatResponse;

	/**
	 * Constructor
	 * 
	 * @param _nfcTag
	 */
	public NfcBankomatCardReader(ITag tag, Context ctx) {
		super();
		this._tag = tag;
		this._ctl = AppController.getInstance();
		this._tagList = new ArrayList<TagAndValue>();
		this._ctx = ctx;
	}

	/**
	 * Connects to IsoDep
	 * 
	 * @throws IOException
	 */
	public void connect() throws IOException {
		_tag.connect();
	}

	/**
	 * Disconnects to IsoDep
	 * 
	 * @throws IOException
	 */
	public void disconnect() throws IOException {
		_tag.close();
	}

	/**
	 * Try to read all bankomat card data<br>
	 * 
	 * @param performFullFileScan
	 *            <code>true</code> if we should try to scan all EFs, false if
	 *            only some well known on Austrian Bankomat Cards
	 * 
	 * @return
	 * @throws IOException
	 */
	public CardInfo readAllCardData(boolean performFullFileScan) throws IOException {
		CardInfo result = new CardInfo(_ctx);
		_ctl.log("Starting to read data from card..");
		// we don't have this info over omapi:
		if (!(_tag instanceof OmapiSessionTag)) {
			result.addSectionHeader(_ctx.getResources().getString(R.string.section_nfc));
			result.setNfcTagId(_tag.getId());
			_ctl.log("NFC Tag ID: " + prettyPrintString(bytesToHex(_tag.getId()), 2));
			_ctl.log("Historical bytes: " + prettyPrintString(bytesToHex(_tag.getHistoricalBytes()), 2));
			result.addSectionHeader(_ctx.getResources().getString(R.string.section_GPCS_CPLC));
			result = readCPLCInfos(result);
		}
		result.addSectionHeader(_ctx.getResources().getString(R.string.section_emv));
		result = readQuickInfos(result);
		result = readMaestroCardInfos(result, performFullFileScan);
		result = readVisaCardInfos(result, performFullFileScan);
		result = readMastercardInfos(result, performFullFileScan);
		_ctl.log("FINISHED! :-)");
		return result;
	}

	/**
	 * Try to read generic infos about the SmartCard as defined in the
	 * "GlobalPlatform Card Specification" (GPCS).
	 * 
	 * @param result
	 * @return
	 * @throws IOException
	 * @throws TlvParsingException
	 */
	private CardInfo readCPLCInfos(CardInfo result) throws IOException {
		_ctl.log("Trying to read Card Production Life Cycle (CPLC) data as "
				+ "defined by GlobalPlatform Card Specification (GPCS)..");
		byte[] resultPdu = sendGetCPLC();
		// if not success, abort here
		if (!isStatusSuccess(getLast2Bytes(resultPdu))) {
			return result;
		}
		if (resultPdu.length <= 2) {
			return result;
		}

		try {
			CPLC cplcData = CPLC.parse(cutoffLast2Bytes(resultPdu));
			String cplcString = cplcData.toString();
			_ctl.log(cplcString);
			Log.d(TAG, "CPLC data: " + cplcString);

			Pattern p = Pattern.compile("^0+$");
			Map<String, String> fields = cplcData.getFields();
			_ctl.log("Same date human readable parsed:");
			for (String key : fields.keySet()) {
				String val = fields.get(key);
				if (p.matcher(val).matches()) {
					// ignore fields which are just 000s
					continue;
				}
				String humanReadableVal = CPLC.getHumanReadableValue(key, val);
				_ctl.log("  * " + key + ":\n    " + humanReadableVal);
				result.addKeyValuePair(new InfoKeyValuePair(key, humanReadableVal));
			}
		} catch (TlvParsingException pe) {
			_ctl.log("ERROR: Catched Exception while reading CPLC data:\n" + pe + "\n" + pe.getMessage());
			Log.w(TAG, "Catched Exception while reading CPLC infos: ", pe);
		} catch (RuntimeException re) {
			_ctl.log("ERROR: Catched Exception while reading CPLC infos:\n" + re + "\n" + re.getMessage());
			Log.w(TAG, "Catched Exception while reading CPLC infos: ", re);
		}
		return result;
	}

	/**
	 * Read QUICK card infos from card
	 * 
	 * @param result
	 * @throws IOException
	 */
	private CardInfo readQuickInfos(CardInfo result) throws IOException {
		Log.d(TAG, "check if card contains QUICK AID..");
		_ctl.log("Trying to select QUICK AID..");
		byte[] selectAidResponse = selectApplicationGetBytes(APPLICATION_ID_QUICK);
		boolean isQuickCard = isStatusSuccess(getLast2Bytes(selectAidResponse));
		_ctl.log("is a Quick card: " + isQuickCard);
		result.setQuickCard(isQuickCard);
		if (!isQuickCard) {
			return result;
		}
		// ok, so let's catch exceptions here, instead of just letting the whole
		// scan abort, so that the user gets at least some infos where the
		// parsing failed:
		try {
			result.setQuickBalance(getQuickCardBalance());
			result.setQuickCurrency(Iso4217CurrencyCodes.getCurrencyAsString(getQuickCardCurrencyBytes()));
			result = tryReadingQuickLogEntries(result);
		} catch (TlvParsingException pe) {
			_ctl.log("ERROR: Catched Exception while reading QUICK infos:\n" + pe + "\n" + pe.getMessage());
			Log.w(TAG, "Catched Exception while reading QUICK infos: ", pe);
		} catch (RuntimeException re) {
			_ctl.log("ERROR: Catched Exception while reading QUICK infos:\n" + re + "\n" + re.getMessage());
			Log.w(TAG, "Catched Exception while reading QUICK infos: ", re);
		}
		return result;
	}

	/**
	 * Read MAESTRO card infos from card
	 * 
	 * @param result
	 * @param fullFileScan
	 *            <code>true</code> if we should try to iterate over all EFs,
	 *            false if only some
	 * @throws IOException
	 */
	private CardInfo readMaestroCardInfos(CardInfo result, boolean fullFileScan) throws IOException {
		Log.d(TAG, "check if card contains MAESTRO AID..");
		_ctl.log("Trying to select Maestro AID..");
		byte[] selectAidResponse = selectApplicationGetBytes(APPLICATION_ID_EMV_MAESTRO_BANKOMAT);
		logBerTlvResponse(selectAidResponse);
		boolean isMaestroCard = isStatusSuccess(getLast2Bytes(selectAidResponse));
		_ctl.log("is a MAESTRO card: " + isMaestroCard);
		result.setMaestroCard(isMaestroCard);
		if (!isMaestroCard) {
			return result;
		}
		// ok, so let's catch exceptions here, instead of just letting the whole
		// scan abort, so that the user gets at least some infos where the
		// parsing failed:
		try {
			result = readEmvData(selectAidResponse, result, fullFileScan);
		} catch (RuntimeException re) {
			_ctl.log("ERROR: Catched Exception while reading Maestro infos:\n" + re + "\n" + re.getMessage());
			Log.w(TAG, "Catched Exception while reading Maestro infos: ", re);
		} catch (TlvParsingException tle) {
			_ctl.log("ERROR: Catched Exception while reading Maestro infos:\n" + tle + "\n" + tle.getMessage());
			Log.w(TAG, "Catched Exception while reading Maestro infos: ", tle);
		}
		return result;
	}

	/**
	 * Read Mastercard infos from card
	 * 
	 * @param result
	 * @param fullFileScan
	 *            <code>true</code> if we should try to iterate over all EFs,
	 *            false if only some
	 * @throws IOException
	 */
	private CardInfo readMastercardInfos(CardInfo result, boolean fullFileScan) throws IOException {
		Log.d(TAG, "check if card contains Mastercard Creditcard AID..");
		_ctl.log("Trying to select Mastercard Creditcard AID..");
		byte[] selectAidResponse = selectApplicationGetBytes(APPLICATION_ID_EMV_MASTERCARD);
		logBerTlvResponse(selectAidResponse);
		boolean isMastercard = isStatusSuccess(getLast2Bytes(selectAidResponse));
		_ctl.log("is a Mastercard Creditcard: " + isMastercard);
		result.setMasterCard(isMastercard);
		if (!isMastercard) {
			return result;
		}
		// ok, so let's catch exceptions here, instead of just letting the whole
		// scan abort, so that the user gets at least some infos where the
		// parsing failed:
		try {
			result = readEmvData(selectAidResponse, result, fullFileScan);
		} catch (RuntimeException re) {
			_ctl.log("ERROR: Catched Exception while reading mastercard infos:\n" + re + "\n" + re.getMessage());
			Log.w(TAG, "Catched Exception while reading mastercard  infos: ", re);
		} catch (TlvParsingException tle) {
			_ctl.log("ERROR: Catched Exception while reading mastercard  infos:\n" + tle + "\n" + tle.getMessage());
			Log.w(TAG, "Catched Exception while reading mastercard  infos: ", tle);
		}
		return result;
	}

	/**
	 * Read VISA card infos from card
	 * 
	 * @param result
	 * @param fullFileScan
	 *            <code>true</code> if we should try to iterate over all EFs,
	 *            false if only some
	 * @throws IOException
	 */
	private CardInfo readVisaCardInfos(CardInfo result, boolean fullFileScan) throws IOException {
		Log.d(TAG, "check if card contains VISA Creditcard AID..");
		_ctl.log("Trying to select VISA Creditcard AID..");
		byte[] selectAidResponse = selectApplicationGetBytes(APPLICATION_ID_EMV_VISA_CREDITCARD);
		logBerTlvResponse(selectAidResponse);
		boolean isVisaCard = isStatusSuccess(getLast2Bytes(selectAidResponse));
		_ctl.log("is a VISA Creditcard: " + isVisaCard);
		result.setVisaCard(isVisaCard);
		if (!isVisaCard) {
			return result;
		}
		// ok, so let's catch exceptions here, instead of just letting the whole
		// scan abort, so that the user gets at least some infos where the
		// parsing failed:
		try {
			result = readEmvData(selectAidResponse, result, fullFileScan);
		} catch (RuntimeException re) {
			_ctl.log("ERROR: Catched Exception while reading VISA card infos:\n" + re + "\n" + re.getMessage());
			Log.w(TAG, "Catched Exception while reading VISA card infos: ", re);
		} catch (TlvParsingException tle) {
			_ctl.log("ERROR: Catched Exception while reading VISA card infos:\n" + tle + "\n" + tle.getMessage());
			Log.w(TAG, "Catched Exception while reading VISA card infos: ", tle);
		}
		return result;
	}

	/**
	 * Try to read some EMV data
	 * 
	 * @param selectAidResponse
	 * @param result
	 * @param fullFileScan
	 *            <code>true</code> if we should try to iterate over all EFs,
	 *            false if only some well known on Austrian Bankomat Cards
	 * @return
	 * @throws IOException
	 * @throws TlvParsingException
	 */
	private CardInfo readEmvData(byte[] selectAidResponse, CardInfo result, boolean fullFileScan)
			throws IOException, TlvParsingException {
		tryToReadLogFormat();
		result = tryToReadPinRetryCounter(result);
		tryToReadCurrentAtcValue();
		tryToReadLastOnlineAtcRegisterValue();
		tryToReadAllCommonSimpleTlvTags();
		tryToReadAllCommonBerTlvTags();
		tryToReadAdditionalGetDataFields();
		result = searchForFiles(result, fullFileScan, true);
		result.addKeyValuePairs(filterTagsForResult(_ctx, _tagList, false));
		result = lookForLogEntryEmvTag(result);
		return result;
	}

	/**
	 * Try to send command for reading LOG FORMAT tag
	 * 
	 * @throws IOException
	 */
	private void tryToReadLogFormat() throws IOException {
		_ctl.log("trying to send GET DATA to get 'Log Format' tag from  card...");
		_ctl.log("sent: " + bytesToHex(EMV_COMMAND_GET_DATA_PIN_RETRY_COUNTER));
		byte[] resultPdu = _tag.transceive(EMV_COMMAND_GET_DATA_LOG_FORMAT);
		_logFormatResponse = bytesToHex(resultPdu);
		logResultPdu(resultPdu);
		logBerTlvResponse(resultPdu);
	}

	/**
	 * Try to send command for reading PIN RETRY counter
	 * 
	 * @throws IOException
	 * @throws TlvParsingException
	 */
	private CardInfo tryToReadPinRetryCounter(CardInfo result) throws IOException, TlvParsingException {
		_ctl.log("trying to read PIN retry counter from card...");
		_ctl.log("sent: " + bytesToHex(EMV_COMMAND_GET_DATA_PIN_RETRY_COUNTER));
		byte[] resultPdu = _tag.transceive(EMV_COMMAND_GET_DATA_PIN_RETRY_COUNTER);
		logResultPdu(resultPdu);
		logBerTlvResponse(resultPdu);
		if (isStatusSuccess(getLast2Bytes(resultPdu))) {
			BERTLV tlv = getNextTLV(new ByteArrayInputStream(resultPdu));
			int pinRetryCounter = tlv.getValueBytes()[0];
			_ctl.log("-----------------------------------------------------");
			_ctl.log("  Current PIN retry counter: >>>>>> " + pinRetryCounter + " <<<<<<");
			_ctl.log("-----------------------------------------------------");
			result.setPinRetryCounter(pinRetryCounter);
		}
		return result;
	}

	/**
	 * Tries to send some additional GET DATA commands.
	 * 
	 * @param result
	 * @return
	 * @throws IOException
	 * @throws TlvParsingException
	 */
	private void tryToReadAdditionalGetDataFields() throws IOException, TlvParsingException {
		_ctl.log("trying to send GET DATA for getting 'card risk management currency(?)'...");
		_ctl.log("sent: " + bytesToHex(EMV_COMMAND_GET_DATA_CRM_CURRENCY));
		byte[] resultPdu = _tag.transceive(EMV_COMMAND_GET_DATA_CRM_CURRENCY);
		logResultPdu(resultPdu);
		logBerTlvResponse(resultPdu);

		_ctl.log("trying to send GET DATA for getting 'card risk management country(?)'...");
		_ctl.log("sent: " + bytesToHex(EMV_COMMAND_GET_DATA_CRM_COUNTRY));
		resultPdu = _tag.transceive(EMV_COMMAND_GET_DATA_CRM_COUNTRY);
		logResultPdu(resultPdu);
		logBerTlvResponse(resultPdu);

		_ctl.log("trying to send GET DATA for getting 'lower consecutive offline limit(?)'...");
		_ctl.log("sent: " + bytesToHex(EMV_COMMAND_GET_DATA_LOWER_CONSECUTIVE_OFFLINE_LIMIT));
		resultPdu = _tag.transceive(EMV_COMMAND_GET_DATA_LOWER_CONSECUTIVE_OFFLINE_LIMIT);
		logResultPdu(resultPdu);
		logBerTlvResponse(resultPdu);

		_ctl.log("trying to send GET DATA for getting 'upper consecutive offline limit(?)'...");
		_ctl.log("sent: " + bytesToHex(EMV_COMMAND_GET_DATA_UPPER_CONSECUTIVE_OFFLINE_LIMIT));
		resultPdu = _tag.transceive(EMV_COMMAND_GET_DATA_UPPER_CONSECUTIVE_OFFLINE_LIMIT);
		logResultPdu(resultPdu);
		logBerTlvResponse(resultPdu);

		_ctl.log("trying to send GET DATA for getting 'lower cumulative offline tx amount(?)'...");
		_ctl.log("sent: " + bytesToHex(EMV_COMMAND_GET_DATA_LOWER_CUMULATIVE_TX_AMOUNT));
		resultPdu = _tag.transceive(EMV_COMMAND_GET_DATA_LOWER_CUMULATIVE_TX_AMOUNT);
		logResultPdu(resultPdu);
		logBerTlvResponse(resultPdu);

		_ctl.log("trying to send GET DATA for getting 'upper cumulative offline tx amount(?)'...");
		_ctl.log("sent: " + bytesToHex(EMV_COMMAND_GET_DATA_UPPER_CUMULATIVE_TX_AMOUNT));
		resultPdu = _tag.transceive(EMV_COMMAND_GET_DATA_UPPER_CUMULATIVE_TX_AMOUNT);
		logResultPdu(resultPdu);
		logBerTlvResponse(resultPdu);

		_ctl.log("trying to send GET DATA for unknown tag DF4D...");
		_ctl.log("sent: " + bytesToHex(EMV_COMMAND_GET_DATA_DF4D));
		resultPdu = _tag.transceive(EMV_COMMAND_GET_DATA_DF4D);
		logResultPdu(resultPdu);
		logBerTlvResponse(resultPdu);

		_ctl.log("trying to send GET DATA for unknown tag DF50...");
		_ctl.log("sent: " + bytesToHex(EMV_COMMAND_GET_DATA_DF50));
		resultPdu = _tag.transceive(EMV_COMMAND_GET_DATA_DF50);
		logResultPdu(resultPdu);
		logBerTlvResponse(resultPdu);

		_ctl.log("trying to send GET DATA for unknown tag DF51...");
		_ctl.log("sent: " + bytesToHex(EMV_COMMAND_GET_DATA_DF51));
		resultPdu = _tag.transceive(EMV_COMMAND_GET_DATA_DF51);
		logResultPdu(resultPdu);
		logBerTlvResponse(resultPdu);

	}

	/**
	 * Try to send GET DATA for ATC
	 * 
	 * @throws IOException
	 */
	private void tryToReadCurrentAtcValue() throws IOException {
		_ctl.log("trying to send GET DATA for getting 'ATC' (current application transaction counter)...");
		_ctl.log("sent: " + bytesToHex(EMV_COMMAND_GET_DATA_APP_TX_COUNTER));
		byte[] resultPdu = _tag.transceive(EMV_COMMAND_GET_DATA_APP_TX_COUNTER);
		logResultPdu(resultPdu);
		logBerTlvResponse(resultPdu);
	}

	/**
	 * Try to send GET DATA for ATC
	 * 
	 * @throws IOException
	 */
	private void tryToReadLastOnlineAtcRegisterValue() throws IOException {
		_ctl.log(
				"trying to send GET DATA for getting 'Last online ATC Register' (application transaction counter of last online transaction)...");
		_ctl.log("sent: " + bytesToHex(EMV_COMMAND_GET_DATA_LAST_ONLINE_APP_TX_COUNTER));
		byte[] resultPdu = _tag.transceive(EMV_COMMAND_GET_DATA_LAST_ONLINE_APP_TX_COUNTER);
		logResultPdu(resultPdu);
		logBerTlvResponse(resultPdu);
	}

	/**
	 * Tries to read all common simple TLV tags
	 * 
	 * @throws IOException
	 */
	private void tryToReadAllCommonSimpleTlvTags() throws IOException {
		_ctl.log("trying to send command for getting all common simple TLV tags...");
		_ctl.log("sent: " + bytesToHex(EMV_COMMAND_GET_DATA_ALL_COMMON_SIMPLE_TLV));
		byte[] resultPdu = _tag.transceive(EMV_COMMAND_GET_DATA_ALL_COMMON_SIMPLE_TLV);
		logResultPdu(resultPdu);
		logBerTlvResponse(resultPdu);
	}

	/**
	 * Tries to read all common BER TLV tags
	 * 
	 * @throws IOException
	 */
	private void tryToReadAllCommonBerTlvTags() throws IOException {
		_ctl.log("trying to send command for getting all common BER TLV tags...");
		_ctl.log("sent: " + bytesToHex(EMV_COMMAND_GET_DATA_ALL_COMMON_BER_TLV));
		byte[] resultPdu = _tag.transceive(EMV_COMMAND_GET_DATA_ALL_COMMON_BER_TLV);
		logResultPdu(resultPdu);
		logBerTlvResponse(resultPdu);
	}

	/**
	 * CAUTION!!! If run out of PIN retries the app will be BLOCKED and your
	 * card UNUSABLE!!! Try to send VERIFY PIN in PLAINTEXT!<br>
	 * Only works if plaintext pin is allowed in the cards CVM (cardholder
	 * verification methods) methods. See tag 8E "CVM List" if it is allowed on
	 * your card. <br>
	 * <br>
	 * UPDATE: Austrian cards don't support plain text pin verification (and
	 * this good!!), they only support enciphered PIN verfication (which means
	 * we need to encipher the PIN with the card's public key before we send it.
	 * Had no time time until now to read into this topic.
	 * 
	 * @throws IOException
	 */
	@SuppressWarnings("unused")
	private void tryToVerifyPlaintextPin(String pin) throws IOException {
		// this just performs PLAINTEXT pin verification (not supported on
		// modern cards)
		// TODO: maybe implement enciphered PIN verification..
		_ctl.log("trying to VERIFY PLAINTEXT PIN: " + pin);
		byte[] resultPdu = _tag.transceive(createApduVerifyPIN(pin, true));
		logResultPdu(resultPdu);
		logBerTlvResponse(resultPdu);
	}

	private byte[] transceiveAndLog(byte[] data) throws IOException {
		_ctl.log("sending: " + bytesToHex(data));
		return _tag.transceive(data);
	}

	/**
	 * tests with Paylife "quick" cards (that's an Austrian thing)..
	 * 
	 * @throws IOException
	 */
	private CardInfo tryReadingQuickLogEntries(CardInfo result) throws IOException {
		List<QuickTransactionLogEntry> quickLogs = new ArrayList<QuickTransactionLogEntry>();

		byte[] resultPdu;

		_ctl.log("-----------------------------------");
		_ctl.log("-- reading Paylife QUICK log entries: ");
		// selecting DF containing logs:
		resultPdu = transceiveAndLog(fromHexString("00 a4 00 00 02 01 04"));
		logResultPdu(resultPdu);
		int currRecord = 1;
		while (true) {
			// read currently selected file
			resultPdu = readRecord(0, currRecord, true);
			if (isStatusSuccess(getLast2Bytes(resultPdu))) {
				QuickTransactionLogEntry log = parseQuickTxLogEntryFromByteArray(resultPdu);
				_ctl.log("-----------------------------------");
				_ctl.log(log.toString());
				quickLogs.add(log);
				currRecord++;
			} else {
				break;
			}
		}
		result.setQuickLog(quickLogs);
		_ctl.log("-----------------------------------");
		return result;
	}

	/**
	 * some tests.. that's my "playground" for experimenting with new commands..
	 * :-)
	 * 
	 * @throws IOException
	 */
	@SuppressWarnings("unused")
	private CardInfo tryReadingTests(CardInfo result) throws IOException {
		// byte[] cmd;
		byte[] resultPdu;
		//
		// DANGEROUS!!!!!!
		// DANGEROUS!!!!!!
		// DANGEROUS!!!!!!
		//
		// GET CHALLENGE is an active command which may change the state in your
		// card!!! Only perform if you know what you do!!!
		//
		//
		// _ctl.log("trying to send command GET CHALLENGE: ");
		// resultPdu = _localIsoDep.transceive(EMV_COMMAND_GET_CHALLENGE);
		// logResultPdu(resultPdu);
		//
		//
		// Log.d(TAG, "trying to send SELECT COMMAND 3F 00: ");
		// cmd = createSelectFile(fromHexString("3F 00"));
		// Log.d(TAG, "sending: " + bytesToHex(cmd));
		// resultPdu = _localIsoDep.transceive(cmd);
		// logResultPdu(resultPdu);
		//
		// Log.d(TAG, "trying to send SELECT COMMAND 00 02: ");
		// cmd = createSelectFile(fromHexString("00 02"));
		// Log.d(TAG, "sending: " + bytesToHex(cmd));
		// resultPdu = _localIsoDep.transceive(cmd);
		// logResultPdu(resultPdu);
		//
		// searchForFiles(result, false);
		//
		// cmd="80 CA XX XX 00";
		// _ctl.log("trying to send command (): "+cmd);
		// resultPdu = _localIsoDep.transceive(fromHexString(cmd));
		// logResultPdu(resultPdu);
		// logBerTlvResponse(resultPdu);
		// String cmd;
		// cmd = "80 CA BF 30 00";
		// _ctl.log("trying to send command (): " + cmd);
		// resultPdu = _localIsoDep.transceive(fromHexString(cmd));
		// logResultPdu(resultPdu);
		// logBerTlvResponse(resultPdu);
		// cmd = "80 CA 9F 35 00";
		// _ctl.log("trying to send command (): " + cmd);
		// resultPdu = _localIsoDep.transceive(fromHexString(cmd));
		// logResultPdu(resultPdu);
		// logBerTlvResponse(resultPdu);
		// cmd = "80 CA 9F 50 00";
		// _ctl.log("trying to send command (): " + cmd);
		// resultPdu = _localIsoDep.transceive(fromHexString(cmd));
		// logResultPdu(resultPdu);
		// logBerTlvResponse(resultPdu);

		// QUICK:
		// -- 3F 00
		// \ -- 00 02: EF 5 REC 1: 005C 9000
		//

		return result;
	}

	/**
	 * Checks if the EMV TAG "9F 4D Log Entry" is found within the list. This
	 * EMV tag normally specifies where to find the log records on the card (and
	 * also how many of them should be stored).<br/>
	 * If this tag is not present, it's a strong indication that there are no
	 * logs on the card.
	 * 
	 * @param result
	 * @return
	 */
	private CardInfo lookForLogEntryEmvTag(CardInfo result) {
		boolean foundLogTag = false;
		for (TagAndValue tv : _tagList) {
			if ("9F4D".equals(bytesToHex(tv.getTag().getTagBytes()))) {
				foundLogTag = true;
			}
		}
		if (foundLogTag) {
			Log.d(TAG, "YES! EMV Tag 'Log Entry' found! This card *may* " + "store transactions logs.");
		} else {
			Log.d(TAG, "NO! Dit not find the EMV Tag 'Log Entry'! This means "
					+ "that this card propably won't store transactions logs at all.");
		}
		result.setContainsTxLogs(foundLogTag);
		return result;
	}

	/**
	 * Just try reading all EF files from 0 to 10 and see if there will be emv
	 * data returned.
	 * 
	 * @param result
	 * @param fullFileScan
	 *            <code>true</code> if we should try to iterate over all EFs,
	 *            false if only some well known on Austrian Bankomat Cards
	 * @param tryToParse
	 *            try to parse result data
	 * @return
	 * @throws IOException
	 */
	private CardInfo searchForFiles(CardInfo result, boolean fullFileScan, boolean tryToParse) throws IOException {
		_ctl.log("We ignore the cards 'Application File Locator' and just iterate over files here..");

		// we now simply check in 2 loops a lot of files and records if they
		// return BER-TLV encoded data or Transaction Logs

		// On my Bank Austria card there are more EMV records than reported in
		// the 'Application File Locator', also there is one log entry more than
		// reported in the "9F4D Log Entry" Tag. So I think it's not so bad to
		// just iterate over everything.

		// if we find something looking like a TX log, add it to TX list
		List<EmvTransactionLogEntry> txList = new ArrayList<EmvTransactionLogEntry>();

		int consecutiveErrorRecords = 0;

		// iterate over EFs
		for (int shortEfFileIdentifier = 0; shortEfFileIdentifier < 32; shortEfFileIdentifier++) {

			// ugly and hardcoded, but keep it for now
			// jump to next if EF not in whitelst
			if (!fullFileScan) {
				if (shortEfFileIdentifier != 1 && shortEfFileIdentifier != 2 && shortEfFileIdentifier != 3
						&& shortEfFileIdentifier != 4 && shortEfFileIdentifier != 21
						&& shortEfFileIdentifier != LOG_RECORD_EF)
					continue;
			}

			// for each new EF set the consecutive error counter to 0
			consecutiveErrorRecords = 0;

			Log.d(TAG, "Trying now to read EF " + shortEfFileIdentifier + "...");

			// iterate over records within EF
			for (int currentRecord = 0; currentRecord < 256; currentRecord++) {
				if ((fullFileScan && consecutiveErrorRecords > 6) || (!fullFileScan && consecutiveErrorRecords > 2)) {
					// if we had 6 errors (or 3 if we do a fast scan) in a row
					// we assume that no more
					// records will come and just leave this EF and go
					// to the next
					break;
				}
				byte[] responsePdu = readRecord(shortEfFileIdentifier, currentRecord, false);
				if (isStatusSuccess(getLast2Bytes(responsePdu))) {
					// also if we find a record set counter to 0
					consecutiveErrorRecords = 0;
					if (tryToParse) {
						if (shortEfFileIdentifier == LOG_RECORD_EF && lengthLooksLikeTxLog(responsePdu)) {
							EmvTransactionLogEntry txLogEntry = tryToParseLogEntry(responsePdu);
							if (txLogEntry != null) {
								txList.add(txLogEntry);
								_ctl.log(txLogEntry.toString());
							}
						} else {
							// avoid that a single unparsable record may abort
							// the whole scan
							try {
								logBerTlvResponse(responsePdu);
							} catch (Exception e) {
								Log.w(TAG, "Ignored exception while parsing TLV data", e);
							}
						}
					} else {
						logResultPdu(responsePdu);
					}
				} else {
					consecutiveErrorRecords++;
					// if card returns error for this record, just try the
					// next...
					continue;
				}
			}

		}
		result.setTransactionLog(txList);
		return result;
	}

	/**
	 * Very simple test for log record..
	 * 
	 * @param rawRecord
	 * @return
	 */
	private boolean lengthLooksLikeTxLog(byte[] rawRecord) {
		if (LOG_FORMAT_BANKOMAT_AUSTRIA.equals(_logFormatResponse)) {
			return (rawRecord.length == LOG_LENGTH_BANKOMAT_AUSTRIA);
		} else if (LOG_FORMAT_MASTERCARD.equals(_logFormatResponse)) {
			return (rawRecord.length == LOG_LENGTH_MASTERCARD);
		} else if (LOG_FORMAT_VISA.equals(_logFormatResponse)) {
			return (rawRecord.length == LOG_LENGTH_VISA);
		}
		return false;
	}

	/**
	 * @param rawRecord
	 * @return
	 */
	private EmvTransactionLogEntry tryToParseLogEntry(byte[] rawRecord) {
		if (LOG_FORMAT_BANKOMAT_AUSTRIA.equals(_logFormatResponse)) {
			return parseBankomatTxLogEntryFromByteArray(rawRecord);
		} else if (LOG_FORMAT_MASTERCARD.equals(_logFormatResponse)) {
			return parseMastercardTxLogEntryFromByteArray(rawRecord);
		} else if (LOG_FORMAT_VISA.equals(_logFormatResponse)) {
			return parseVisaTxLogEntryFromByteArray(rawRecord);
		}
		return null;
	}

	/**
	 * Try to parse the raw byte array into an object
	 * 
	 * @param rawRecord
	 *            (without status word
	 * @return the parsed record or <code>null</code> if something could not be
	 *         parsed
	 */
	private QuickTransactionLogEntry parseQuickTxLogEntryFromByteArray(byte[] rawRecord) {

		// ATC amount amount curr Term# term stat? tagBCD? zeit rest
		// (conv to dez)

		// 0 1 2 3 6 7 10 1112 13 16 17 20 21 22 25 26 28
		// 27 001D 000001A4 000001A4 0978 000984AB 00001AC4 90 00014339 194500

		// 29 32 3334
		// 0000014D 9000

		// logformat of quick (not documented, as guessed by me)
		//
		// 01 bytes -> unknown byte 1, always seems to be 0x27 ??
		// 02 bytes -> Application Transaction Counter (ATC)
		// 04 bytes -> Amount (no BCD, stored as normal integer)
		// 04 bytes -> Amount2 (no BCD, stored as normal integer) seems always
		// to be the same as the first amount??
		// 02 bytes -> Transaction Currency Code
		// 04 bytes -> Terminal Info 1
		// 04 bytes -> Terminal Info 2
		// 01 bytes -> unknown byte 2, always seems to be 0x90 ??
		// 04 bytes -> day as integer
		// 03 bytes -> time same coding as in EMV
		// 04 bytes -> Remaining balance afterwards (no BCD, stored as normal
		// integer) ??

		// 9A (03 bytes) -> Transaction Date
		// 9F 52 (06 bytes) -> Application Default Action (ADA)

		if (rawRecord.length < LOG_LENGTH_QUICK) {
			Log.w(TAG, "parseTxLogEntryFromByteArray: byte array is not long enough for quick log entry:\n"
					+ prettyPrintString(bytesToHex(rawRecord), 2));
			return null;
		}
		QuickTransactionLogEntry tx = new QuickTransactionLogEntry();
		tx.setUnknownByte1(rawRecord[0]);
		tx.setAtc(byteArrayToInt(getByteArrayPart(rawRecord, 1, 2)));
		tx.setAmount(getAmountFromBytes(getByteArrayPart(rawRecord, 3, 6)));
		tx.setAmount2(getAmountFromBytes(getByteArrayPart(rawRecord, 7, 10)));
		tx.setCurrency(Iso4217CurrencyCodes.getCurrencyAsString(getByteArrayPart(rawRecord, 11, 12)));
		tx.setTerminalInfos1(readLongFromBytes(getByteArrayPart(rawRecord, 13, 16), 0, 4));
		tx.setTerminalInfos2(readLongFromBytes(getByteArrayPart(rawRecord, 17, 20), 0, 4));
		tx.setUnknownByte2(rawRecord[21]);
		tx.setTransactionTimestamp(
				getTimeStampFromQuickLog(readBcdIntegerFromBytes(getByteArrayPart(rawRecord, 22, 25)),
						getByteArrayPart(rawRecord, 26, 28)),
				true);
		tx.setRemainingBalance(getAmountFromBytes(getByteArrayPart(rawRecord, 29, 32)));
		// and raw entry
		tx.setRawEntry(rawRecord);
		return tx;
	}

	/**
	 * Try to parse the raw byte array into an object
	 * 
	 * @param rawRecord
	 *            (without status word
	 * @return the parsed record or <code>null</code> if something could not be
	 *         parsed
	 */
	private EmvTransactionLogEntry parseMastercardTxLogEntryFromByteArray(byte[] rawRecord) {

		// hardcoded to log format of Mastercard (2014)

		// 9F 4F - 1A bytes: Log Format
		// --------------------------------------
		// 9F 27 (01 bytes) -> Cryptogram Information Data
		// 9F 02 (06 bytes) -> Amount, Authorised (Numeric)
		// 5F 2A (02 bytes) -> Transaction Currency Code
		// 9A (03 bytes) -> Transaction Date
		// 9F 36 (02 bytes) -> Application Transaction Counter (ATC)
		// 9F 52 (06 bytes) -> Application Default Action (ADA)

		if (rawRecord.length < LOG_LENGTH_MASTERCARD) {
			// only continue if record is at least 24(+2 status) bytes long
			Log.w(TAG, "parseTxLogEntryFromByteArray: byte array is not long enough for log entry:\n"
					+ prettyPrintString(bytesToHex(rawRecord), 2));
			return null;
		}

		EmvTransactionLogEntry tx = new EmvTransactionLogEntry();
		try {
			tx.setCryptogramInformationData(rawRecord[0]);
			tx.setAmount(getAmountFromBcdBytes(getByteArrayPart(rawRecord, 1, 6)));
			tx.setCurrency(Iso4217CurrencyCodes.getCurrencyAsString(getByteArrayPart(rawRecord, 7, 8)));
			tx.setTransactionTimestamp(getDateFromBcdBytes(getByteArrayPart(rawRecord, 9, 11)), false);
			tx.setAtc(byteArrayToInt(getByteArrayPart(rawRecord, 12, 13)));
			tx.setApplicationDefaultAction(getByteArrayPart(rawRecord, 14, 19));
			tx.setRawEntry(rawRecord);
		} catch (Exception e) {
			String msg = "Exception while trying to parse transaction entry: " + e + "\n" + e.getMessage()
					+ "\nraw byte array:\n" + prettyPrintString(bytesToHex(rawRecord), 2);
			Log.w(TAG, msg, e);
			_ctl.log(msg);
			return null;
		}
		return tx;
	}

	/**
	 * Try to parse the raw byte array into an object
	 * 
	 * @param rawRecord
	 *            (without status word
	 * @return the parsed record or <code>null</code> if something could not be
	 *         parsed
	 */
	private EmvTransactionLogEntry parseVisaTxLogEntryFromByteArray(byte[] rawRecord) {

		// hardcoded to log format of Mastercard (2014)

		// 9F 4F - 1A bytes: Log Format
		// --------------------------------------
		// 9F 36 (02 bytes) -> Application Transaction Counter (ATC)
		// 9F 02 (06 bytes) -> Amount, Authorised (Numeric)
		// 9F 03 (06 bytes) -> Amount, Other (Numeric)
		// 9F 1A (02 bytes) -> Terminal Country Code
		// 95 (05 bytes) -> Terminal Verification Results (TVR)
		// 5F 2A (02 bytes) -> Transaction Currency Code
		// 9A (03 bytes) -> Transaction Date
		// 9C (01 bytes) -> Transaction Type
		// 9F 80 (04 bytes) -> [UNHANDLED TAG]

		if (rawRecord.length < LOG_LENGTH_VISA) {
			Log.w(TAG, "parseTxLogEntryFromByteArray: byte array is not long enough for VISA log entry:\n"
					+ prettyPrintString(bytesToHex(rawRecord), 2));
			return null;
		}

		EmvTransactionLogEntry tx = new EmvTransactionLogEntry();
		try {
			tx.setAtc(byteArrayToInt(getByteArrayPart(rawRecord, 0, 1)));
			tx.setAmount(getAmountFromBcdBytes(getByteArrayPart(rawRecord, 2, 7)));
			// TODO: currently we ignore "Amount, Other" for VISA logs
			// 8-13
			// TODO: include terminal country code (14-15)
			// TODO: include TVR for VISA (16-20)
			tx.setCurrency(Iso4217CurrencyCodes.getCurrencyAsString(getByteArrayPart(rawRecord, 21, 22)));
			tx.setTransactionTimestamp(getDateFromBcdBytes(getByteArrayPart(rawRecord, 23, 25)), false);
			// TODO: include Transaction Type (26)
			// TODO: include unknown tag 9f 80 (27-30)

			tx.setRawEntry(rawRecord);
		} catch (Exception e) {
			String msg = "Exception while trying to parse transaction entry: " + e + "\n" + e.getMessage()
					+ "\nraw byte array:\n" + prettyPrintString(bytesToHex(rawRecord), 2);
			Log.w(TAG, msg, e);
			_ctl.log(msg);
			return null;
		}
		return tx;
	}

	/**
	 * Try to parse the raw byte array into an object
	 * 
	 * @param rawRecord
	 *            (without status word
	 * @return the parsed record or <code>null</code> if something could not be
	 *         parsed
	 */
	private EmvTransactionLogEntry parseBankomatTxLogEntryFromByteArray(byte[] rawRecord) {

		// hardcoded to log format of Austrian cards

		// 9F 4F - 1A bytes: Log Format
		// --------------------------------------
		// 9F 27 (01 bytes) -> Cryptogram Information Data
		// 9F 02 (06 bytes) -> Amount, Authorised (Numeric)
		// 5F 2A (02 bytes) -> Transaction Currency Code
		// 9A (03 bytes) -> Transaction Date
		// 9F 36 (02 bytes) -> Application Transaction Counter (ATC)
		// 9F 52 (06 bytes) -> Application Default Action (ADA)
		// DF 3E (01 bytes) -> [UNHANDLED TAG]
		// 9F 21 (03 bytes) -> Transaction Time (HHMMSS)
		// 9F 7C (14 bytes) -> Customer Exclusive Data

		if (rawRecord.length < 24) {
			// only continue if record is at least 24(+2 status) bytes long
			Log.w(TAG, "parseTxLogEntryFromByteArray: byte array is not long enough:\n"
					+ prettyPrintString(bytesToHex(rawRecord), 2));
			return null;
		}

		EmvTransactionLogEntry tx = new EmvTransactionLogEntry();
		try {
			tx.setCryptogramInformationData(rawRecord[0]);
			tx.setAmount(getAmountFromBcdBytes(getByteArrayPart(rawRecord, 1, 6)));
			tx.setCurrency(Iso4217CurrencyCodes.getCurrencyAsString(getByteArrayPart(rawRecord, 7, 8)));
			tx.setTransactionTimestamp(
					getTimeStampFromBcdBytes(getByteArrayPart(rawRecord, 9, 11), getByteArrayPart(rawRecord, 21, 23)),
					true);
			tx.setAtc(byteArrayToInt(getByteArrayPart(rawRecord, 12, 13)));
			tx.setApplicationDefaultAction(getByteArrayPart(rawRecord, 14, 19));
			tx.setUnknownByte(rawRecord[20]);

			// if record has only 24 bytes then there is no cust excl data
			// as it starts at byte 25
			if (rawRecord.length == 24) {
				tx.setCustomerExclusiveData(new byte[0]);
			} else {
				// for being tolerant we parse from byte 25 untiil end (last 2
				// bytes are status)
				tx.setCustomerExclusiveData(getByteArrayPart(rawRecord, 24, rawRecord.length - 3));
			}

			tx.setRawEntry(rawRecord);
		} catch (Exception e) {
			String msg = "Exception while trying to parse transaction entry: " + e + "\n" + e.getMessage()
					+ "\nraw byte array:\n" + prettyPrintString(bytesToHex(rawRecord), 2);
			Log.w(TAG, msg, e);
			_ctl.log(msg);
			return null;
		}
		return tx;
	}

	/**
	 * Perform a READ RECORD command on the card
	 * 
	 * @param shortEfFileIdentifier
	 * @param recordNumber
	 * @param logAlways
	 *            if <code>true</code> log always, otherwise log only on
	 *            successful response
	 * 
	 * @return
	 * @throws IOException
	 * 
	 */
	private byte[] readRecord(int shortEfFileIdentifier, int recordNumber, boolean logAlways) throws IOException {
		byte[] readRecordApdu = createReadRecordApdu(shortEfFileIdentifier, recordNumber);
		byte[] resultPdu = _tag.transceive(readRecordApdu);
		if (logAlways || isStatusSuccess(getLast2Bytes(resultPdu))) {
			String msg = "READ RECORD for EF " + shortEfFileIdentifier + " and RECORD " + recordNumber;
			Log.d(TAG, msg);
			_ctl.log(msg);
			_ctl.log("sent: " + bytesToHex(readRecordApdu));
			logResultPdu(resultPdu);
		}
		return resultPdu;
	}

	/**
	 * Select MF (master file). Imagine this as kind of "cd /" on the card
	 * structure
	 * 
	 * @return
	 * @throws IOException
	 */
	@SuppressWarnings("unused")
	private byte[] selectMasterfile() throws IOException {
		byte[] readRecordApdu = createSelectMasterFile();
		byte[] resultPdu = _tag.transceive(readRecordApdu);
		if (isStatusSuccess(getLast2Bytes(resultPdu))) {
			String msg = "SELECT MF  (cd / ) ";
			Log.d(TAG, msg);
			_ctl.log(msg);
			_ctl.log("sent: " + bytesToHex(readRecordApdu));
			logResultPdu(resultPdu);
		}
		return resultPdu;
	}

	/**
	 * Select parent DF. Imagine this as kind of "cd .." on the card structure
	 * 
	 * @return
	 * @throws IOException
	 */
	@SuppressWarnings("unused")
	private byte[] selectParentDf() throws IOException {
		byte[] readRecordApdu = createSelectParentDfFile();
		byte[] resultPdu = _tag.transceive(readRecordApdu);
		if (isStatusSuccess(getLast2Bytes(resultPdu))) {
			String msg = "SELECT parent DF  (cd .. ) ";
			Log.d(TAG, msg);
			_ctl.log(msg);
			_ctl.log("sent: " + bytesToHex(readRecordApdu));
			logResultPdu(resultPdu);
		}
		return resultPdu;
	}

	/**
	 * @return balance of quick card, or -1 on error
	 * @throws IOException
	 */
	private long getQuickCardBalance() throws IOException {
		_ctl.log("Reading QUICK balance");
		_ctl.log("sent: " + bytesToHex(ISO_COMMAND_QUICK_READ_BALANCE));
		byte[] resultPdu = _tag.transceive(ISO_COMMAND_QUICK_READ_BALANCE);
		logResultPdu(resultPdu);
		if (!isStatusSuccess(getLast2Bytes(resultPdu))) {
			Log.w(TAG, "getQuickCardBalance: Response status word was not ok! Error: "
					+ statusToString(getLast2Bytes(resultPdu)) + ". In hex: " + bytesToHex(resultPdu));
			_ctl.log("will return balance -1");
			return -1;
		}
		long balance = getAmountFromBytes(resultPdu);
		_ctl.log("QUICK balance = " + balance);
		return balance;
	}

	/**
	 * @return 2-bytes long representing ISO xxxx currency
	 * @throws IOException
	 * @throws TlvParsingException
	 */
	private byte[] getQuickCardCurrencyBytes() throws IOException, TlvParsingException {
		_ctl.log("Reading QUICK currency");
		_ctl.log("sent: " + bytesToHex(ISO_COMMAND_QUICK_READ_CURRENCY));
		byte[] resultPdu = _tag.transceive(ISO_COMMAND_QUICK_READ_CURRENCY);
		logResultPdu(resultPdu);
		if (!isStatusSuccess(getLast2Bytes(resultPdu))) {
			String msg = "getQuickCardCurrencyBytes: Response status was not 'SUCCESS'! The response was: "
					+ statusToString(getLast2Bytes(resultPdu)) + ". In hex: " + bytesToHex(resultPdu)
					+ "\nThe complete response was:\n" + prettyPrintString(bytesToHex(resultPdu), 2);
			Log.w(TAG, msg);
			throw new TlvParsingException(msg);
		}
		byte[] rawCurrency = new byte[2];
		System.arraycopy(resultPdu, 0, rawCurrency, 0, 2);
		_ctl.log("QUICK currency = " + prettyPrintString(bytesToHex(rawCurrency), 2));
		_ctl.log("QUICK currency = " + Iso4217CurrencyCodes.getCurrencyAsString(rawCurrency));
		return rawCurrency;
	}

	/**
	 * Send GET_CPLC_COMMAND command to the card to receive
	 * "card production life cycle" (CPLC) data according the GlobalPlatform
	 * Card Specification.
	 * 
	 * @return the bytes as returned by the SmartCard
	 * @throws IOException
	 */
	private byte[] sendGetCPLC() throws IOException {
		// cannot read CPLC over OMAPI
		if (_tag instanceof OmapiSessionTag) {
			// return generic error for OMAPI
			return fromHexString("6F00");
		}
		Log.d(TAG, "sending GET CPLC command..");
		byte[] command = EmvUtils.GPCS_GET_CPLC_COMMAND;
		Log.d(TAG, "will send byte array: " + bytesToHex(command));
		_ctl.log("sent: " + bytesToHex(command));
		byte[] resultPdu = _tag.transceive(command);
		logResultPdu(resultPdu);
		Log.d(TAG, "received byte array:  " + bytesToHex(resultPdu));

		// some card don't return CPLC if sent with Le 00
		// retry it with (hardcoded) Le value
		// TODO: better check if SW1 == 6D ("incorrect len, SW2 specifies
		// correct length")
		// and send specified len
		if (!isStatusSuccess(getLast2Bytes(resultPdu))) {
			Log.d(TAG, "sending GET CPLC returned an error, will retry with Le set..");
			Log.d(TAG, "sending GET CPLC command with Le set..");
			command = EmvUtils.GPCS_GET_CPLC_COMMAND_WITH_LENGTH;
			Log.d(TAG, "will send byte array: " + bytesToHex(command));
			_ctl.log("sent: " + bytesToHex(command));
			resultPdu = _tag.transceive(command);
			logResultPdu(resultPdu);
			Log.d(TAG, "received byte array:  " + bytesToHex(resultPdu));
		}
		return resultPdu;
	}

	/**
	 * Select an AID on the card and return returned byte array
	 * 
	 * @param appId
	 * @return the bytes as returned by the SmartCard
	 * @throws IOException
	 */
	private byte[] selectApplicationGetBytes(byte[] appId) throws IOException {
		Log.d(TAG, "sending ISO7816 SELECT command, with AID: " + bytesToHex(appId));
		byte[] resultPdu = _tag.selectAid(appId);
		// byte[] command = createSelectAid(appId);
		// _ctl.log("sent: " + bytesToHex(command));
		logResultPdu(resultPdu);
		Log.d(TAG, "received byte array:  " + bytesToHex(resultPdu));
		return resultPdu;
	}

	/**
	 * log result pdu
	 * 
	 * @param resultPdu
	 */
	private void logResultPdu(byte[] resultPdu) {
		Log.d(TAG, "received: " + bytesToHex(resultPdu));
		Log.d(TAG, "status: " + prettyPrintString(bytesToHex(getLast2Bytes(resultPdu)), 2));
		Log.d(TAG, "status: " + statusToString(getLast2Bytes(resultPdu)));
		_ctl.log("received: " + bytesToHex(resultPdu));
		_ctl.log("status: " + prettyPrintString(bytesToHex(getLast2Bytes(resultPdu)), 2) + " - "
				+ statusToString(getLast2Bytes(resultPdu)));
	}

	/**
	 * Try to decode a response PDU as BER TLV encoded data and log it
	 * 
	 * @param resultPdu
	 */
	private void logBerTlvResponse(byte[] resultPdu) {
		if (resultPdu.length > 2) {
			try {
				byte[] data = cutoffLast2Bytes(resultPdu);
				_ctl.log("Trying to decode response as BER-TLV..");
				_ctl.log(prettyPrintBerTlvAPDUResponse(data, 0));
				// and add all found tags to list
				_tagList.addAll(getTagsFromBerTlvAPDUResponse(data));
			} catch (TlvParsingException e) {
				_ctl.log("decoding error... maybe this data is not BER-TLV encoded?");
				Log.w(TAG,
						"exception while parsing BER-TLV PDU response\n" + prettyPrintString(bytesToHex(resultPdu), 2),
						e);
			}
		}
	}
}