package at.zweng.bankomatinfos.iso7816emv;

import static at.zweng.bankomatinfos.util.Utils.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

import at.zweng.bankomatinfos.exceptions.TlvParsingException;

/**
 * Util functions around EMV (https://en.wikipedia.org/wiki/EMV) standard and
 * ISO 7816<br>
 * <br>
 * See here for some basic ISO 7816-4 command infos:
 * http://www.cardwerk.com/smartcards/smartcard_standard_ISO7816-
 * 4_6_basic_interindustry_commands.aspx#chap6_1
 * 
 * @author Johannes Zweng <johannes@zweng.at>
 */
public class EmvUtils {
	/**
	 * ISO command SELECT
	 */

	public static final byte[] ISO_COMMAND_SELECT = { (byte) 0x00, (byte) 0xA4,
			(byte) 0x04, (byte) 0x00 };

	/**
	 * command read QUICK balance: 00B0820000 (in fact this is a READ BINARY
	 * command, reading EF 2 starting from offset 00, reading all bytes)
	 */
	public static final byte[] ISO_COMMAND_QUICK_READ_BALANCE = { (byte) 0x00,
			(byte) 0xB0, (byte) 0x82, (byte) 0x00, (byte) 0x00 };

	/**
	 * command read QUICK currency: 00B0810000 (in fact this is a READ BINARY
	 * command, reading EF 1 starting from offset 0x15, reading 2 bytes)
	 */
	public static final byte[] ISO_COMMAND_QUICK_READ_CURRENCY = { (byte) 0x00,
			(byte) 0xB0, (byte) 0x81, (byte) 0x15, (byte) 0x02 };

	/**
	 * EMV command GET CHALLENGE (returns 8 byte random number)
	 */
	public static final byte[] EMV_COMMAND_GET_CHALLENGE = { (byte) 0x00,
			(byte) 0x84, (byte) 0x00, (byte) 0x00, (byte) 0x00 };

	/**
	 * EMV GET DATA command for reading Tag "ATC" (Tag 9F 36)
	 */
	public static final byte[] EMV_COMMAND_GET_DATA_APP_TX_COUNTER = {
			(byte) 0x80, (byte) 0xCA, (byte) 0x9F, (byte) 0x36, (byte) 0x00 };

	/**
	 * EMV GET DATA command for reading Tag "Last Online ATC Register" (Tag 9F
	 * 13)
	 */
	public static final byte[] EMV_COMMAND_GET_DATA__LAST_ONLINE_APP_TX_COUNTER = {
			(byte) 0x80, (byte) 0xCA, (byte) 0x9F, (byte) 0x13, (byte) 0x00 };

	/**
	 * EMV GET DATA command for reading Tag "PIN retry counter" (Tag 9F 17)
	 */
	public static final byte[] EMV_COMMAND_GET_DATA_PIN_RETRY_COUNTER = {
			(byte) 0x80, (byte) 0xCA, (byte) 0x9F, (byte) 0x17, (byte) 0x00 };

	/**
	 * EMV GET DATA command for reading Tag "Log format" (Tag 9F 4F)
	 */
	public static final byte[] EMV_COMMAND_GET_DATA_LOG_FORMAT = { (byte) 0x80,
			(byte) 0xCA, (byte) 0x9F, (byte) 0x4F, (byte) 0x00 };

	/**
	 * EMV command for GET DATA
	 * "all the common BER-TLV data objects readable in the context" -->
	 * http://www.cardwerk.com/smartcards/smartcard_standard_ISO7816-
	 * 4_6_basic_interindustry_commands.aspx#chap6_9 , Table 52
	 */
	public static final byte[] EMV_COMMAND_GET_DATA_ALL_COMMON_BER_TLV = {
			(byte) 0x80, (byte) 0xCA, (byte) 0x00, (byte) 0xFF, (byte) 0x00 };
	/**
	 * EMV command for GET DATA
	 * "all the common SIMPLE-TLV data objects readable in the context" -->
	 * http://www.cardwerk.com/smartcards/smartcard_standard_ISO7816-
	 * 4_6_basic_interindustry_commands.aspx#chap6_9 , Table 52
	 */
	public static final byte[] EMV_COMMAND_GET_DATA_ALL_COMMON_SIMPLE_TLV = {
			(byte) 0x80, (byte) 0xCA, (byte) 0x02, (byte) 0xFF, (byte) 0x00 };

