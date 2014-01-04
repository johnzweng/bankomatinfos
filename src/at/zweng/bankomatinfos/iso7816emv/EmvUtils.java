package at.zweng.bankomatinfos.iso7816emv;

import static at.zweng.bankomatinfos.util.Utils.*;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

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
	// ..
	public static final byte[] SW_CMD_CLASS_NOT_SUPPORTED = { (byte) 0x6E,
			(byte) 0x00 };
	public static final byte[] SW_CMD_ABORTED_UNKNOWN_ERR = { (byte) 0x6F,
			(byte) 0x00 };
	public static final short SW_APPLET_SELECT_FAILED = 0x6999;
	public static final short SW_CLA_NOT_SUPPORTED = 0x6E00;
	public static final short SW_INS_NOT_SUPPORTED = 0x6D00;
	public static final short SW_COMMAND_NOT_ALLOWED = 0x6986;
	public static final short SW_SECURITY_STATUS_NOT_SATISFIED = 0x6982;
	public static final short SW_DATA_INVALID = 0x6984;
	public static final short SW_CONDITIONS_NOT_SATISFIED = 0x6985;
	public static final short SW_INCORRECT_P1P2 = 0x6A86;
	public static final short SW_WRONG_LENGTH = 0x6700;
	public static final short SW_WRONG_DATA = 0x6A80;
	public static final short FILE_NOT_FOUND = 0x6A82;
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
		} else if (statusWord[0] == (byte) 0x6C) {
			return "incorrect length, second byte specifies correct length";
		} else if (compare2byteArrays(statusWord, SW_DATA_FAILURE)) {
			return "success";
		} else if (compare2byteArrays(statusWord, SW_CMD_CLASS_NOT_SUPPORTED)) {
			return "this command class (CLA) is not supported";
		} else if (compare2byteArrays(statusWord, SW_CMD_ABORTED_UNKNOWN_ERR)) {
			return "command aborted with unknown errors";
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

}
