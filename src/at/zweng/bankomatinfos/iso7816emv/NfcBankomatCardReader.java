package at.zweng.bankomatinfos.iso7816emv;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.util.Log;
import at.zweng.bankomatinfos.AppController;
import at.zweng.bankomatinfos.exceptions.NoSmartCardException;
import at.zweng.bankomatinfos.exceptions.TlvParsingException;
import at.zweng.bankomatinfos.model.CardInfo;
import at.zweng.bankomatinfos.model.TransactionLogEntry;
import static at.zweng.bankomatinfos.util.Utils.*;
import static at.zweng.bankomatinfos.iso7816emv.EmvUtils.*;

/**
 * Performs all the reading operations on a card.
 * 
 * @author Johannes Zweng <johannes@zweng.at>
 */
public class NfcBankomatCardReader {
	private Tag _nfcTag;
	private IsoDep _localIsoDep;
	private AppController _ctl;

	/**
	 * Constructor
	 * 
	 * @param _nfcTag
	 */
	public NfcBankomatCardReader(Tag nfcTag) {
		super();
		this._nfcTag = nfcTag;
		this._ctl = AppController.getInstance();
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
	 * @return
	 * @throws IOException
	 */
	public CardInfo readAllCardData() throws IOException {
		CardInfo result = new CardInfo();
		_ctl.log("Starting to read data from card..");
		result.setNfcTagId(_nfcTag.getId());
		_ctl.log("NFC Tag ID: "
				+ prettyPrintHexString(bytesToHex(_nfcTag.getId())));
		_ctl.log("Historical bytes: "
				+ prettyPrintHexString(bytesToHex(_localIsoDep
						.getHistoricalBytes())));
		result = readQuickInfos(result);
		result = readMaestroCardInfos(result);
		_ctl.log("FINISHED! :-)");
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
			result.setQuickCurrency(getCurrencyAsString(getQuickCardCurrencyBytes()));
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
	 * @throws IOException
	 */
	private CardInfo readMaestroCardInfos(CardInfo result) throws IOException {
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
			result = readMaestroEmvData(selectAidResponse, result);
		} catch (RuntimeException re) {
			_ctl.log("ERROR: Catched Exception while reading Maestro infos:\n"
					+ re + "\n" + re.getMessage());
			Log.w(TAG, "Catched Exception while reading Maestro infos: ", re);
		}
		return result;
	}

	/**
	 * Try to read some EMV data
	 * 
	 * @param selectAidResponse
	 * @param result
	 * @return
	 * @throws IOException
	 */
	private CardInfo readMaestroEmvData(byte[] selectAidResponse,
			CardInfo result) throws IOException {
		// send GET PROCESSING OPTIONS
		_ctl.log("trying to send GET PROCESSING OPTIONS command..");
		byte[] processingOptionsApdu = createGetProcessingOptionsApdu(selectAidResponse);
		_ctl.log("sent: " + bytesToHex(processingOptionsApdu));
		byte[] resultPdu = _localIsoDep.transceive(processingOptionsApdu);
		logResultPdu(resultPdu);
		if (!isStatusSuccess(getLast2Bytes(resultPdu))) {
			Log.w(TAG,
					"GET PROCESSING OPTIONS: Response status word was not ok! Error: "
							+ statusToString(getLast2Bytes(resultPdu))
							+ ". In hex: " + bytesToHex(resultPdu));
			Log.w(TAG, "will not read EMV data");
			_ctl.log("GET PROCESSING OPTIONS did not return successfully..");
			return result;
		}
		logBerTlvResponse(resultPdu);

		tryToReadLogFormat();
		tryToReadCardHolderName();
		tryToReadAllCommonSimpleTlvTags();
		tryToReadAllCommonBerTlvTags();
		result = searchForFiles(result);
		return result;
	}

	/**
	 * Try to send command for reading LOG FORMAT tag
	 * 
	 * @throws IOException
	 */
	private void tryToReadLogFormat() throws IOException {
		_ctl.log("trying to send command for getting 'Log Format'...");
		byte[] resultPdu = _localIsoDep.transceive(createGetLogFormatApdu());
		logResultPdu(resultPdu);
		logBerTlvResponse(resultPdu);
	}

	/**
	 * Try to send command for reading Cardholder Name tag
	 * 
	 * @throws IOException
	 */
	private void tryToReadCardHolderName() throws IOException {
		_ctl.log("trying to send command for getting 'Cardholder Name'...");
		byte[] resultPdu = _localIsoDep.transceive(createGetCardholderNameApdu());
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
		byte[] resultPdu = _localIsoDep.transceive(createGetAllCommonSimpleTlvApdu());
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
		byte[] resultPdu = _localIsoDep.transceive(createGetAllCommonBerTlvApdu());
		logResultPdu(resultPdu);
		logBerTlvResponse(resultPdu);
	}

	/**
	 * Just try reading all EF files from 0 to 10 and see if there will be emv
	 * data returned.
	 * 
	 * @param result
	 * @return
	 * @throws IOException
	 */
	private CardInfo searchForFiles(CardInfo result) throws IOException {
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
			// for each new EF set the consecutive error counter to 0
			consecutiveErrorRecords = 0;

			Log.d(TAG, "Trying now to read EF " + shortEfFileIdentifier + "...");

			// iterate over records within EF
			for (int currentRecord = 0; currentRecord < 256; currentRecord++) {
				if (consecutiveErrorRecords > 6) {
					// if we had 6 errors in a row we assume that no more
					// records will come and just leave this EF and go
					// to the next
					break;
				}
				byte[] responsePdu = readRecord(shortEfFileIdentifier,
						currentRecord, false);
				if (isStatusSuccess(getLast2Bytes(responsePdu))) {
					// also if we find a record set counter to 0
					consecutiveErrorRecords = 0;
					if (responsePduLooksLikeTxLogEntry(responsePdu)) {
						TransactionLogEntry entry = tryParseTxLogEntryFromByteArray(responsePdu);
						if (entry != null) {
							txList.add(entry);
						}
					} else {
						logBerTlvResponse(responsePdu);
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
	 * Try to parse the raw byte array into an object
	 * 
	 * @param rawRecord
	 * @return the parsed record or <code>null</code> if something could not be
	 *         parsed
	 */
	private TransactionLogEntry tryParseTxLogEntryFromByteArray(byte[] rawRecord) {
		if (rawRecord.length < 26) {
			// only continue if record is at least 24(+2 status) bytes long
			Log.w(TAG,
					"parseTxLogEntryFromByteArray: byte array is not long enough:\n"
							+ prettyPrintHexString(bytesToHex(rawRecord)));
			return null;
		}
		if (!"400000".equals(bytesToHex(getByteArrayPart(rawRecord, 0, 2)))) {
			// only continue if record starts with 40 00 00
			Log.w(TAG,
					"parseTxLogEntryFromByteArray: byte array doesn't start with 40 00 00:\n"
							+ prettyPrintHexString(bytesToHex(rawRecord)));
			return null;
		}
		byte[] amount = getByteArrayPart(rawRecord, 3, 6);
		byte[] currency = getByteArrayPart(rawRecord, 7, 8);
		byte[] date = getByteArrayPart(rawRecord, 9, 11);
		byte[] time = getByteArrayPart(rawRecord, 21, 23);

		TransactionLogEntry tx = new TransactionLogEntry();
		try {
			tx.setCurrency(getCurrencyAsString(currency));
			tx.setTransactionTimestamp(getTimeStampFromBcdBytes(date, time));
			tx.setAmount(getAmountFromBcdBytes(amount));
			tx.setRawEntry(rawRecord);
		} catch (Exception e) {
			Log.w(TAG,
					"Exception while trying to parse transaction entry. byte array:\n"
							+ prettyPrintHexString(bytesToHex(rawRecord)), e);
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
	 */
	private byte[] getQuickCardCurrencyBytes() throws IOException {
		_ctl.log("Reading QUICK currency");
		_ctl.log("sent: " + bytesToHex(ISO_COMMAND_QUICK_READ_CURRENCY));
		byte[] resultPdu = _localIsoDep
				.transceive(ISO_COMMAND_QUICK_READ_CURRENCY);
		logResultPdu(resultPdu);
		if (!isStatusSuccess(getLast2Bytes(resultPdu))) {
			String msg = "getQuickCardCurrencyBytes: Response status word was not ok! Error: "
					+ statusToString(getLast2Bytes(resultPdu))
					+ ". In hex: "
					+ bytesToHex((resultPdu));
			Log.w(TAG, msg);
			throw new IOException(msg);
		}
		byte[] rawCurrency = new byte[2];
		System.arraycopy(resultPdu, 0, rawCurrency, 0, 2);
		_ctl.log("QUICK currency = "
				+ prettyPrintHexString(bytesToHex(rawCurrency)));
		_ctl.log("QUICK currency = " + getCurrencyAsString(rawCurrency));
		return rawCurrency;
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
		byte[] command = createSelect(appId);
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
		_ctl.log("received: " + bytesToHex(resultPdu));
		_ctl.log("status: "
				+ prettyPrintHexString(bytesToHex(getLast2Bytes(resultPdu))));
		_ctl.log("status: " + statusToString(getLast2Bytes(resultPdu)));
	}

	/**
	 * Try to decode a response PDU as BER TLV encoded data and log it
	 * 
	 * @param resultPdu
	 */
	private void logBerTlvResponse(byte[] resultPdu) {
		if (resultPdu.length > 2) {
			try {
				_ctl.log("Trying to decode response as BER-TLV..");
				_ctl.log(prettyPrintBerTlvAPDUResponse(
						cutoffLast2Bytes(resultPdu), 0));
			} catch (TlvParsingException e) {
				_ctl.log("decoding error... maybe this data is not BER-TLV encoded?");
				Log.w(TAG, "exception while parsing BER-TLV PDU response\n"
						+ prettyPrintHexString(bytesToHex(resultPdu)), e);
			}
		}
	}

}