	/**
	 * Application ID for Quick (IEP): D040000001000002
	 */
	public static final byte[] APPLICATION_ID_QUICK = { (byte) 0xD0,
			(byte) 0x40, (byte) 0x00, (byte) 0x00, (byte) 0x01, (byte) 0x00,
			(byte) 0x00, (byte) 0x02 };
	/**
	 * Application ID for EMV Maestro Debit (Bankomat-Karte): A0000000043060
	 */
	public static final byte[] APPLICATION_ID_EMV_MAESTRO_BANKOMAT = {
			(byte) 0xA0, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x04,
			(byte) 0x30, (byte) 0x60 };

	/**
	 * Currency values<br>
	 * <br>
	 * currencies defined in ISO4217 numeric<br>
	 * https://de.wikipedia.org/wiki/ISO_4217#Aktuell_g.C3.BCltige_W.C3.
	 * A4hrungen
	 */
	public static final byte[] ISO4217_CURRENCY_EURO = { (byte) 0x09,
			(byte) 0x78 };
	public static final byte[] ISO4217_CURRENCY_ATS = { (byte) 0x00,
			(byte) 0x40 };

	//
	// Values of the status word (last 2 bytes) in the response
	//
	public static final byte[] SW_SUCCESS = { (byte) 0x90, (byte) 0x00 };
	public static final byte[] SW_DATA_FAILURE = { (byte) 0x62, (byte) 0x81 };
	public static final byte[] SW_FILEEND_REACHED = { (byte) 0x62, (byte) 0x82 };
	public static final byte[] SW_FILE_LOCKED = { (byte) 0x62, (byte) 0x83 };
	public static final byte[] SW_FILEINFO_ISO_FAILURE = { (byte) 0x62,
			(byte) 0x84 };
	public static final byte[] SW_MEMORY_ERROR = { (byte) 0x65, (byte) 0x81 };
	public static final byte[] SW_LENGTH_ERROR = { (byte) 0x67, (byte) 0x00 };
	public static final byte[] SW_FUNC_CLASS_BYTE_NOT_SUPPORTED = {
			(byte) 0x68, (byte) 0x00 };
	public static final byte[] SW_LOGIC_CHAN_NOT_SUPPORTED = { (byte) 0x68,
			(byte) 0x81 };
	public static final byte[] SW_SEC_MSG_NOT_SUPPORTED = { (byte) 0x68,
			(byte) 0x82 };
	public static final byte[] SW_CMD_NOT_ALLOWED = { (byte) 0x69, (byte) 0x00 };
	public static final byte[] SW_CMD_INCOMPATIBLE = { (byte) 0x69, (byte) 0x81 };
	public static final byte[] SW_SEC_STATE_NOT_FULFILLED = { (byte) 0x69,
			(byte) 0x82 };
	public static final byte[] SW_AUTH_METHOD_LOCKED = { (byte) 0x69,
			(byte) 0x83 };
	public static final byte[] SW_REFERENCED_DATA_LOCKED = { (byte) 0x69,
			(byte) 0x84 };
	public static final byte[] SW_USAGE_COND_NOT_FULFILLED = { (byte) 0x69,
			(byte) 0x85 };
	public static final byte[] SW_CMD_NOT_ALLOWED_NO_EF_SEL = { (byte) 0x69,
			(byte) 0x86 };
	// einige ausgelassen
	public static final byte[] SW_INCORRECT_PARAMS = { (byte) 0x6A, (byte) 0x00 };
	public static final byte[] SW_INCORRECT_DATA = { (byte) 0x6A, (byte) 0x80 };
	public static final byte[] SW_FUNC_NOT_SUPPORTED = { (byte) 0x6A,
			(byte) 0x81 };
	public static final byte[] SW_FILE_NOT_FOUND = { (byte) 0x6A, (byte) 0x82 };
	public static final byte[] SW_RECORD_NOT_FOUND = { (byte) 0x6A, (byte) 0x83 };
	public static final byte[] SW_REFERENCED_DATA_NOT_FOUND = { (byte) 0x6A,
			(byte) 0x88 };
	public static final byte[] SW_INCORRECT_PARAMETERS_P1_P2 = { (byte) 0x6A,
			(byte) 0x86 };
	// ..
	public static final byte[] SW_CMD_CLASS_NOT_SUPPORTED = { (byte) 0x6E,
			(byte) 0x00 };
	public static final byte[] SW_CMD_ABORTED_UNKNOWN_ERR = { (byte) 0x6F,
			(byte) 0x00 };
	public static final byte[] SW_INS_NOT_SUPPORTED = { (byte) 0x6D,
			(byte) 0x00 };
	public static final byte[] SW_COMMAND_NOT_ALLOWED = { (byte) 0x69,
			(byte) 0x86 };
	public static final short SW_APPLET_SELECT_FAILED = 0x6999;
	public static final short SW_CLA_NOT_SUPPORTED = 0x6E00;
	public static final short SW_SECURITY_STATUS_NOT_SATISFIED = 0x6982;
	public static final short SW_DATA_INVALID = 0x6984;
	public static final short SW_CONDITIONS_NOT_SATISFIED = 0x6985;
	public static final short SW_WRONG_LENGTH = 0x6700;
	public static final short SW_WRONG_DATA = 0x6A80;
	public static final short SW_WRONG_P1P2 = 0x6B00;
	public static final short SW_UNKNOWN = 0x6F00;

