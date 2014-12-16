package at.zweng.bankomatinfos.iso7816emv;

import static at.zweng.bankomatinfos.util.Utils.bytesToHex;

/**
 * Currency values<br>
 * <br>
 * currencies defined in ISO4217 numeric<br>
 * https://en.wikipedia.org/wiki/ISO_4217
 */
public class Iso4217CurrencyCodes {

	/**
	 * @param currencyByte
	 *            2-byte representation of currency
	 * @return String representation of currency
	 */
	public static String getCurrencyAsString(byte[] currencyCode) {
		String byteString = bytesToHex(currencyCode);
		if ("0978".equals(byteString)) {
			return "€";
		}
		if ("0040".equals(byteString)) {
			return "ATS";
		}
		if ("0840".equals(byteString)) {
			return "USD";
		}
		if ("0826".equals(byteString)) {
			return "GBP";
		}
		if ("0946".equals(byteString)) {
			return "RON";
		}
		if ("0977".equals(byteString)) {
			return "BAM";
		}
		if ("0975".equals(byteString)) {
			return "BGN";
		}
		if ("0124".equals(byteString)) {
			return "CAD";
		}
		if ("0756".equals(byteString)) {
			return "CHF";
		}
		if ("0348".equals(byteString)) {
			return "HUF";
		}
		if ("0985".equals(byteString)) {
			return "PLN";
		}
		if ("0941".equals(byteString)) {
			return "RSD";
		}
		if ("0643".equals(byteString)) {
			return "RUB";
		}
		if ("0752".equals(byteString)) {
			return "SEK";
		}
		if ("0980".equals(byteString)) {
			return "UAH";
		}

		// special code for "not set" or "undefined"
		if ("0999".equals(byteString)) {
			// TODO localization
			return "<currency not set>";
		}
		return "ISO 4217 Currency Code " + byteString;
	}

}
