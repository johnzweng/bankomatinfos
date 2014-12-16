package at.zweng.bankomatinfos.iso7816emv;

import static at.zweng.bankomatinfos.iso7816emv.EmvUtils.APPLICATION_ID_EMV_MAESTRO_BANKOMAT;
import static at.zweng.bankomatinfos.iso7816emv.EmvUtils.APPLICATION_ID_EMV_MASTERCARD;
import static at.zweng.bankomatinfos.iso7816emv.EmvUtils.APPLICATION_ID_EMV_VISA_CREDITCARD;
import static at.zweng.bankomatinfos.iso7816emv.EmvUtils.APPLICATION_ID_QUICK;
import static at.zweng.bankomatinfos.iso7816emv.EmvUtils.EMV_COMMAND_GET_DATA_ALL_COMMON_BER_TLV;
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
import static at.zweng.bankomatinfos.iso7816emv.EmvUtils.isStatusSuccess;
import static at.zweng.bankomatinfos.iso7816emv.EmvUtils.prettyPrintBerTlvAPDUResponse;
import static at.zweng.bankomatinfos.iso7816emv.EmvUtils.statusToString;
import static at.zweng.bankomatinfos.util.Utils.TAG;
import static at.zweng.bankomatinfos.util.Utils.byteArrayToInt;
import static at.zweng.bankomatinfos.util.Utils.bytesToHex;
import static at.zweng.bankomatinfos.util.Utils.cutoffLast2Bytes;
import static at.zweng.bankomatinfos.util.Utils.getByteArrayPart;
import static at.zweng.bankomatinfos.util.Utils.getLast2Bytes;
import static at.zweng.bankomatinfos.util.Utils.prettyPrintString;

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
import at.zweng.bankomatinfos.model.InfoKeyValuePair;
import at.zweng.bankomatinfos.model.TransactionLogEntry;
import at.zweng.bankomatinfos2.R;

/**
 * Performs all the reading operations on a card.
 * 
 * @author Johannes Zweng <johannes@zweng.at>
 */
public class NfcBankomatCardReader {
	private Tag _nfcTag;
	private IsoDep _localIsoDep;
	private AppController _ctl;
	private List<TagAndValue> _tagList;
	private Context _ctx;

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
	public NfcBankomatCardReader(Tag nfcTag, Context ctx) {
		super();
		this._nfcTag = nfcTag;
		this._ctl = AppController.getInstance();
		this._tagList = new ArrayList<TagAndValue>();
		this._ctx = ctx;
	}

	/**
	 * Connects to IsoDep
	 * 
	 * @throws IOException
	 */
	public void connectIsoDep() throws IOException, NoSmartCardException {
		_localIsoDep = IsoDep.get(_nfcTag);
		if (_localIsoDep == null) {
			throw new NoSmartCardException("This NFC tag is no ISO 7816 card");
		}
		_localIsoDep.connect();
	}