	/**
	 * Creates a APDU to send
	 * 
	 * @param command
	 * @param cmdData
	 * @param lengthExpected
	 *            the expected result length
	 * @return
	 */
	public static byte[] createAPDU(byte[] command, byte[] cmdData,
			byte lengthExpected) {
		// According to ISO7816 the command section in the PDU
		// is fixed to 4 bytes
		int lengthOfCommand = 4;

		// length of new PDU is command + data + 2
		byte[] pdu = new byte[lengthOfCommand + cmdData.length + 2];

		// the first 5 bytes (or shorter) seem to contain the command
		System.arraycopy(command, 0, pdu, 0, command.length);

		// the 5th byte contains the length of the data section (1-byte)
		pdu[lengthOfCommand] = ((byte) cmdData.length);

		// then starting at offset 5 the data is copied in
		System.arraycopy(cmdData, 0, pdu, 5, cmdData.length);

		// because the total length is length of command + length of data + 2,
		// we will still have 1 place left at the end of the array. --> LE field
		pdu[pdu.length - 1] = lengthExpected;

		//
		// Example:
		// command: 00 A4 04 00 (length 4)
		// data: D0 40 00 00 01 00 00 02 (length 8)
		// --> will result in:
		// PDU: 00 A4 04 00 08 D0 40 00 00 01 00 00 02 00 (length 14)

		// Log.d(TAG, "createAPDU returns: " + bytesToHex(pdu));
		return pdu;
	}

	/**
	 * Creates a SELECT command PDU
	 * 
	 * @param appId
	 * @return
	 */
	public static byte[] createSelect(byte[] appId) {
		byte[] result = createAPDU(ISO_COMMAND_SELECT, appId, (byte) 0);
		return result;
	}

	/**
	 * Creates a READ RECORD command PDU<br>
	 * See http://www.cardwerk.com/smartcards/smartcard_standard_ISO7816-
	 * 4_6_basic_interindustry_commands.aspx#chap6_5 for details
	 * 
	 * @param shortEfFileIdentifier
	 * @param record
	 * @return command APDU
	 */
	public static byte[] createReadRecordApdu(int shortEfFileIdentifier,
			int record) {
		int sfi = shortEfFileIdentifier;
		StringBuilder cmd = new StringBuilder();
		cmd.append("00B2");
		// P1 byte:
		cmd.append(int2Hex(record));
		// P2 byte:
		// b8 b7 b6 b5 b4 b3 b2 b1
		// b8-b4 contain the SFI
		sfi = sfi << 3;
		// and we set the right 3 bits (b1-b3) to 100
		sfi += 4;
		cmd.append(int2Hex(sfi));
		// and we set the LE field to 00:
		cmd.append("00");
		return fromHexString(cmd.toString());
	}

	/**
	 * Calculates a (hopefully) correct APDU for the EMV GET PROCESSING OPTIONS
	 * command, based on the result of the select application response of the
	 * card.
	 * 
	 * @param selectionResponse
	 * @return
	 */
	public static byte[] createGetProcessingOptionsApdu(byte[] selectionResponse) {
		// TODO implement createGetProcessingOptionsApdu!!

		// This method currently returns a static value, which I manually
		// computed based on the data of my test card. May NOT WORK with other
		// cards which send different PDOL
		// IMPLEMENT ME!!

		// In short:
		// ----------
		// When selecting an application the card includes in its response the
		// tag 9F38 "Processing Options Data Object List (PDOL)" (see also
		// http://www.eftlab.co.uk/index.php/site-map/knowledge-base/145-emv-nfc-tags)
		//
		// The card declares with the PDOL a number of tags and their expected
		// lengths which it wants to see in a following GET PROCESSING OPTIONS
		// command.
		//
		// EXAMPLE:
		// My card returns a PDOL of "9f 5c 08".
		// This contains only a single tag (9f 5c) with a length value of 08.
		// "9f 5c" is the tag "Cumulative Total Transaction Amount Upper Limit"
		// (CTTAUL)". So my card only wants to get 8 bytes in the GET PROCESSING
		// OPTIONS which represent the CTTAUL value.
		// EMV cards may also request more than 1 tag, for example terminal's
		// country or currency code.. or a random number.. etc.

		//
		// Manually building my GET PROCESSING OPTIONS command:
		//

		// "9f 5c 08" = PDOL as returned on app selection in Tag 9f38

		// Manually constructed command:
		// 80A80000 0A 83 08 FFFFFFFFFFFFFFFF 00
		// In detail:
		// 80A80000 is GET PROCESSING OPTIONS
		// 0A is total length of following PDOL (=11 bytes)
		// 83 is Tag "command template" and "Identifies the data field of a
		// command message"
		// 08 is the length of the following data
		// FFFFFFFFFFFFFFFF is the data (CTTAUL value in my case)
		// 00 is expected response length (unlimited)

		// return fromHexString("80A800000A8308000000000000000000");
		return fromHexString("80A800000A8308FFFFFFFFFFFFFFFF00");
	}

