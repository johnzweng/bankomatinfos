package at.zweng.bankomatinfos.iso7816emv;

import static at.zweng.bankomatinfos.util.Utils.bytesToHex;

public class Iso3166CountryCodes {

	/**
	 * Returns ISO3166 country codes as string (not all countries listed here)
	 * TODO: add more country codes
	 * 
	 * @param countryCode
	 * @return
	 */
	public static String getCountryAsString(byte[] countryCode) {
		String byteString = bytesToHex(countryCode);
		if ("0040".equals(byteString)) {
			return "Austria";
		}
		if ("0070".equals(byteString)) {
			return "Bosnia and Herzegovina";
		}
		if ("0124".equals(byteString)) {
			return "Canada";
		}
		if ("0191".equals(byteString)) {
			return "Hrvatska (Croatia)";
		}
		if ("0196".equals(byteString)) {
			return "Cyprus, Republic of";
		}
		if ("0203".equals(byteString)) {
			return "Czech Republic";
		}
		if ("0208".equals(byteString)) {
			return "Denmark";
		}
		if ("0233".equals(byteString)) {
			return "Estonia";
		}
		if ("0246".equals(byteString)) {
			return "Finland";
		}
		if ("0250".equals(byteString)) {
			return "France";
		}
		if ("0276".equals(byteString)) {
			return "Germany";
		}
		if ("0300".equals(byteString)) {
			return "Greece";
		}
		if ("0348".equals(byteString)) {
			return "Hungary";
		}
		if ("0380".equals(byteString)) {
			return "Italy";
		}
		if ("0528".equals(byteString)) {
			return "Netherlands";
		}
		if ("0578".equals(byteString)) {
			return "Norway";
		}
		if ("0642".equals(byteString)) {
			return "Romania";
		}
		if ("0703".equals(byteString)) {
			return "Slovakia";
		}
		if ("0705".equals(byteString)) {
			return "Slovenia";
		}
		if ("0724".equals(byteString)) {
			return "Spain";
		}
		if ("0752".equals(byteString)) {
			return "Sweden";
		}
		if ("0756".equals(byteString)) {
			return "Switzerland";
		}
		if ("0840".equals(byteString)) {
			return "USA";
		}
		if ("0891".equals(byteString)) {
			return "Serbia and Montenegro";
		}
		return "Country Code: " + byteString + " (ISO 3166)";
	}

}
