package at.zweng.bankomatinfos.model;

public class InfoKeyValuePair {
	private final String _name;
	private final String _value;
	private final boolean _isSectionHeader;

	/**
	 * @param _name
	 * @param _value
	 */
	public InfoKeyValuePair(String name, String value) {
		super();
		this._name = name;
		this._value = value;
		this._isSectionHeader = false;
	}

	public InfoKeyValuePair(String sectionHeaderName) {
		super();
		this._name = sectionHeaderName;
		this._value = null;
		this._isSectionHeader = true;
	}

	/**
	 * @return the _name
	 */
	public String getName() {
		return _name;
	}

	/**
	 * @return the _value
	 */
	public String getValue() {
		return _value;
	}

	/**
	 * @return _isSectionHeader
	 */
	public boolean isSectionHeader() {
		return _isSectionHeader;
	}

}
