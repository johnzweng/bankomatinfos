package at.zweng.bankomatinfos.model;

import java.util.Date;

/**
 * Represents a single entry in the cards transaction log
 * 
 * @author Johannes Zweng <johannes@zweng.at>
 */
public class TransactionLogEntry {

	private Date _transactionTimestamp;
	private long _amount;
	private String _currency;
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

}
