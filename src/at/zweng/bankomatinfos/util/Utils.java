package at.zweng.bankomatinfos.util;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.security.InvalidParameterException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import at.zweng.bankomatinfos.R;
import at.zweng.bankomatinfos.ui.AboutDialogFragment;
import at.zweng.bankomatinfos.ui.ChangelogDialogFragment;
import at.zweng.bankomatinfos.ui.DonateDialogFragment;

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

	private static SimpleDateFormat fullTimeWithDateFormat = new SimpleDateFormat(
			"dd.MM.yyyy HH:mm:ss", Locale.US);

	private static SimpleDateFormat dateOnlyDateFormat = new SimpleDateFormat(
			"dd.MM.yyyy", Locale.US);

	private final static SimpleDateFormat fullTimeMilliseconds = new SimpleDateFormat(
			"HH:mm:ss.SSS", Locale.US);

	/**
	 * Helper method, returns current time as string
	 * 
	 * @return
	 */
	public static String getFullTimestampString() {
		return fullTimeMilliseconds.format(new Date());
	}

	/**
	 * Returns a hexadecimal String representation of a byte array (without
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
	 * @param b
	 * @return hex representation as string
	 */
	public static String byte2Hex(byte b) {
		String[] HEX_DIGITS = { "0", "1", "2", "3", "4", "5", "6", "7", "8",
				"9", "A", "B", "C", "D", "E", "F" };
		int nb = b & 0xFF;
		int i_1 = (nb >> 4) & 0xF;
		int i_2 = nb & 0xF;
		return HEX_DIGITS[i_1] + HEX_DIGITS[i_2];
	}

	/**
	 * Takes an string and inserts a whitespace every second char
	 * 
	 * @param in
	 *            hex string (or any other string) (ex: "0011AAEEFF")
	 * @return string with inserted whitespaces (ex: "00 11 AA EE FF")
	 */
	public static String prettyPrintString(String in, int groupCount) {
		StringBuilder buf = new StringBuilder();
		for (int i = 0; i < in.length(); i++) {
			char c = in.charAt(i);
			buf.append(c);
			int nextPos = i + 1;
			if (nextPos % groupCount == 0 && nextPos != in.length()) {
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
		if (balance < 100) {
			return "0,"
					+ String.format(Locale.GERMANY, "%02d",
							Long.valueOf(balance % 100L));
		}
		String format;
		format = "%,d";
		// format = "%d";
		return String.format(Locale.GERMANY, format, (balance / 100L))
				+ ","
				+ String.format(Locale.GERMANY, "%02d",
						Long.valueOf(balance % 100L));
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
		return fullTimeWithDateFormat.format(d);
	}

	/**
	 * format date
	 * 
	 * @param d
	 * @return
	 */
	public static String formatDateOnly(Date d) {
		return dateOnlyDateFormat.format(d);
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
	 * Returns num spaces
	 * 
	 * @param num
	 * @return
	 */
	public static String getSpaces(int num) {
		StringBuilder buf = new StringBuilder(num);
		for (int i = 0; i < num; i++) {
			buf.append(" ");
		}
		return buf.toString();
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
	 * Returns the input byte array without the last 2 bytes (status word)
	 * 
	 * @param input
	 * @return
	 */
	public static byte[] cutoffLast2Bytes(byte[] input) {
		if (input == null) {
			throw new IllegalArgumentException(
					"cutoffLast2Bytes: input was null");
		}
		if (input.length < 2) {
			throw new IllegalArgumentException(
					"cutoffLast2Bytes: input was shorter than 2 bytes");
		}
		byte[] output = new byte[input.length - 2];
		for (int i = 0; i < input.length - 2; i++) {
			output[i] = input[i];
		}
		return output;
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

	/**
	 * returns copy of given byte array
	 * 
	 * source: https://code.google.com/p/javaemvreader/
	 * 
	 * @param array2Copy
	 * @return
	 */
	public static byte[] copyByteArray(byte[] array2Copy) {
		if (array2Copy == null) {
			// return new byte[0] instead?
			throw new IllegalArgumentException(
					"Argument 'array2Copy' cannot be null");
		}
		return copyByteArray(array2Copy, 0, array2Copy.length);
	}

	/**
	 * returns copy of byte-array (or part of it)
	 * 
	 * source: https://code.google.com/p/javaemvreader/
	 * 
	 * @param array2Copy
	 * @param startPos
	 * @param length
	 * @return
	 */
	public static byte[] copyByteArray(byte[] array2Copy, int startPos,
			int length) {
		if (array2Copy == null) {
			// return new byte[0] instead?
			throw new IllegalArgumentException(
					"Argument 'array2Copy' cannot be null");
		}
		if (array2Copy.length < startPos + length) {
			throw new IllegalArgumentException("startPos(" + startPos
					+ ")+length(" + length + ") > byteArray.length("
					+ array2Copy.length + ")");
		}
		byte[] copy = new byte[array2Copy.length];
		System.arraycopy(array2Copy, startPos, copy, 0, length);
		return copy;
	}

	/**
	 * Calculate int value from given byte array
	 * 
	 * source: https://code.google.com/p/javaemvreader/
	 * 
	 * @param byteArray
	 * @return
	 */
	public static int byteArrayToInt(byte[] byteArray) {
		return byteArrayToInt(byteArray, 0, byteArray.length);
	}

	/**
	 * Calculate int value from given byte array
	 * 
	 * source: https://code.google.com/p/javaemvreader/
	 * 
	 * @param byteArray
	 * @param startPos
	 * @param length
	 * @return
	 */
	public static int byteArrayToInt(byte[] byteArray, int startPos, int length) {
		if (byteArray == null) {
			throw new IllegalArgumentException(
					"Parameter 'byteArray' cannot be null");
		}
		if (length <= 0 || length > 4) {
			throw new IllegalArgumentException(
					"Length must be between 1 and 4. Length = " + length);
		}
		int value = 0;
		for (int i = startPos; i < length; i++) {
			value += ((byteArray[i] & 0xFF) << 8 * (byteArray.length - i - 1));
		}
		return value;
	}

	/**
	 * Checks if a specific bit is set source:
	 * https://code.google.com/p/javaemvreader/
	 * 
	 * @param val
	 * @param bitPos
	 *            The leftmost bit is 8 (the most significant bit)
	 * @return
	 */
	public static boolean isBitSet(byte val, int bitPos) {
		if (bitPos < 1 || bitPos > 8) {
			throw new IllegalArgumentException(
					"parameter 'bitPos' must be between 1 and 8. bitPos="
							+ bitPos);
		}
		if ((val >> (bitPos - 1) & 0x1) == 1) {
			return true;
		}
		return false;
	}

	/**
	 * Returns int value of a single byte (sets left 3 bytes to 00)
	 * 
	 * source: https://code.google.com/p/javaemvreader/
	 * 
	 * @param b
	 * @return
	 */
	public static int byteToInt(byte b) {
		return (int) b & 0xFF;
	}

	/**
	 * This returns a String with length = 8
	 * 
	 * source: https://code.google.com/p/javaemvreader/
	 * 
	 * @param val
	 * @return
	 */
	public static String byte2BinaryLiteral(byte val) {
		String s = Integer.toBinaryString(byteToInt(val));
		if (s.length() < 8) {
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < 8 - s.length(); i++) {
				sb.append('0');
			}
			sb.append(s);
			s = sb.toString();
		}
		return s;
	}

	/**
	 * convert an hex string to ASCII
	 * 
	 * @param hex
	 * @return
	 */
	public static String hex2Ascii(String hex) {
		hex.replaceAll(" ", "");
		StringBuilder output = new StringBuilder();
		for (int i = 0; i < hex.length(); i += 2) {
			String str = hex.substring(i, i + 2);
			output.append((char) Integer.parseInt(str, 16));
		}
		return output.toString();
	}

	/**
	 * The length of the returned array depends on the size of the int
	 * 
	 * @param value
	 * @return
	 */
	public static byte[] intToByteArray(int value) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		byte one = (byte) (value >>> 24);
		byte two = (byte) (value >>> 16);
		byte three = (byte) (value >>> 8);
		byte four = (byte) (value);

		boolean found = false;

		if (one > 0x00) {
			baos.write(one);
			found = true;
		}
		if (found || two > 0x00) {
			baos.write(two);
			found = true;
		}

		if (found || three > 0x00) {
			baos.write(three);
			found = true;
		}

		baos.write(four);

		return baos.toByteArray();
	}

	/**
	 * Returns a byte array with length = 4
	 * 
	 * @param value
	 * @return
	 */
	public static byte[] intToByteArray4(int value) {
		return new byte[] { (byte) (value >>> 24), (byte) (value >>> 16),
				(byte) (value >>> 8), (byte) value };
	}

	/**
	 * Pretty print a hex string with indentation
	 * 
	 * source: https://code.google.com/p/javaemvreader/
	 * 
	 * @param in
	 * @param indent
	 * @param wrapLines
	 * @return
	 */
	public static String prettyPrintHex(String in, int indent, boolean wrapLines) {
		StringBuilder buf = new StringBuilder();

		for (int i = 0; i < in.length(); i++) {
			char c = in.charAt(i);
			buf.append(c);

			int nextPos = i + 1;
			if (wrapLines && nextPos % 32 == 0 && nextPos != in.length()) {
				buf.append("\n").append(getSpaces(indent));
			} else if (nextPos % 2 == 0 && nextPos != in.length()) {
				buf.append(" ");
			}
		}
		return buf.toString();
	}

	/**
	 * Pretty print a hex string with indentation
	 * 
	 * source: https://code.google.com/p/javaemvreader/
	 * 
	 * @param in
	 * @param indent
	 * @return
	 */
	public static String prettyPrintHex(String in, int indent) {
		return prettyPrintHex(in, indent, true);
	}

	/**
	 * Returns app version string.
	 * 
	 * @param ctx
	 * @return
	 */
	public static String getAppVersion(Context ctx) {
		PackageManager pm = ctx.getPackageManager();
		try {
			PackageInfo info = pm.getPackageInfo(ctx.getPackageName(),
					PackageManager.GET_ACTIVITIES);
			if (info == null) {
				Log.w(TAG, "PackageInfo is null");
				return "?";
			}
			return info.versionName;
		} catch (NameNotFoundException e) {
			Log.w(TAG, "PackageInfo NameNotFoundException", e);
			return "?";
		}
	}

	/**
	 * show about dialog
	 */
	public static void showAboutDialog(FragmentManager fm) {
		DialogFragment aboutFragment = new AboutDialogFragment();
		aboutFragment.show(fm, "dialog_about");
	}

	/**
	 * show donation dialog
	 */
	public static void showDonationDialog(FragmentManager fm) {
		DialogFragment donateFragment = new DonateDialogFragment();
		donateFragment.show(fm, "dialog_donate");
	}

	/**
	 * show changelog dialog
	 * 
	 * @param <code>true</code> if full changelog should be shown,
	 *        <code>false</code> if only changes since last installed app
	 *        version should be shown
	 */
	public static void showChangelogDialog(FragmentManager fm,
			boolean fullChangelog) {
		DialogFragment changelogFragment = ChangelogDialogFragment
				.newInstance(fullChangelog);
		changelogFragment.show(fm, "dialog_changelog");
	}

	/**
	 * about dialog text
	 * 
	 * @param ctx
	 * @return
	 */
	public static Spanned getAboutDialogText(Context ctx) {
		StringBuilder sb = new StringBuilder();
		sb.append("<b><font color=\"#ff3232\">");
		sb.append(ctx.getResources().getString(R.string.app_name));
		sb.append("</font></b>");
		sb.append("<br/><br/>");

		sb.append("<b><font color=\"#ff3232\">Version:</font></b> ");
		sb.append(getAppVersion(ctx));
		sb.append("<br/><br/>");

		sb.append("<b><font color=\"#ff3232\">License:</font></b> ");
		sb.append("GPL-3");
		sb.append("<br/><br/>");

		sb.append("<b><font color=\"#ff3232\">Author:</font></b>");
		sb.append("<br/>Johannes Zweng<br/><a");
		sb.append("href=\"mailto:android-dev@zweng.at?subject=Feedback%20Bankomat%20Info%20App\">");
		sb.append("android-dev@zweng.at</a><br/>");
		sb.append("<i>Be curious! Have fun! :-)</i>");
		sb.append("<br/><br/>");

		// SOURCECODE
		sb.append("<b><font color=\"#ff3232\">Sourcecode:</font></b>");
		sb.append("<br/>Sourcecode of this app: https://github.com/johnzweng/bankomatinfos");
		sb.append("<br/><br/>");

		// DOWNLOADS
		sb.append("<b><font color=\"#ff3232\">Downloads:</font></b>");
		sb.append("<br/>You can find this (and all previous versions) of this app here: http://johannes.zweng.at/android/Market/BankomatInfos");
		sb.append("<br/><br/>");

		// ICON
		sb.append("<b><font color=\"#ff3232\">App icon:</font></b>");
		sb.append("<br/>Copyright owner of the app's icon: https://www.iconfinder.com/zohanimasi");
		sb.append("<br/>The icon <b>may not be used or re-distributed</b> in any form without the ");
		sb.append("permission of the icon's copyright owner!");
		sb.append("<br/><br/>");

		// CREDITS
		sb.append("<b><font color=\"#ff3232\">Credits:</font></b>");

		// javaemvreader
		sb.append("<br/>&#8226; Uses some classes from http://code.google.com/p/javaemvreader/ ");
		sb.append("project (licensed under Apache 2.0 license). Many thanks! :-)");
		sb.append("<br/>");

		// changelog
		sb.append("<br/>&#8226; Thanks to Karsten Priegnitz for his easy-to-use changelog builder: https://code.google.com/p/android-change-log/");

		sb.append("<br/>");
		return Html.fromHtml(sb.toString());
	}

	/**
	 * Returns the stackstrace as String
	 * 
	 * @param t
	 * @return
	 */
	public static String getStacktrace(Throwable t) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		t.printStackTrace(pw);
		return sw.toString();
	}

}
