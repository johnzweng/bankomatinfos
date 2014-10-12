/*
 * Copyright 2010 sasc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package at.zweng.bankomatinfos.iso7816emv;

import java.io.ByteArrayInputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import at.zweng.bankomatinfos.exceptions.TlvParsingException;
import static at.zweng.bankomatinfos.iso7816emv.EmvUtils.*;
import static at.zweng.bankomatinfos.util.Utils.*;

/**
 * Card Production Life-Cycle Data (CPLC) as defined by the Global Platform Card
 * Specification (GPCS)
 * 
 * Provides information on "who did what" prior to card issuance.
 *
 * Based on code by nelenkov
 */
public class CPLC {

	private static final Map<String, Integer> FIELD_NAMES_LENGTHS = new LinkedHashMap<String, Integer>();
	private Map<String, String> fields = new LinkedHashMap<String, String>();

	static {
		FIELD_NAMES_LENGTHS.put("IC Fabricator", 2);
		FIELD_NAMES_LENGTHS.put("IC Type", 2);
		FIELD_NAMES_LENGTHS.put("Operating System", 2);
		FIELD_NAMES_LENGTHS.put("Operating System Release Date", 2);
		FIELD_NAMES_LENGTHS.put("Operating System Release Level", 2);
		FIELD_NAMES_LENGTHS.put("IC Fabrication Date", 2);
		FIELD_NAMES_LENGTHS.put("IC Serial Number", 4);
		FIELD_NAMES_LENGTHS.put("IC Batch Identifier", 2);
		FIELD_NAMES_LENGTHS.put("IC ModuleFabricator", 2);
		FIELD_NAMES_LENGTHS.put("IC ModulePackaging Date", 2);
		FIELD_NAMES_LENGTHS.put("ICC Manufacturer", 2);
		FIELD_NAMES_LENGTHS.put("IC Embedding Date", 2);
		FIELD_NAMES_LENGTHS.put("Prepersonalizer Identifier", 2);
		FIELD_NAMES_LENGTHS.put("Prepersonalization Date", 2);
		FIELD_NAMES_LENGTHS.put("Prepersonalization Equipment", 4);
		FIELD_NAMES_LENGTHS.put("Personalizer Identifier", 2);
		FIELD_NAMES_LENGTHS.put("Personalization Date", 2);
		FIELD_NAMES_LENGTHS.put("Personalization Equipment", 4);
	}

	private CPLC() {
	}

	public static CPLC parse(byte[] raw) throws TlvParsingException {
		CPLC result = new CPLC();

		byte[] cplc = null;
		if (raw.length == 42) {
			cplc = raw;
		} else if (raw.length == 45) {
			BERTLV tlv = getNextTLV(new ByteArrayInputStream(raw));
			if (!tlv.getTag().equals(GPTags.CPLC)) {
				throw new IllegalArgumentException(
						"CPLC data not valid. Found tag: " + tlv.getTag());
			}
			cplc = tlv.getValueBytes();
		} else {
			throw new IllegalArgumentException("CPLC data not valid.");
		}
		int idx = 0;

		for (String fieldName : FIELD_NAMES_LENGTHS.keySet()) {
			int length = FIELD_NAMES_LENGTHS.get(fieldName);
			byte[] value = Arrays.copyOfRange(cplc, idx, idx + length);
			idx += length;
			String valueStr = bytesToHex(value);
			result.fields.put(fieldName, valueStr);
		}
		return result;
	}

	/**
	 * Global Platform CUID
	 * 
	 * Concatenating four data fields from the Global Platform Card Production
	 * Life Cycle (CPLC) data in the following sequence forms a card unique
	 * identifier (CUID): ICFabricatorID || ICType || ICBatchIdentifier ||
	 * ICSerialNumber (10 bytes)
	 * 
	 * @return
	 */
	public String createCardUniqueIdentifier() {
		return fields.get("IC Fabricator") + fields.get("IC Type")
				+ fields.get("IC Batch Identifier")
				+ fields.get("IC Serial Number");
	}