	/**
	 * The VERIFY command is used for OFFLINE authentication. The Transaction
	 * PIN Data (input) is compared with the Reference PIN Data stored in the
	 * application (ICC).
	 * 
	 * NOTE: The EMV command "Offline PIN" is vulnerable to a Man-in-the-middle
	 * attack. Terminals should request online pin verification instead!!
	 * 
	 * 
	 * Case 3 C-APDU
	 * 
	 * @param pin
	 *            the PIN to verify
	 * @param transmitInPlaintext
	 * @return
	 */
	public static byte[] createApduVerifyPIN(String pin,
			boolean transmitInPlaintext) {
		int pinLength = pin.length();
		if (pinLength < 4 || pinLength > 12) { // 0x0C
			throw new IllegalArgumentException(
					"Invalid PIN length. Must be in the range 4 to 12. Length="
							+ pinLength);
		}
		StringBuilder builder = new StringBuilder("00 20 00 ");

		// EMV book 3 Table 23 (page 88) lists 7 qualifiers,
		// but only 2 are relevant in our case (hence the use of boolean)
		byte p2QualifierPlaintextPIN = (byte) 0x80;
		byte p2QualifierEncipheredPIN = (byte) 0x88;
		if (transmitInPlaintext) {
			builder.append(byte2Hex(p2QualifierPlaintextPIN));
			byte[] tmp = new byte[8]; // Plaintext Offline PIN Block. This block
										// is split into nibbles (4 bits)
			tmp[0] = (byte) 0x20; // Control field (binary 0010xxxx)
			tmp[0] |= pinLength;
			Arrays.fill(tmp, 1, tmp.length, (byte) 0xFF); // Filler bytes

			boolean highNibble = true; // Alternate between high and low nibble
			for (int i = 0; i < pinLength; i++) { // Put each PIN digit into its
													// own nibble
				int pos = i / 2;
				int digit = Integer.parseInt(pin.substring(i, i + 1)); // Safe
																		// to
																		// use
																		// parseInt
																		// here,
																		// since
																		// the
																		// original
																		// String
																		// data
																		// came
																		// from
																		// a
																		// 'long'
				if (highNibble) {
					tmp[1 + pos] &= (byte) 0x0F; // Clear bits
					tmp[1 + pos] |= (byte) (digit << 4);

				} else {
					tmp[1 + pos] &= (byte) 0xF0; // Clear bits
					tmp[1 + pos] |= (byte) (digit);
				}
				highNibble = !highNibble;
			}
			builder.append(" 08 "); // Lc length
			builder.append(bytesToHex(tmp)); // block
		} else {
			builder.append(byte2Hex(p2QualifierEncipheredPIN));
			// TODO Enciphered PIN not supported
			throw new UnsupportedOperationException(
					"Enciphered PIN not implemented");
		}
		return fromHexString(builder.toString());
	}

	/**
	 * Check if the given 2 bytes status words mean SUCCESS
	 * 
	 * @param statusWord
	 * @return string representation of the statusword
	 */
	public static boolean isStatusSuccess(byte[] statusWord) {
		if (statusWord == null || statusWord.length != 2) {
			throw new IllegalArgumentException(
					"isStatusSuccess: status word was either null or length was != 2");
		}
		return compare2byteArrays(statusWord, SW_SUCCESS);
	}

