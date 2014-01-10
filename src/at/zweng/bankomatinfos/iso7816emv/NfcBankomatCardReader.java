package at.zweng.bankomatinfos.iso7816emv;

import java.io.ByteArrayInputStream;
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
	 * @param performFullFileScan
	 *            <code>true</code> if we should try to scan all EFs, false if
	 *            only some well known on Austrian Bankomat Cards
	 * 
	 * @return
	 * @throws IOException
	 */
	public CardInfo readAllCardData(boolean performFullFileScan)
			throws IOException {
		CardInfo result = new CardInfo();
		_ctl.log("Starting to read data from card..");
		result.setNfcTagId(_nfcTag.getId());
		_ctl.log("NFC Tag ID: "
				+ prettyPrintHexString(bytesToHex(_nfcTag.getId())));
		_ctl.log("Historical bytes: "
				+ prettyPrintHexString(bytesToHex(_localIsoDep
						.getHistoricalBytes())));
		result = readQuickInfos(result);
		result = readMaestroCardInfos(result, performFullFileScan);
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
			result = readMaestroEmvData(selectAidResponse, result, fullFileScan);
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
	private CardInfo readMaestroEmvData(byte[] selectAidResponse,
			CardInfo result, boolean fullFileScan) throws IOException,
			TlvParsingException {

		// reading transaction logs is WRONG implemented (currently hardcoded
		// for Bank Austria style)
		// TODO: read cards FCI for getting locataion and format of log entries
		// and parse transaction log dynamically based on Log format

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
			_ctl.log("GET PROCESSING OPTIONS did not return successfully..");
			return result;
		}
		logBerTlvResponse(resultPdu);

		tryToReadLogFormat();
		result = tryToReadPinRetryCounter(result);
		tryToReadCurrentAtcValue();
		tryToReadLastOnlineAtcRegisterValue();
		tryToReadAllCommonSimpleTlvTags();
		tryToReadAllCommonBerTlvTags();
		// result = tryreadingTests(result);
		result = searchForFiles(result, fullFileScan, true);
		return result;
	}

	/**
	 * Try to send command for reading LOG FORMAT tag
	 * 
	 * @throws IOException
	 */
	private void tryToReadLogFormat() throws IOException {
		_ctl.log("trying to send GET DATA to get 'Log Format' tag from  card...");
		byte[] resultPdu = _localIsoDep
				.transceive(EMV_COMMAND_GET_DATA_LOG_FORMAT);
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
	 * Try to send GET DATA for ATC
	 * 
	 * @throws IOException
	 */
	private void tryToReadCurrentAtcValue() throws IOException {
		_ctl.log("trying to send GET DATA for getting 'ATC' (current application transaction counter)...");
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
		byte[] resultPdu = _localIsoDep
				.transceive(EMV_COMMAND_GET_DATA_APP_TX_COUNTER);
		logResultPdu(resultPdu);
		logBerTlvResponse(resultPdu);
	}

	/**
	 * CAUTION!!! If run out of PIN retries the app will be BLOCKED and your
	 * card UNUSABLE!!! Try to send VERIFY PIN <br>
	 * Only works if playntext pin is allowed in the cards CVM (cardholder
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
	private CardInfo tryreadingTests(CardInfo result) throws IOException {
		// byte[] cmd;
		byte[] resultPdu;
		//
		// _ctl.log("trying to send command GET CHALLENGE: ");
		// resultPdu = _localIsoDep.transceive(EMV_COMMAND_GET_CHALLENGE);
		// logResultPdu(resultPdu);
		//
		// _ctl.log("trying to send command GET CHALLENGE: ");
		// resultPdu = _localIsoDep.transceive(EMV_COMMAND_GET_CHALLENGE);
		// logResultPdu(resultPdu);
		//
		// _ctl.log("trying to send command GET CHALLENGE: ");
		// resultPdu = _localIsoDep.transceive(EMV_COMMAND_GET_CHALLENGE);
		// logResultPdu(resultPdu);
		//
		// _ctl.log("trying to send command GET CHALLENGE: ");
		// resultPdu = _localIsoDep.transceive(EMV_COMMAND_GET_CHALLENGE);
		// logResultPdu(resultPdu);
		//
		// _ctl.log("trying to send command GET CHALLENGE: ");
		// resultPdu = _localIsoDep.transceive(EMV_COMMAND_GET_CHALLENGE);
		// logResultPdu(resultPdu);
		//
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
		String cmd;
		cmd = "80 CA BF 30 00";
		_ctl.log("trying to send command (): " + cmd);
		resultPdu = _localIsoDep.transceive(fromHexString(cmd));
		logResultPdu(resultPdu);
		logBerTlvResponse(resultPdu);
		cmd = "80 CA 9F 35 00";
		_ctl.log("trying to send command (): " + cmd);
		resultPdu = _localIsoDep.transceive(fromHexString(cmd));
		logResultPdu(resultPdu);
		logBerTlvResponse(resultPdu);
		cmd = "80 CA 9F 50 00";
		_ctl.log("trying to send command (): " + cmd);
		resultPdu = _localIsoDep.transceive(fromHexString(cmd));
		logResultPdu(resultPdu);
		logBerTlvResponse(resultPdu);

		// QUICK:
		// -- 3F 00
		// \ -- 00 02: EF 5 REC 1: 005C 9000
		//

		return result;
	}

	/**
	 * Tries to read all common simple TLV tags
	 * 
	 * @throws IOException
	 */
	private void tryToReadAllCommonSimpleTlvTags() throws IOException {
		_ctl.log("trying to send command for getting all common simple TLV tags...");
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
		byte[] resultPdu = _localIsoDep
				.transceive(EMV_COMMAND_GET_DATA_ALL_COMMON_BER_TLV);
		logResultPdu(resultPdu);
		logBerTlvResponse(resultPdu);
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

			// TODO: better implementation of whitelisted EFs
			// TODO: and in future honor the AFL response of the card to do
			// this!!

			// ugly and hardcoded, but keep it for now
			// jump to next if EF not in whitelst
			if (!fullFileScan) {
				if (shortEfFileIdentifier != 1 && shortEfFileIdentifier != 2
						&& shortEfFileIdentifier != 3
						&& shortEfFileIdentifier != 4
						&& shortEfFileIdentifier != 11)
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
						if (responsePduLooksLikeTxLogEntry(responsePdu)) {
							TransactionLogEntry entry = tryParseTxLogEntryFromByteArray(responsePdu);
							if (entry != null) {
								txList.add(entry);
							}
						} else {
							logBerTlvResponse(responsePdu);
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
	 * Try to parse the raw byte array into an object
	 * 
	 * @param rawRecord
	 * @return the parsed record or <code>null</code> if something could not be
	 *         parsed
	 */
	private TransactionLogEntry tryParseTxLogEntryFromByteArray(byte[] rawRecord) {
		// TODO: transaction log parsing is INCORRECT!!
		// according to EMV the format may be dynamic and is specified by the
		// card in the "Log format" tag.
		// TODO: change this method to parse tx log entries dynamically based on
		// format as specified by card

		// UPDATE: according users' responses it seems that all Austrian
		// Cards use the same logging structure (of course, all the cards come
		// from the same issuer). So I keep this for now..

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
	 * Select MF (master file). Imagine this as kind of "cd /" on the card
	 * structure
	 * 
	 * @return
	 * @throws IOException
	 */
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
					+ prettyPrintHexString(bytesToHex(resultPdu));
			Log.w(TAG, msg);
			throw new TlvParsingException(msg);
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
		Log.d(TAG, "status: "
				+ prettyPrintHexString(bytesToHex(getLast2Bytes(resultPdu))));
		Log.d(TAG, "status: " + statusToString(getLast2Bytes(resultPdu)));
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