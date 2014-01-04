package at.zweng.bankomatinfos.util;

import java.nio.ByteBuffer;
import java.security.InvalidParameterException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;

/**
 * Some static helper methods
 * 
 * @author Johannes Zweng <johannes@zweng.at>
 */
public class Utils {

	/**
	 * Log tag
	 */
	public final static String TAG = "BankomatInfos";

	private static SimpleDateFormat fullTimeFormat = new SimpleDateFormat(
			"dd.MM.yyyy HH:mm:ss", Locale.US);

	/**
	 * Returns a hexadecimal String representation of a byte array (witthout
	 * spaces)
	 * 
	 * @param bytes
	 * @return
	 */
	public static String bytesToHex(byte[] bytes) {
		final char[] hexArray = { '0', '1', '2', '3', '4', '5', '6', '7', '8',
				'9', 'A', 'B', 'C', 'D', 'E', 'F' };
		char[] hexChars = new char[bytes.length * 2];
		int v;
		for (int j = 0; j < bytes.length; j++) {
			v = bytes[j] & 0xFF;
			hexChars[j * 2] = hexArray[v >>> 4];
			hexChars[j * 2 + 1] = hexArray[v & 0x0F];
		}
		return new String(hexChars);
	}

	/**
	 * Takes an string and inserts a whitespace every second char
	 * 
	 * @param in
	 *            hex string (or any other string) (ex: "0011AAEEFF")
	 * @return string with inserted whitespaces (ex: "00 11 AA EE FF")
	 */
	public static String prettyPrintHexString(String in) {
		StringBuilder buf = new StringBuilder();
		for (int i = 0; i < in.length(); i++) {
			char c = in.charAt(i);
			buf.append(c);
			int nextPos = i + 1;
			if (nextPos % 2 == 0 && nextPos != in.length()) {
				buf.append(" ");
			}
		}
		return buf.toString();
	}

	/**
	 * returns a new byte array containing only the last 2 bytes of the input
	 * array
	 * 
	 * @param input
	 * @return
	 */
	public static byte[] getLast2Bytes(byte[] input) {
		if (input == null) {
			throw new IllegalArgumentException("getLast2Bytes: input was null");
		}
		if (input.length < 2) {
			throw new IllegalArgumentException(
					"getLast2Bytes: input was shorter than 2 bytes");
		}
		byte[] output = new byte[2];
		output[0] = input[(input.length) - 2];
		output[1] = input[(input.length) - 1];
		return output;
	}

	/**
	 * Dumb helper method, only used to compare 2 byte arrays
	 * 
	 * @param first
	 * @param second
	 * @return true only if both arrays are identical
	 */
	public static boolean compare2byteArrays(byte[] first, byte[] second) {
		if (first == null)
			return false;
		if (second == null)
			return false;
		if (first.length != second.length)
			return false;
		for (int i = 0; i < first.length; i++) {
			if (first[i] != second[i]) {
				return false;
			}
		}
		return true;
	}

	/**
	 * @param balance
	 * @return
	 */
	public static String formatBalance(long balance) {
		float floatBalance = ((float) balance) / 100f;
		return String.format(Locale.US, "%.2f", floatBalance);
	}

	/**
	 * Reads a long value out of a byte array, beginning on the given offset and
	 * the given length
	 * 
	 * @param rawData
	 * @param offset
	 * @param lenght
	 * @return
	 * @throws InvalidParameterException
	 */
	public static long readLongFromBytes(byte[] rawData, int offset, int lenght)
			throws InvalidParameterException {
		if (lenght > 8) {
			throw new InvalidParameterException(
					"cannot parse more than 8 bytes into LONG type");
		}
		int i = offset + lenght;
		if (i > rawData.length) {
			throw new InvalidParameterException(
					"offset plus length exceeds input data");
		}
		if (offset < 0 || lenght < 0) {
			throw new InvalidParameterException("offset or length are <0");
		}
		byte[] cutoutBalance = new byte[8];
		System.arraycopy(rawData, offset, cutoutBalance, 8 - lenght, lenght);
		ByteBuffer buf = ByteBuffer.wrap(cutoutBalance);
		long result = buf.getLong();
		return result;
	}

	/**
	 * Display a alert dialog
	 * 
	 * @param ctx
	 * @param title
	 * @param message
	 */
	public static void displaySimpleAlertDialog(Context ctx, String title,
			String message) {
		Builder builder = new AlertDialog.Builder(ctx);
		if (title != null) {
			builder.setTitle(title);
		}
		if (message != null) {
			builder.setMessage(message);
		}
		builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
			}
		}).create().show();
	}

	/**
	 * format date
	 * 
	 * @param d
	 * @return
	 */
	public static String formatDateWithTime(Date d) {
		return fullTimeFormat.format(d);
	}

	/**
	 * Integer to hex string
	 * 
	 * @param i
	 * @return
	 */
	public static String int2Hex(int i) {
		String hex = Integer.toHexString(i);
		if (hex.length() % 2 != 0) {
			hex = "0" + hex;
		}
		return hex;
	}

	/**
	 * Remove all space characters
	 * 
	 * @param s
	 * @return
	 */
	public static String removeSpaces(String s) {
		return s.replaceAll(" ", "");
	}

	/**
	 * COnvert a given hex string into byte array
	 * 
	 * @param hexString
	 * @return
	 */
	public static byte[] fromHexString(String hexString) {
		hexString = removeSpaces(hexString);
		if (hexString.length() == 0) {
			return new byte[0];
		}
		if ((hexString.length() % 2) != 0) {
			throw new IllegalArgumentException(
					"hex string must contain an even number of characters: "
							+ hexString);
		}
		final byte result[] = new byte[hexString.length() / 2];
		final char hexChars[] = hexString.toCharArray();
		for (int i = 0; i < hexChars.length; i += 2) {
			StringBuilder curr = new StringBuilder(2);
			curr.append(hexChars[i]).append(hexChars[i + 1]);
			result[i / 2] = (byte) Integer.parseInt(curr.toString(), 16);
		}
		return result;
	}

	/**
	 * Returns a part of a byte array
	 * 
	 * @param srcArray
	 * @param startIndex
	 *            (included)
	 * @param endIndex
	 *            (included)
	 * @return
	 */
	public static byte[] getByteArrayPart(byte[] srcArray, int startIndex,
			int endIndex) {
		return Arrays.copyOfRange(srcArray, startIndex, endIndex + 1);
	}

}