	/**
	 * @param statusWord
	 * @return string representation of the statusword
	 */
	public static String statusToString(byte[] statusWord) {
		if (statusWord == null || statusWord.length != 2) {
			throw new IllegalArgumentException(
					"checkStatusWord: status word was either null or length was != 2");
		}

		if (compare2byteArrays(statusWord, SW_SUCCESS)) {
			return "success :-)";
		} else if (statusWord[0] == (byte) 0x61) {
			return "success :-) response can be fetched by GET RESPONSE!";
		} else if (compare2byteArrays(statusWord, SW_DATA_FAILURE)) {
			return "returned data may be not correct";
		} else if (compare2byteArrays(statusWord, SW_FILEEND_REACHED)) {
			return "file end reached, could not read LE bytes";
		} else if (compare2byteArrays(statusWord, SW_FILE_LOCKED)) {
			return "file is locked";
		} else if (compare2byteArrays(statusWord, SW_FILEINFO_ISO_FAILURE)) {
			return "file info FCI is not ISO conform";
		} else if (statusWord[0] == (byte) 0x62) {
			return "warning: state of memory not changed";
		} else if (statusWord[0] == (byte) 0x63) {
			return "warning: state of memory not changed 2";
		} else if (statusWord[0] == (byte) 0x64) {
			return "warning: execution error";
		} else if (compare2byteArrays(statusWord, SW_MEMORY_ERROR)) {
			return "memory error";
		} else if (statusWord[0] == (byte) 0x65) {
			return "warning: execution error";
		} else if (compare2byteArrays(statusWord, SW_LENGTH_ERROR)) {
			return "length error, lc or le incorrect";
		} else if (compare2byteArrays(statusWord,
				SW_FUNC_CLASS_BYTE_NOT_SUPPORTED)) {
			return "function in class byte not supported";
		} else if (compare2byteArrays(statusWord, SW_LOGIC_CHAN_NOT_SUPPORTED)) {
			return "logical channels not supported";
		} else if (compare2byteArrays(statusWord, SW_SEC_MSG_NOT_SUPPORTED)) {
			return "secure messaging not supported";
		} else if (compare2byteArrays(statusWord, SW_CMD_NOT_ALLOWED)) {
			return "command not allowed";
		} else if (compare2byteArrays(statusWord, SW_CMD_INCOMPATIBLE)) {
			return "command incompatible with file system structure";
		} else if (compare2byteArrays(statusWord, SW_SEC_STATE_NOT_FULFILLED)) {
			return "security state not fulfilled";
		} else if (compare2byteArrays(statusWord, SW_AUTH_METHOD_LOCKED)) {
			return "authentication method is locked";
		} else if (compare2byteArrays(statusWord, SW_REFERENCED_DATA_LOCKED)) {
			return "referenced data is locked";
		} else if (compare2byteArrays(statusWord, SW_USAGE_COND_NOT_FULFILLED)) {
			return "usage conditions are not fulfilled";
		} else if (compare2byteArrays(statusWord, SW_CMD_NOT_ALLOWED_NO_EF_SEL)) {
			return "command not allowed (no EF selected)";
		} else if (compare2byteArrays(statusWord, SW_INCORRECT_PARAMS)) {
			return "incorrect parameters P1/P2";
		} else if (compare2byteArrays(statusWord, SW_INCORRECT_DATA)) {
			return "incorrect data for command";
		} else if (compare2byteArrays(statusWord, SW_FUNC_NOT_SUPPORTED)) {
			return "function is not supported";
		} else if (compare2byteArrays(statusWord, SW_FILE_NOT_FOUND)) {
			return "file not found";
		} else if (compare2byteArrays(statusWord, SW_RECORD_NOT_FOUND)) {
			return "record not found";
		} else if (compare2byteArrays(statusWord, SW_REFERENCED_DATA_NOT_FOUND)) {
			return "referenced data (data objects) not found";
		} else if (compare2byteArrays(statusWord, SW_INCORRECT_PARAMETERS_P1_P2)) {
			return "incorrect parameters p1/p2";
		} else if (statusWord[0] == (byte) 0x6C) {
			return "incorrect length, second byte specifies correct length";
		} else if (compare2byteArrays(statusWord, SW_DATA_FAILURE)) {
			return "success";
		} else if (compare2byteArrays(statusWord, SW_CMD_CLASS_NOT_SUPPORTED)) {
			return "this command class (CLA) is not supported";
		} else if (compare2byteArrays(statusWord, SW_CMD_ABORTED_UNKNOWN_ERR)) {
			return "command aborted with unknown errors";
		} else if (compare2byteArrays(statusWord, SW_INS_NOT_SUPPORTED)) {
			return "instruction not supported";
		} else if (compare2byteArrays(statusWord, SW_CMD_NOT_ALLOWED)) {
			return "command not allowed";
		}
		return "----- UNKNOWN RETURN CODE!!! ------";
	}

