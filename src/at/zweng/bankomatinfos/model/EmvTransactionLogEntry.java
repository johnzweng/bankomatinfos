package at.zweng.bankomatinfos.model;

import static at.zweng.bankomatinfos.util.Utils.*;

/**
 * Represents a single entry in the cards transaction log
 * 
 * @author Johannes Zweng <johannes@zweng.at>
 */
public class EmvTransactionLogEntry extends AbstractTransactionLogEntry {

	private Byte _cryptogramInformation;
	private byte[] _applicationDefaultAction;
	private byte[] _customerExclusiveData;
	// TAG "DF 3E"
	private Byte _unknownByte;

	/**
	 * @return the _cryptogramInformation
	 */
	public Byte getCryptogramInformationData() {
		return _cryptogramInformation;
	}

	/**
	 * @param cryptogramInformationData
	 *            the _cryptogramInformation to set
	 */
	public void setCryptogramInformationData(byte cryptogramInformationData) {
		this._cryptogramInformation = cryptogramInformationData;
	}

	/**
	 * @return the _customerExclusiveData
	 */
	public byte[] getCustomerExclusiveData() {
		return _customerExclusiveData;
	}

	/**
	 * @param customerExclusiveData
	 *            the customerExclusiveData to set
	 */
	public void setCustomerExclusiveData(byte[] customerExclusiveData) {
		this._customerExclusiveData = customerExclusiveData;
	}

	/**
	 * @return the _applicationDefaultAction
	 */
	public byte[] getApplicationDefaultAction() {
		return _applicationDefaultAction;
	}

	/**
	 * @param applicationDefaultAction
	 *            the _applicationDefaultAction to set
	 */
	public void setApplicationDefaultAction(byte[] applicationDefaultAction) {
		this._applicationDefaultAction = applicationDefaultAction;
	}

	/**
	 * @return the _unknownByte
	 */
	public Byte getUnknownByte() {
		return _unknownByte;
	}

	/**
	 * @param unknownByte
	 *            the unknownByte to set
	 */
	public void setUnknownByte(byte unknownByte) {
		this._unknownByte = unknownByte;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(
				"EmvTransactionLogEntry [\n  - transactionTimestamp: ");

		sb.append(formatDateWithTime(_transactionTimestamp));
		sb.append("\n  - includes time: " + _hasTime);
		sb.append("\n  - amount: ");
		sb.append(formatBalance(_amount) + "\n  - atc: " + _atc);
		sb.append("\n  - currency: " + _currency);
		sb.append("\n  - cryptogramInformationData: ");
		if (_cryptogramInformation != null) {
			sb.append(byte2Hex(_cryptogramInformation));
			sb.append("\n  - applicationDefaultAction: ");
			sb.append(bytesToHexNullAllowed(_applicationDefaultAction));
		}
		if (_customerExclusiveData != null) {
			sb.append("\n  - customerExclusiveData: ");
			sb.append(bytesToHex(_customerExclusiveData));
		}
		if (_unknownByte != null) {
			sb.append("\n  - unknownByte: " + byte2Hex(_unknownByte));
		}
		sb.append("\n");
		return sb.toString();
	}
}