	/**
	 * Disconnects to IsoDep
	 * 
	 * @throws IOException
	 */
	public void disconnectIsoDep() throws IOException {
		_localIsoDep.close();
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
	public CardInfo readAllCardData(boolean performFullFileScan)
			throws IOException {
		CardInfo result = new CardInfo(_ctx);
		_ctl.log("Starting to read data from card..");
		result.addSectionHeader(_ctx.getResources().getString(
				R.string.section_nfc));
		result.setNfcTagId(_nfcTag.getId());
		_ctl.log("NFC Tag ID: "
				+ prettyPrintString(bytesToHex(_nfcTag.getId()), 2));
		_ctl.log("Historical bytes: "
				+ prettyPrintString(
						bytesToHex(_localIsoDep.getHistoricalBytes()), 2));
		result.addSectionHeader(_ctx.getResources().getString(
				R.string.section_GPCS_CPLC));
		result = readCPLCInfos(result);
		result.addSectionHeader(_ctx.getResources().getString(
				R.string.section_emv));
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
				result.addKeyValuePair(new InfoKeyValuePair(key,
						humanReadableVal));
			}
		} catch (TlvParsingException pe) {
			_ctl.log("ERROR: Catched Exception while reading CPLC data:\n" + pe
					+ "\n" + pe.getMessage());
			Log.w(TAG, "Catched Exception while reading CPLC infos: ", pe);
		} catch (RuntimeException re) {
			_ctl.log("ERROR: Catched Exception while reading CPLC infos:\n"
					+ re + "\n" + re.getMessage());
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
			result.setQuickCurrency(Iso4217CurrencyCodes
					.getCurrencyAsString(getQuickCardCurrencyBytes()));
		} catch (TlvParsingException pe) {
			_ctl.log("ERROR: Catched Exception while reading QUICK infos:\n"
					+ pe + "\n" + pe.getMessage());
			Log.w(TAG, "Catched Exception while reading QUICK infos: ", pe);
		} catch (RuntimeException re) {
			_ctl.log("ERROR: Catched Exception while reading QUICK infos:\n"
					+ re + "\n" + re.getMessage());
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
	private CardInfo readMaestroCardInfos(CardInfo result, boolean fullFileScan)
			throws IOException {
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
			_ctl.log("ERROR: Catched Exception while reading Maestro infos:\n"
					+ re + "\n" + re.getMessage());
			Log.w(TAG, "Catched Exception while reading Maestro infos: ", re);
		} catch (TlvParsingException tle) {
			_ctl.log("ERROR: Catched Exception while reading Maestro infos:\n"
					+ tle + "\n" + tle.getMessage());
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
	private CardInfo readMastercardInfos(CardInfo result, boolean fullFileScan)
			throws IOException {
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
			_ctl.log("ERROR: Catched Exception while reading mastercard infos:\n"
					+ re + "\n" + re.getMessage());
			Log.w(TAG, "Catched Exception while reading mastercard  infos: ",
					re);
		} catch (TlvParsingException tle) {
			_ctl.log("ERROR: Catched Exception while reading mastercard  infos:\n"
					+ tle + "\n" + tle.getMessage());
			Log.w(TAG, "Catched Exception while reading mastercard  infos: ",
					tle);
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
	private CardInfo readVisaCardInfos(CardInfo result, boolean fullFileScan)
			throws IOException {
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
			_ctl.log("ERROR: Catched Exception while reading VISA card infos:\n"
					+ re + "\n" + re.getMessage());
			Log.w(TAG, "Catched Exception while reading VISA card infos: ", re);
		} catch (TlvParsingException tle) {
			_ctl.log("ERROR: Catched Exception while reading VISA card infos:\n"
					+ tle + "\n" + tle.getMessage());
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
	private CardInfo readEmvData(byte[] selectAidResponse, CardInfo result,
			boolean fullFileScan) throws IOException, TlvParsingException {
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
		byte[] resultPdu = _localIsoDep
				.transceive(EMV_COMMAND_GET_DATA_LOG_FORMAT);
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
	private CardInfo tryToReadPinRetryCounter(CardInfo result)
			throws IOException, TlvParsingException {
		_ctl.log("trying to read PIN retry counter from card...");
		_ctl.log("sent: " + bytesToHex(EMV_COMMAND_GET_DATA_PIN_RETRY_COUNTER));
		byte[] resultPdu = _localIsoDep
				.transceive(EMV_COMMAND_GET_DATA_PIN_RETRY_COUNTER);
		logResultPdu(resultPdu);
		logBerTlvResponse(resultPdu);
		if (isStatusSuccess(getLast2Bytes(resultPdu))) {
			BERTLV tlv = getNextTLV(new ByteArrayInputStream(resultPdu));
			int pinRetryCounter = tlv.getValueBytes()[0];
			_ctl.log("-----------------------------------------------------");
			_ctl.log("  Current PIN retry counter: >>>>>> " + pinRetryCounter
					+ " <<<<<<");
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
	private void tryToReadAdditionalGetDataFields() throws IOException,
			TlvParsingException {
		_ctl.log("trying to send GET DATA for getting 'card risk management currency(?)'...");
		_ctl.log("sent: " + bytesToHex(EMV_COMMAND_GET_DATA_CRM_CURRENCY));
		byte[] resultPdu = _localIsoDep
				.transceive(EMV_COMMAND_GET_DATA_CRM_CURRENCY);
		logResultPdu(resultPdu);
		logBerTlvResponse(resultPdu);

		_ctl.log("trying to send GET DATA for getting 'card risk management country(?)'...");
		_ctl.log("sent: " + bytesToHex(EMV_COMMAND_GET_DATA_CRM_COUNTRY));
		resultPdu = _localIsoDep.transceive(EMV_COMMAND_GET_DATA_CRM_COUNTRY);
		logResultPdu(resultPdu);
		logBerTlvResponse(resultPdu);

		_ctl.log("trying to send GET DATA for getting 'lower consecutive offline limit(?)'...");
		_ctl.log("sent: "
				+ bytesToHex(EMV_COMMAND_GET_DATA_LOWER_CONSECUTIVE_OFFLINE_LIMIT));
		resultPdu = _localIsoDep
				.transceive(EMV_COMMAND_GET_DATA_LOWER_CONSECUTIVE_OFFLINE_LIMIT);
		logResultPdu(resultPdu);
		logBerTlvResponse(resultPdu);

		_ctl.log("trying to send GET DATA for getting 'upper consecutive offline limit(?)'...");
		_ctl.log("sent: "
				+ bytesToHex(EMV_COMMAND_GET_DATA_UPPER_CONSECUTIVE_OFFLINE_LIMIT));
		resultPdu = _localIsoDep
				.transceive(EMV_COMMAND_GET_DATA_UPPER_CONSECUTIVE_OFFLINE_LIMIT);
		logResultPdu(resultPdu);
		logBerTlvResponse(resultPdu);

		_ctl.log("trying to send GET DATA for getting 'lower cumulative offline tx amount(?)'...");
		_ctl.log("sent: "
				+ bytesToHex(EMV_COMMAND_GET_DATA_LOWER_CUMULATIVE_TX_AMOUNT));
		resultPdu = _localIsoDep
				.transceive(EMV_COMMAND_GET_DATA_LOWER_CUMULATIVE_TX_AMOUNT);
		logResultPdu(resultPdu);
		logBerTlvResponse(resultPdu);

		_ctl.log("trying to send GET DATA for getting 'upper cumulative offline tx amount(?)'...");
		_ctl.log("sent: "
				+ bytesToHex(EMV_COMMAND_GET_DATA_UPPER_CUMULATIVE_TX_AMOUNT));
		resultPdu = _localIsoDep
				.transceive(EMV_COMMAND_GET_DATA_UPPER_CUMULATIVE_TX_AMOUNT);
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
		byte[] resultPdu = _localIsoDep
				.transceive(EMV_COMMAND_GET_DATA_APP_TX_COUNTER);
		logResultPdu(resultPdu);
		logBerTlvResponse(resultPdu);
	}

	/**
	 * Try to send GET DATA for ATC
	 * 
	 * @throws IOException
	 */
	private void tryToReadLastOnlineAtcRegisterValue() throws IOException {
		_ctl.log("trying to send GET DATA for getting 'Last online ATC Register' (application transaction counter of last online transaction)...");
		_ctl.log("sent: "
				+ bytesToHex(EMV_COMMAND_GET_DATA_LAST_ONLINE_APP_TX_COUNTER));
		byte[] resultPdu = _localIsoDep
				.transceive(EMV_COMMAND_GET_DATA_LAST_ONLINE_APP_TX_COUNTER);
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
		_ctl.log("sent: "
				+ bytesToHex(EMV_COMMAND_GET_DATA_ALL_COMMON_SIMPLE_TLV));
		byte[] resultPdu = _localIsoDep
				.transceive(EMV_COMMAND_GET_DATA_ALL_COMMON_SIMPLE_TLV);
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
		byte[] resultPdu = _localIsoDep
				.transceive(EMV_COMMAND_GET_DATA_ALL_COMMON_BER_TLV);
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
		byte[] resultPdu = _localIsoDep.transceive(createApduVerifyPIN(pin,
				true));
		logResultPdu(resultPdu);
		logBerTlvResponse(resultPdu);
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
			Log.d(TAG, "YES! EMV Tag 'Log Entry' found! This card *may* "
					+ "store transactions logs.");
		} else {
			Log.d(TAG,
					"NO! Dit not find the EMV Tag 'Log Entry'! This means "
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
	private CardInfo searchForFiles(CardInfo result, boolean fullFileScan,
			boolean tryToParse) throws IOException {
		_ctl.log("We ignore the cards 'Application File Locator' and just iterate over files here..");

		// we now simply check in 2 loops a lot of files and records if they
		// return BER-TLV encoded data or Transaction Logs

		// On my Bank Austria card there are more EMV records than reported in
		// the 'Application File Locator', also there is one log entry more than
		// reported in the "9F4D Log Entry" Tag. So I think it's not so bad to
		// just iterate over everything.

		// if we find something looking like a TX log, add it to TX list
		List<TransactionLogEntry> txList = new ArrayList<TransactionLogEntry>();

		int consecutiveErrorRecords = 0;

		// iterate over EFs
		for (int shortEfFileIdentifier = 0; shortEfFileIdentifier < 32; shortEfFileIdentifier++) {

			// ugly and hardcoded, but keep it for now
			// jump to next if EF not in whitelst
			if (!fullFileScan) {
				if (shortEfFileIdentifier != 1 && shortEfFileIdentifier != 2
						&& shortEfFileIdentifier != 3
						&& shortEfFileIdentifier != 4
						&& shortEfFileIdentifier != LOG_RECORD_EF)
					continue;
			}

			// for each new EF set the consecutive error counter to 0
			consecutiveErrorRecords = 0;

			Log.d(TAG, "Trying now to read EF " + shortEfFileIdentifier + "...");

			// iterate over records within EF
			for (int currentRecord = 0; currentRecord < 256; currentRecord++) {
				if ((fullFileScan && consecutiveErrorRecords > 6)
						|| (!fullFileScan && consecutiveErrorRecords > 2)) {
					// if we had 6 errors (or 3 if we do a fast scan) in a row
					// we assume that no more
					// records will come and just leave this EF and go
					// to the next
					break;
				}
				byte[] responsePdu = readRecord(shortEfFileIdentifier,
						currentRecord, false);
				if (isStatusSuccess(getLast2Bytes(responsePdu))) {
					// also if we find a record set counter to 0
					consecutiveErrorRecords = 0;
					if (tryToParse) {
						if (shortEfFileIdentifier == LOG_RECORD_EF
								&& lengthLooksLikeTxLog(responsePdu)) {
							TransactionLogEntry txLogEntry = tryToParseLogEntry(responsePdu);
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
								Log.w(TAG,
										"Ignored exception while parsing TLV data",
										e);
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
		}
		return false;
	}

	/**
	 * @param rawRecord
	 * @return
	 */
	private TransactionLogEntry tryToParseLogEntry(byte[] rawRecord) {
		if (LOG_FORMAT_BANKOMAT_AUSTRIA.equals(_logFormatResponse)) {
			return parseBankomatTxLogEntryFromByteArray(rawRecord);
		} else if (LOG_FORMAT_MASTERCARD.equals(_logFormatResponse)) {
			return parseMastercardTxLogEntryFromByteArray(rawRecord);
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
	private TransactionLogEntry parseMastercardTxLogEntryFromByteArray(
			byte[] rawRecord) {
		// TODO: change this method to parse tx log entries dynamically based on
		// format as specified by card

		// UPDATE: according users' responses it seems that all Austrian
		// Cards use the same logging structure (of course, all the cards come
		// from the same issuer). So I keep this for now..

		// TODO: currently hardcoded to log format of Austrian cards

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
			Log.w(TAG,
					"parseTxLogEntryFromByteArray: byte array is not long enough for log entry:\n"
							+ prettyPrintString(bytesToHex(rawRecord), 2));
			return null;
		}

		TransactionLogEntry tx = new TransactionLogEntry();
		try {
			tx.setCryptogramInformationData(rawRecord[0]);
			tx.setAmount(getAmountFromBcdBytes(getByteArrayPart(rawRecord, 1, 6)));
			tx.setCurrency(Iso4217CurrencyCodes
					.getCurrencyAsString(getByteArrayPart(rawRecord, 7, 8)));
			tx.setTransactionTimestamp(
					getDateFromBcdBytes(getByteArrayPart(rawRecord, 9, 11)),
					false);
			tx.setAtc(byteArrayToInt(getByteArrayPart(rawRecord, 12, 13)));
			tx.setApplicationDefaultAction(getByteArrayPart(rawRecord, 14, 19));
			tx.setRawEntry(rawRecord);
		} catch (Exception e) {
			String msg = "Exception while trying to parse transaction entry: "
					+ e + "\n" + e.getMessage() + "\nraw byte array:\n"
					+ prettyPrintString(bytesToHex(rawRecord), 2);
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
	private TransactionLogEntry parseBankomatTxLogEntryFromByteArray(
			byte[] rawRecord) {
		// TODO: change this method to parse tx log entries dynamically based on
		// format as specified by card

		// UPDATE: according users' responses it seems that all Austrian
		// Cards use the same logging structure (of course, all the cards come
		// from the same issuer). So I keep this for now..

		// TODO: currently hardcoded to log format of Austrian cards

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
			Log.w(TAG,
					"parseTxLogEntryFromByteArray: byte array is not long enough:\n"
							+ prettyPrintString(bytesToHex(rawRecord), 2));
			return null;
		}

		TransactionLogEntry tx = new TransactionLogEntry();
		try {
			tx.setCryptogramInformationData(rawRecord[0]);
			tx.setAmount(getAmountFromBcdBytes(getByteArrayPart(rawRecord, 1, 6)));
			tx.setCurrency(Iso4217CurrencyCodes
					.getCurrencyAsString(getByteArrayPart(rawRecord, 7, 8)));
			tx.setTransactionTimestamp(
					getTimeStampFromBcdBytes(
							getByteArrayPart(rawRecord, 9, 11),
							getByteArrayPart(rawRecord, 21, 23)), true);
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
				tx.setCustomerExclusiveData(getByteArrayPart(rawRecord, 24,
						rawRecord.length - 3));
			}

			tx.setRawEntry(rawRecord);
		} catch (Exception e) {
			String msg = "Exception while trying to parse transaction entry: "
					+ e + "\n" + e.getMessage() + "\nraw byte array:\n"
					+ prettyPrintString(bytesToHex(rawRecord), 2);
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
	private byte[] readRecord(int shortEfFileIdentifier, int recordNumber,
			boolean logAlways) throws IOException {
		byte[] readRecordApdu = createReadRecordApdu(shortEfFileIdentifier,
				recordNumber);
		byte[] resultPdu = _localIsoDep.transceive(readRecordApdu);
		if (logAlways || isStatusSuccess(getLast2Bytes(resultPdu))) {
			String msg = "READ RECORD for EF " + shortEfFileIdentifier
					+ " and RECORD " + recordNumber;
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
		byte[] resultPdu = _localIsoDep.transceive(readRecordApdu);
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
		byte[] resultPdu = _localIsoDep.transceive(readRecordApdu);
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
		byte[] resultPdu = _localIsoDep
				.transceive(ISO_COMMAND_QUICK_READ_BALANCE);
		logResultPdu(resultPdu);
		if (!isStatusSuccess(getLast2Bytes(resultPdu))) {
			Log.w(TAG,
					"getQuickCardBalance: Response status word was not ok! Error: "
							+ statusToString(getLast2Bytes(resultPdu))
							+ ". In hex: " + bytesToHex(resultPdu));
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
	private byte[] getQuickCardCurrencyBytes() throws IOException,
			TlvParsingException {
		_ctl.log("Reading QUICK currency");
		_ctl.log("sent: " + bytesToHex(ISO_COMMAND_QUICK_READ_CURRENCY));
		byte[] resultPdu = _localIsoDep
				.transceive(ISO_COMMAND_QUICK_READ_CURRENCY);
		logResultPdu(resultPdu);
		if (!isStatusSuccess(getLast2Bytes(resultPdu))) {
			String msg = "getQuickCardCurrencyBytes: Response status was not 'SUCCESS'! The response was: "
					+ statusToString(getLast2Bytes(resultPdu))
					+ ". In hex: "
					+ bytesToHex(resultPdu)
					+ "\nThe complete response was:\n"
					+ prettyPrintString(bytesToHex(resultPdu), 2);
			Log.w(TAG, msg);
			throw new TlvParsingException(msg);
		}
		byte[] rawCurrency = new byte[2];
		System.arraycopy(resultPdu, 0, rawCurrency, 0, 2);
		_ctl.log("QUICK currency = "
				+ prettyPrintString(bytesToHex(rawCurrency), 2));
		_ctl.log("QUICK currency = "
				+ Iso4217CurrencyCodes.getCurrencyAsString(rawCurrency));
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
		Log.d(TAG, "sending GET CPLC command..");
		byte[] command = EmvUtils.GPCS_GET_CPLC_COMMAND;
		Log.d(TAG, "will send byte array: " + bytesToHex(command));
		_ctl.log("sent: " + bytesToHex(command));
		byte[] resultPdu = _localIsoDep.transceive(command);
		logResultPdu(resultPdu);
		Log.d(TAG, "received byte array:  " + bytesToHex(resultPdu));
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
		Log.d(TAG, "sending ISO7816 SELECT command, with AID: "
				+ bytesToHex(appId));
		byte[] command = createSelectAid(appId);
		Log.d(TAG, "will send byte array: " + bytesToHex(command));
		_ctl.log("sent: " + bytesToHex(command));
		byte[] resultPdu = _localIsoDep.transceive(command);
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
		Log.d(TAG,
				"status: "
						+ prettyPrintString(
								bytesToHex(getLast2Bytes(resultPdu)), 2));
		Log.d(TAG, "status: " + statusToString(getLast2Bytes(resultPdu)));
		_ctl.log("received: " + bytesToHex(resultPdu));
		_ctl.log("status: "
				+ prettyPrintString(bytesToHex(getLast2Bytes(resultPdu)), 2)
				+ " - " + statusToString(getLast2Bytes(resultPdu)));
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
				Log.w(TAG, "exception while parsing BER-TLV PDU response\n"
						+ prettyPrintString(bytesToHex(resultPdu), 2), e);
			}
		}
	}
}