	/**
	 * @param currencyByte
	 *            2-byte representation of currency
	 * @return String representation of currency
	 */
	public static String getCurrencyAsString(byte[] currencyBytes) {
		if (compare2byteArrays(ISO4217_CURRENCY_EURO, currencyBytes)) {
			return "Euro";
		}
		if (compare2byteArrays(ISO4217_CURRENCY_ATS, currencyBytes)) {
			return "ATS";
		}
		return "Unknown Currency 0x" + bytesToHex(currencyBytes);
	}

	/**
	 * Parses a Date object out of the given 2 byte arrays. The date and time is
	 * strangely encoded in BCD format, which means you have to read it as
	 * hexadeceimal string: for example:<br>
	 * date: 0x131231<br>
	 * time: 0x192355<br>
	 * --> which represents 31. December 2013, 19:23:55
	 * 
	 * @param date
	 * @param time
	 * @return
	 * @throws ParseException
	 */
	public static Date getTimeStampFromBcdBytes(byte[] date, byte[] time)
			throws ParseException {
		if (date == null || date.length != 3) {
			throw new IllegalArgumentException(
					"getTimeStampFromBytes: date must be exactly 3 bytes long");
		}
		if (time == null || time.length != 3) {
			throw new IllegalArgumentException(
					"getTimeStampFromBytes: time must be exactly 3 bytes long");
		}
		DateFormat df = new SimpleDateFormat("yy MM dd  hh mm ss", Locale.US);
		return df.parse(prettyPrintHexString(bytesToHex(date)) + "  "
				+ prettyPrintHexString(bytesToHex(time)));
	}

	/**
	 * Read amount value from byte array
	 * 
	 * @param amount
	 * @return
	 */
	public static long getAmountFromBytes(byte[] amount) {
		if (amount == null || amount.length < 4) {
			throw new IllegalArgumentException(
					"getAmountFromBytes: needs at least 4 bytes");
		}
		return readLongFromBytes(amount, 0, 4);
	}

	/**
	 * Read amount value from byte array whch holds the value in BCD format
	 * (which means you have to read it as hex string to get the decimal
	 * representation)
	 * 
	 * @param amount
	 *            (example: 0x2345)
	 * @return long value (example: 2345)
	 */
	public static long getAmountFromBcdBytes(byte[] amount) {
		if (amount == null || amount.length != 4) {
			throw new IllegalArgumentException(
					"getAmountFromBcdBytes: needs 4 bytes");
		}
		return Long.parseLong(bytesToHex(amount));
	}

	/**
	 * check if a response PDU looks like an TX log entry<br>
	 * 
	 * @param responsePdu
	 * @return
	 */
	public static boolean responsePduLooksLikeTxLogEntry(byte[] responsePdu) {
		// TODO: this is wrong! The log format may be custom format.
		// TODO: read cards FCI for getting locataion and format of log entries

		if (responsePdu == null) {
			return false;
		}
		// 24 bytes minimum for parsing tx log + 2 bytes status word
		if (responsePdu.length < 26) {
			return false;
		}
		// starts with bytes 40 00?
		if (!"4000".equals(bytesToHex(getByteArrayPart(responsePdu, 0, 1)))) {
			return false;
		}
		return true;
	}

