package at.zweng.bankomatinfos.iso7816emv;

/**
 * Represents simple (not constructed) EMV tags and the corresponding value
 * bytes read from the card
 * 
 * @author Johannes Zweng <johannes@zweng.at>
 * 
 */
public class TagAndValue {

	private final EmvTag _tag;
	private final byte[] _value;

	/**
	 * Constructor
	 * 
	 * @param _tag
	 * @param _value
	 */
	public TagAndValue(EmvTag tag, byte[] value) {
		super();
		this._tag = tag;
		this._value = value;
	}

	/**
	 * @return the _tag
	 */
	public EmvTag getTag() {
		return _tag;
	}

	/**
	 * @return the _value
	 */
	public byte[] getValue() {
		return _value;
	}

}
