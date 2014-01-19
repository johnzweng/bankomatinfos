package at.zweng.bankomatinfos.model;

public class InfoKeyValuePair {
	private final String _name;
	private final String _value;

	/**
	 * 
	 * @param _name
	 * @param _value
	 */
	public InfoKeyValuePair(String name, String value) {
		super();
		this._name = name;
		this._value = value;
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

}
