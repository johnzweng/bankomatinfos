package at.zweng.bankomatinfos.iso7816emv;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.util.Log;
import at.zweng.bankomatinfos.exceptions.NoSmartCardException;
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

	/**
	 * Constructor
	 * 
	 * @param _nfcTag
	 */
	public NfcBankomatCardReader(Tag nfcTag) {
		super();
		this._nfcTag = nfcTag;
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
	 * TODO: read all the other EMV stuff too..
	 * 
	 * @return
	 * @throws IOException
	 */
	public CardInfo readAllCardData() throws IOException {
		CardInfo result = new CardInfo();
		result.setNfcTagId(_nfcTag.getId());
		readQuickInfos(result);
		readMaestroCardInfos(result);
		return result;
	}

	/**
	 * Read MAESTRO card infos from card
	 * 
	 * @param result
	 * @throws IOException
	 */
	private void readMaestroCardInfos(CardInfo result) throws IOException {
		Log.d(TAG, "check if card contains MAESTRO AID..");
		boolean isMaestroCard = testIfAidExists(APPLICATION_ID_EMV_MAESTRO_BANKOMAT);
		result.setMaestroCard(isMaestroCard);
		if (!isMaestroCard) {
			return;
		}
		readMaestroTransactions(result);
	}

	/**
	 * Tries to read the Maestro transaction log from the card and stores it
	 * into the CardInfo object
	 * 
	 * @param result
	 * @return the {@link CardInfo} object
	 * @throws IOException
	 */
	private CardInfo readMaestroTransactions(CardInfo result)
			throws IOException {
		//
		// ***************************************************
		//
		// on my Bank Austria Card the Transaction log
		// was in short EF identifier 11, in record 1-11
		// I don't know if this is the same on other cards!!
		//
		// ***************************************************
		//
		int shortEfFileIdentifier = 11;

		List<TransactionLogEntry> txList = new ArrayList<TransactionLogEntry>();

		// On my Bank Austria card I just get results for records 1-11, but
		// maybe other cards return more (just geussing). So we request here
		// records 0-20, and silently ignore errors (just log return value in
		// logcat)
		for (int currentRecord = 0; currentRecord < 21; currentRecord++) {
			Log.d(TAG, "reading tx log: READ RECORD for EF "
					+ shortEfFileIdentifier + " and RECORD " + currentRecord);
			byte[] rawRecord = readRecord(shortEfFileIdentifier, currentRecord);
			if (!isStatusSuccess(getLast2Bytes(rawRecord))) {
				Log.w(TAG, "READ RECORD for EF " + shortEfFileIdentifier
						+ " and RECORD " + currentRecord
						+ " failed. The card returned:\n"
						+ prettyPrintHexString(bytesToHex(rawRecord)));
				// if card returns error for this record, just try the next...
				continue;
			}
			TransactionLogEntry entry = parseTxLogEntryFromByteArray(rawRecord);
			if (entry != null) {
				txList.add(entry);
			} else {
				Log.w(TAG, "Could not parse transaction item, record num: "
						+ currentRecord + ", raw byte array:\n"
						+ prettyPrintHexString(bytesToHex(rawRecord)));
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
	private TransactionLogEntry parseTxLogEntryFromByteArray(byte[] rawRecord) {
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
	 * @return
	 * @throws IOException
	 */
	private byte[] readRecord(int shortEfFileIdentifier, int recordNumber)
			throws IOException {
		return _localIsoDep.transceive(createReadRecordApdu(
				shortEfFileIdentifier, recordNumber));
	}

	/**
	 * Read QUICK card infos from card
	 * 
	 * @param result
	 * @throws IOException
	 */
	private void readQuickInfos(CardInfo result) throws IOException {
		Log.d(TAG, "check if card contains QUICK AID..");
		boolean isQuickCard = testIfAidExists(APPLICATION_ID_QUICK);
		result.setQuickCard(isQuickCard);
		if (!isQuickCard) {
			return;
		}
		result.setQuickBalance(getQuickCardBalance());
		result.setQuickCurrency(getCurrencyAsString(getQuickCardCurrencyBytes()));
	}

	/**
	 * @return balance of quick card, or -1 on error
	 * @throws IOException
	 */
	private long getQuickCardBalance() throws IOException {
		byte[] resultPdu = _localIsoDep
				.transceive(ISO_COMMAND_QUICK_READ_BALANCE);
		if (!isStatusSuccess(getLast2Bytes(resultPdu))) {
			Log.w(TAG,
					"getQuickCardBalance: Response status word was not ok! Error: "
							+ statusToString(getLast2Bytes(resultPdu))
							+ ". In hex: " + bytesToHex(resultPdu));
			return -1;
		}
		return getAmountFromBytes(resultPdu);
	}

	/**
	 * @return 2-bytes long representing ISO xxxx currency
	 * @throws IOException
	 */
	private byte[] getQuickCardCurrencyBytes() throws IOException {
		byte[] resultPdu = _localIsoDep
				.transceive(ISO_COMMAND_QUICK_READ_CURRENCY);
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
		byte[] resultPdu = _localIsoDep.transceive(command);
		Log.d(TAG, "received byte array:  " + bytesToHex(resultPdu));
		return resultPdu;
	}

	/**
	 * Selects an application on the card
	 * 
	 * @return <code>true</code> if successful
	 * 
	 * @throws IOException
	 */
	private boolean selectApplication(byte[] appId) throws IOException {
		byte[] resultPdu = selectApplicationGetBytes(appId);
		return isStatusSuccess(getLast2Bytes(resultPdu));

	}

	/**
	 * @param aid
	 *            application identifier
	 * @return <code>true</code> if application exists on SmartCard,
	 *         <code>false</code> otherwise
	 * @throws IOException
	 */
	private boolean testIfAidExists(byte[] aid) throws IOException {
		if (selectApplication(aid)) {
			return true;
		} else {
			return false;
		}
	}
}
