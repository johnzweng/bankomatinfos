package at.zweng.bankomatinfos.model;

import static at.zweng.bankomatinfos.util.Utils.*;
import java.util.Date;

/**
 * Represents a single entry in the cards transaction log
 * 
 * @author Johannes Zweng <johannes@zweng.at>
 */
public class TransactionLogEntry {

	private Date _transactionTimestamp;
	private long _amount;
	private int _atc;
	private String _currency;
	private byte _cryptogramInformation;
	private byte[] _applicationDefaultAction;
	private byte[] _customerExclusiveData;
	// TAG "DF 3E"
	private byte _unknownByte;
	private byte[] _rawEntry;

	/**
	 * @return the _transactionTimestamp
	 */
	public Date getTransactionTimestamp() {
		return _transactionTimestamp;
	}

	/**
	 * @param _transactionTimestamp
	 *            the _transactionTimestamp to set
	 */
	public void setTransactionTimestamp(Date transactionTimestamp) {
		this._transactionTimestamp = transactionTimestamp;
	}

	/**
	 * @return the _amount
	 */
	public long getAmount() {
		return _amount;
	}

	/**
	 * @param _amount
	 *            the _amount to set
	 */
	public void setAmount(long amount) {
		this._amount = amount;
	}

	/**
	 * @return the _currency
	 */
	public String getCurrency() {
		return _currency;
	}

	/**
	 * @param _currency
	 *            the _currency to set
	 */
	public void setCurrency(String currency) {
		this._currency = currency;
	}

	/**
	 * @return the _atc (application transaction counter)
	 */
	public int getAtc() {
		return _atc;
	}

	/**
	 * @param atc
	 *            the _atc (application transaction counter) to set
	 */
	public void setAtc(int atc) {
		this._atc = atc;
	}

	/**
	 * @return the _cryptogramInformation
	 */
	public byte getCryptogramInformationData() {
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
	public void setApplicationDefaultAction(
			byte[] applicationDefaultAction) {
		this._applicationDefaultAction = applicationDefaultAction;
	}

	/**
	 * @return the _unknownByte
	 */
	public byte getUnknownByte() {
		return _unknownByte;
	}

	/**
	 * @param unknownByte
	 *            the unknownByte to set
	 */
	public void setUnknownByte(byte unknownByte) {
		this._unknownByte = unknownByte;
	}

	/**
	 * @return the _rawEntry
	 */
	public byte[] getRawEntry() {
		return _rawEntry;
	}

	/**
	 * @param _rawEntry
	 *            the _rawEntry to set
	 */
	public void setRawEntry(byte[] rawEntry) {
		this._rawEntry = rawEntry;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "TransactionLogEntry [\n  - transactionTimestamp: "
				+ formatDateWithTime(_transactionTimestamp) + "\n  - amount: "
				+ formatBalance(_amount) + "\n  - atc: " + _atc
				+ "\n  - currency: " + _currency
				+ "\n  - cryptogramInformationData: "
				+ byte2Hex(_cryptogramInformation)
				+ "\n  - applicationDefaultAction: "
				+ bytesToHex(_applicationDefaultAction)
				+ "\n  - customerExclusiveData: "
				+ bytesToHex(_customerExclusiveData) + "\n  - unknownByte: "
				+ byte2Hex(_unknownByte) + "\n]";
	}

}
