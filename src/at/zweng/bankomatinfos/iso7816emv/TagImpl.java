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

import java.util.Arrays;
import static at.zweng.bankomatinfos.util.Utils.*;

/**
 * source https://code.google.com/p/javaemvreader/
 * 
 * @author sasc
 */
public class TagImpl implements EmvTag {

	byte[] idBytes;
	String name;
	String description;
	TagValueType tagValueType;
	Class tagClass;

	TagType type;

	public TagImpl(String id, TagValueType tagValueType, String name,
			String description) {
		build(fromHexString(id), tagValueType, name, description);
	}

	public TagImpl(byte[] idBytes, TagValueType tagValueType, String name,
			String description) {
		build(idBytes, tagValueType, name, description);
	}

	private void build(byte[] idBytes, TagValueType tagValueType, String name,
			String description) {
		this.idBytes = idBytes;
		this.name = name;
		this.description = description;
		this.tagValueType = tagValueType;

		if (isBitSet(this.idBytes[0], 6)) {
			this.type = TagType.CONSTRUCTED;
		} else {
			this.type = TagType.PRIMITIVE;
		}
		// Bits 8 and 7 of the first byte of the tag field indicate a class.
		// The value 00 indicates a data object of the universal class.
		// The value 01 indicates a data object of the application class.
		// The value 10 indicates a data object of the context-specific class.
		// The value 11 indicates a data object of the private class.
		byte classValue = (byte) (this.idBytes[0] >>> 6 & 0x03);
		switch (classValue) {
		case (byte) 0x00:
			tagClass = Class.UNIVERSAL;
			break;
		case (byte) 0x01:
			tagClass = Class.APPLICATION;
			break;
		case (byte) 0x02:
			tagClass = Class.CONTEXT_SPECIFIC;
			break;
		case (byte) 0x03:
			tagClass = Class.PRIVATE;
			break;
		default:
			throw new RuntimeException("UNEXPECTED TAG CLASS: "
					+ byte2BinaryLiteral(classValue) + " "
					+ bytesToHex(this.idBytes) + " " + name);
		}

	}

	@Override
	public boolean isConstructed() {
		return type == TagType.CONSTRUCTED;
	}

	@Override
	public byte[] getTagBytes() {
		return idBytes;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getDescription() {
		return description;
	}

	@Override
	public TagValueType getTagValueType() {
		return tagValueType;
	}

	@Override
	public TagType getType() {
		return type;
	}

	@Override
	public Class getTagClass() {
		return tagClass;
	}

	@Override
	public boolean equals(Object other) {
		if (!(other instanceof EmvTag))
			return false;
		EmvTag that = (EmvTag) other;
		if (this.getTagBytes().length != that.getTagBytes().length)
			return false;

		return Arrays.equals(this.getTagBytes(), that.getTagBytes());
	}

	@Override
	public int hashCode() {
		int hash = 3;
		hash = 59 * hash + Arrays.hashCode(this.idBytes);
		return hash;
	}

	@Override
	public int getNumTagBytes() {
		return idBytes.length;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("EmvTag[");
		sb.append(bytesToHex(getTagBytes()));
		sb.append("] Name=");
		sb.append(getName());
		sb.append(", TagType=");
		sb.append(getType());
		sb.append(", ValueType=");
		sb.append(getTagValueType());
		sb.append(", Class=");
		sb.append(tagClass);
		return sb.toString();
	}
}
