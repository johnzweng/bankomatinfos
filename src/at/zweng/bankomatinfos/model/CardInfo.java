package at.zweng.bankomatinfos.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the data read from a bankomat card.
 * 
 * @author Johannes Zweng <johannes@zweng.at>
 */
public class CardInfo {

	private byte[] _nfcTagId;
	private boolean _quickCard;
	private boolean _maestroCard;
	private long _quickBalance;
	private String _quickCurrency;
	private List<TransactionLogEntry> _transactionLog;

	/**
	 * Constructor
	 */
	public CardInfo() {
		// create empty list
		this._transactionLog = new ArrayList<TransactionLogEntry>();
	}

	/**
	 * @return the _nfcTagId
	 */
	public byte[] getNfcTagId() {
		return _nfcTagId;
	}

	/**
	 * @param _nfcTagId
	 *            the _nfcTagId to set
	 */
	public void setNfcTagId(byte[] nfcTagId) {
		this._nfcTagId = nfcTagId;
	}

	/**
	 * @return the _transactionLog
	 */
	public List<TransactionLogEntry> getTransactionLog() {
		return _transactionLog;
	}

	/**
	 * @param _transactionLog
	 *            the _transactionLog to set
	 */
	public void setTransactionLog(List<TransactionLogEntry> transactionLog) {
		this._transactionLog = transactionLog;
	}

	/**
	 * @return the _quickCard
	 */
	public boolean isQuickCard() {
		return _quickCard;
	}

	/**
	 * @param _quickCard
	 *            the _quickCard to set
	 */
	public void setQuickCard(boolean quickCard) {
		this._quickCard = quickCard;
	}

	/**
	 * @return the _maestroCard
	 */
	public boolean isMaestroCard() {
		return _maestroCard;
	}

	/**
	 * @param _maestroCard
	 *            the _maestroCard to set
	 */
	public void setMaestroCard(boolean maestroCard) {
		this._maestroCard = maestroCard;
	}

	/**
	 * @return the _quickBalance
	 */
	public long getQuickBalance() {
		return _quickBalance;
	}

	/**
	 * @param _quickBalance
	 *            the _quickBalance to set
	 */
	public void setQuickBalance(long quickBalance) {
		this._quickBalance = quickBalance;
	}

	/**
	 * @return the _quickCurrency
	 */
	public String getQuickCurrency() {
		return _quickCurrency;
	}

	/**
	 * @param _quickCurrency
	 *            the _quickCurrency to set
	 */
	public void setQuickCurrency(String quickCurrency) {
		this._quickCurrency = quickCurrency;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "CardInfo [_nfcTagId=" + _nfcTagId + ", _quickCard="
				+ _quickCard + ", _maestroCard=" + _maestroCard
				+ ", _quickBalance=" + _quickBalance + ", _quickCurrency="
				+ _quickCurrency + ", _transactionLog=" + _transactionLog + "]";
	}

}