	@Override
	public String toString() {
		StringWriter sw = new StringWriter();
		dump(new PrintWriter(sw), 0);
		return sw.toString();
	}

	/**
	 * Prints information about this CPLC
	 * 
	 * @param pw
	 * @param indent
	 */
	public void dump(PrintWriter pw, int indent) {
		pw.println("Card Production Life Cycle Data (CPLC)");
		for (String key : fields.keySet()) {
			pw.println(String.format("%s: %s", key, fields.get(key)
					+ ("IC Fabricator".equals(key) ? " ("
							+ getFabricatorName(fields.get(key)) + ")" : "")));
		}
		pw.println(" -> Card Unique Identifier: "
				+ createCardUniqueIdentifier());
	}

	public static String getFabricatorName(String id) {
		if ("4180".equals(id)) {
			return "Atmel";
		}
		if ("4250".equals(id)) {
			return "Samsung";
		}
		if ("4790".equals(id)) {
			return "NXP";
		}
		if ("4090".equals(id)) {
			return "Infineon Technologies AG";
		}
		if ("2391".equals(id)) {
			return "AUSTRIA CARD";
		}
		if ("3060".equals(id)) {
			return "Renesas";
		}
		return "Unknown (0x" + id + ")";
	}

	public static String getOperatingSystemprovider(String id) {
		if ("2391".equals(id)) {
			return "AUSTRIA CARD OS (ACOS)";
		}
		if ("8211".equals(id)) {
			return "SCS OS";
		}
		if ("1291".equals(id) || "1981".equals(id)) {
			return "TOP";
		}
		if ("230".equals(id) || "0230".equals(id)) {
			return "G230";
		}
		if ("D000".equalsIgnoreCase(id)) {
			return "Gemalto OS";
		}
		if ("4051".equals(id) || "4A5A".equalsIgnoreCase(id)
				|| "4070".equals(id) || "4791".equals(id)) {
			return "NXP JCOP";
		}
		if ("4091".equals(id)) {
			return "Trusted Logic jTOP";
		}
		if ("8231".equals(id)) {
			return "OCS";
		}
		if ("1671".equals(id)) {
			return "G&D Sm@rtCaf";
		}
		if ("27".equals(id) || "027".equals(id) || "0027".equals(id)) {
			return "STM027";
		}
		return "Unknown (0x" + id + ")";
	}

	public static String getHumanReadableValue(final String key,
			final String val) {
		if ("IC Fabricator".equals(key)) {
			// return getFabricatorName(val) + " (0x" + val + ")";
			return getFabricatorName(val);
		}
		if ("ICC Manufacturer".equals(key)) {
			// return getFabricatorName(val) + " (0x" + val + ")";
			return getFabricatorName(val);
		}
		if ("IC ModuleFabricator".equals(key)) {
			// return getFabricatorName(val) + " (0x" + val + ")";
			return getFabricatorName(val);
		}
		if ("Prepersonalizer Identifier".equals(key)) {
			// return getFabricatorName(val) + " (0x" + val + ")";
			return getFabricatorName(val);
		}
		if ("Operating System".equals(key)) {
			// return getOperatingSystemprovider(val) + " (0x" + val + ")";
			return getOperatingSystemprovider(val);
		}
		if (key.contains("Date")) {
			Date dateVal;
			try {
				dateVal = calculateCplcDate(fromHexString(val));
			} catch (Exception e) {
				return "0x" + val;
			}
			return formatDateOnly(dateVal);
		}
		if ("IC Batch Identifier".equals(key)
				|| "Operating System Release Level".equals(key)) {
			try {
				int decimal = Integer.parseInt(val, 16);
				return Integer.toString(decimal);
			} catch (NumberFormatException nfe) {
				return "0x" + val;
			}
		}
		return "0x" + val;
	}

	/**
	 * @return the parsed fields
	 */
	public Map<String, String> getFields() {
		return fields;
	}

}