	/**
	 * 
	 * source: https://code.google.com/p/javaemvreader/
	 * 
	 * @param stream
	 * @return
	 * @throws NfcException
	 */
	public static BERTLV getNextTLV(ByteArrayInputStream stream)
			throws TlvParsingException {
		if (stream.available() < 2) {
			throw new TlvParsingException(
					"Error parsing data. Available bytes < 2 . Length="
							+ stream.available());
		}

		// ISO/IEC 7816 uses neither '00' nor 'FF' as tag value.
		// Before, between, or after TLV-coded data objects,
		// '00' or 'FF' bytes without any meaning may occur
		// (for example, due to erased or modified TLV-coded data objects).

		stream.mark(0);
		int peekInt = stream.read();
		byte peekByte = (byte) peekInt;
		// peekInt == 0xffffffff indicates EOS
		while (peekInt != -1
				&& (peekByte == (byte) 0xFF || peekByte == (byte) 0x00)) {
			stream.mark(0); // Current position
			peekInt = stream.read();
			peekByte = (byte) peekInt;
		}
		stream.reset(); // Reset back to the last known position without 0x00 or
						// 0xFF

		if (stream.available() < 2) {
			throw new TlvParsingException(
					"Error parsing data. Available bytes < 2 . Length="
							+ stream.available());
		}

		byte[] tagIdBytes = readTagIdBytes(stream);

		// We need to get the raw length bytes.
		// Use quick and dirty workaround
		stream.mark(0);
		int posBefore = stream.available();
		// Now parse the lengthbyte(s)
		// This method will read all length bytes. We can then find out how many
		// bytes was read.
		int length = readTagLength(stream);
		// Now find the raw length bytes
		int posAfter = stream.available();
		stream.reset();
		byte[] lengthBytes = new byte[posBefore - posAfter];
		stream.read(lengthBytes, 0, lengthBytes.length);

		int rawLength = byteArrayToInt(lengthBytes);

		byte[] valueBytes = null;

		EmvTag tag = EMVTags.getNotNull(tagIdBytes);

		// Find VALUE bytes
		if (rawLength == 128) { // 1000 0000
			// indefinite form
			stream.mark(0);
			int prevOctet = 1;
			int curOctet = 0;
			int len = 0;
			while (true) {
				len++;
				curOctet = stream.read();
				if (curOctet < 0) {
					throw new TlvParsingException(
							"Error parsing data. TLV "
									+ "length byte indicated indefinite length, but EOS "
									+ "was reached before 0x0000 was found"
									+ stream.available());
				}
				if (prevOctet == 0 && curOctet == 0) {
					break;
				}
				prevOctet = curOctet;
			}
			len -= 2;
			valueBytes = new byte[len];
			stream.reset();
			stream.read(valueBytes, 0, len);
			length = len;
		} else {
			// definite form
			valueBytes = new byte[length];
			stream.read(valueBytes, 0, length);
		}

		// Remove any trailing 0x00 and 0xFF
		stream.mark(0);
		peekInt = stream.read();
		peekByte = (byte) peekInt;
		while (peekInt != -1
				&& (peekByte == (byte) 0xFF || peekByte == (byte) 0x00)) {
			stream.mark(0);
			peekInt = stream.read();
			peekByte = (byte) peekInt;
		}
		stream.reset(); // Reset back to the last known position without 0x00 or
						// 0xFF

		BERTLV tlv = new BERTLV(tag, length, lengthBytes, valueBytes);
		return tlv;
	}

	/**
	 * Tries to parse a byte array as EMV BER-TLV encoded data and returns a
	 * pretty formatted string (useful for logging and debugging output)<br>
	 * 
	 * source: https://code.google.com/p/javaemvreader/
	 * 
	 * @param data
	 * @param indentLength
	 * @return
	 * @throws NfcException
	 */
	public static String prettyPrintBerTlvAPDUResponse(byte[] data,
			int indentLength) throws TlvParsingException {
		StringBuilder buf = new StringBuilder();

		ByteArrayInputStream stream = new ByteArrayInputStream(data);

		while (stream.available() > 0) {
			buf.append("\n");

			buf.append(getSpaces(indentLength));

			BERTLV tlv = getNextTLV(stream);

			// Log.debug(tlv.toString());

			byte[] tagBytes = tlv.getTagBytes();
			byte[] lengthBytes = tlv.getRawEncodedLengthBytes();
			byte[] valueBytes = tlv.getValueBytes();

			EmvTag tag = tlv.getTag();

			// buf.append(" TAG: ");
			buf.append(prettyPrintHexString(bytesToHex(tagBytes)));
			buf.append("  -  ");
			buf.append(prettyPrintHexString(bytesToHex(lengthBytes)));
			buf.append(" bytes: ");
			buf.append(tag.getName());

			// int extraIndent = (lengthBytes.length * 3) + (tagBytes.length *
			// 3);
			int extraIndent = (lengthBytes.length * 2) + (tagBytes.length * 2);
			// int extraIndent = 2;

			if (tag.isConstructed()) {
				// indentLength += extraIndent;
				// Recursion
				buf.append(prettyPrintBerTlvAPDUResponse(valueBytes,
						indentLength + extraIndent));
			} else {
				buf.append("\n");
				if (tag.getTagValueType() == TagValueType.DOL) {
					buf.append(getFormattedTagAndLength(valueBytes,
							indentLength + extraIndent));
				} else {
					buf.append(getSpaces(indentLength + extraIndent));

					buf.append(prettyPrintHex(bytesToHex(valueBytes),
							indentLength + extraIndent));

					buf.append(" (");
					buf.append(getTagValueAsString(tag, valueBytes));
					buf.append(")");
				}
			}
		}
		return buf.toString();
	}

	/**
	 * read tag id bytes from EMV tag bytestream
	 * 
	 * source: https://code.google.com/p/javaemvreader/
	 * 
	 * @param stream
	 * @return
	 */
	private static byte[] readTagIdBytes(ByteArrayInputStream stream) {
		ByteArrayOutputStream tagBAOS = new ByteArrayOutputStream();
		byte tagFirstOctet = (byte) stream.read();
		tagBAOS.write(tagFirstOctet);

		// Find TAG bytes
		byte MASK = (byte) 0x1F;
		if ((tagFirstOctet & MASK) == MASK) { // EMV book 3, Page 178 or Annex
												// B1 (EMV4.3)
			// Tag field is longer than 1 byte
			do {
				int nextOctet = stream.read();
				if (nextOctet < 0) {
					break;
				}
				byte tlvIdNextOctet = (byte) nextOctet;

				tagBAOS.write(tlvIdNextOctet);

				if (!isBitSet(tlvIdNextOctet, 8)) {
					break;
				}
			} while (true);
		}
		return tagBAOS.toByteArray();
	}

	/**
	 * read length value from EMV tag bytestream
	 * 
	 * source: https://code.google.com/p/javaemvreader/
	 * 
	 * @param stream
	 * @return
	 */
	private static int readTagLength(ByteArrayInputStream stream) {
		// Find LENGTH bytes
		int length;
		int tmpLength = stream.read();

		if (tmpLength <= 127) { // 0111 1111
			// short length form
			length = tmpLength;
		} else if (tmpLength == 128) { // 1000 0000
			// length identifies indefinite form, will be set later
			length = tmpLength;
		} else {
			// long length form
			int numberOfLengthOctets = tmpLength & 127; // turn off 8th bit
			tmpLength = 0;
			for (int i = 0; i < numberOfLengthOctets; i++) {
				int nextLengthOctet = stream.read();
				tmpLength <<= 8;
				tmpLength |= nextLengthOctet;
			}
			length = tmpLength;
		}
		return length;
	}

	/**
	 * Returns a string representation of a list of Tag and Lengths (eg DOLs)<br>
	 * 
	 * source: https://code.google.com/p/javaemvreader/
	 * 
	 * @param data
	 * @param indentLength
	 * @return
	 */
	private static String getFormattedTagAndLength(byte[] data, int indentLength) {
		StringBuilder buf = new StringBuilder();
		String indent = getSpaces(indentLength);
		ByteArrayInputStream stream = new ByteArrayInputStream(data);

		boolean firstLine = true;
		while (stream.available() > 0) {
			if (firstLine) {
				firstLine = false;
			} else {
				buf.append("\n");
			}
			buf.append(indent);

			EmvTag tag = EMVTags.getNotNull(readTagIdBytes(stream));
			int length = readTagLength(stream);

			buf.append(prettyPrintHexString(bytesToHex(tag.getTagBytes())));
			buf.append(" (");
			buf.append(bytesToHex(intToByteArray(length)));
			buf.append(" bytes) -> ");
			buf.append(tag.getName());
		}
		return buf.toString();
	}

	/**
	 * Tag value as string
	 * 
	 * source: https://code.google.com/p/javaemvreader/
	 * 
	 * @param tag
	 * @param value
	 * @return
	 */
	private static String getTagValueAsString(EmvTag tag, byte[] value) {
		StringBuilder buf = new StringBuilder();
		switch (tag.getTagValueType()) {
		case TEXT:
			buf.append("=");
			buf.append(new String(value));
			break;
		case NUMERIC:
			buf.append("NUMERIC");
			break;
		case BINARY:
			buf.append("BINARY");
			break;

		case MIXED:
			buf.append("=");
			buf.append(getSafePrintChars(value));
			break;

		case DOL:
			buf.append("");
			break;
		default:
			buf.append("");
			break;
		}
		return buf.toString();
	}

	// This prints all non-control characters common to all parts of ISO/IEC
	// 8859
	// See EMV book 4 Annex B: Table 36: Common Character Set
	// source: https://code.google.com/p/javaemvreader/
	private static String getSafePrintChars(byte[] byteArray) {
		if (byteArray == null) {
			// return "" instead?
			throw new IllegalArgumentException(
					"Argument 'byteArray' cannot be null");
		}
		return getSafePrintChars(byteArray, 0, byteArray.length);
	}

	// source: https://code.google.com/p/javaemvreader/
	private static String getSafePrintChars(byte[] byteArray, int startPos,
			int length) {
		if (byteArray == null) {
			// return "" instead?
			throw new IllegalArgumentException(
					"Argument 'byteArray' cannot be null");
		}
		if (byteArray.length < startPos + length) {
			throw new IllegalArgumentException("startPos(" + startPos
					+ ")+length(" + length + ") > byteArray.length("
					+ byteArray.length + ")");
		}
		StringBuilder buf = new StringBuilder();
		for (int i = startPos; i < length; i++) {
			if (byteArray[i] >= (byte) 0x20 && byteArray[i] < (byte) 0x7F) {
				buf.append((char) byteArray[i]);
			} else {
				buf.append(".");
			}
		}
		return buf.toString();
	}

}
