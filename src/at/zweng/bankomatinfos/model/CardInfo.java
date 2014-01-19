package at.zweng.bankomatinfos.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.content.Context;
import at.zweng.bankomatinfos.R;
import static at.zweng.bankomatinfos.util.Utils.*;

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
	private int _pinRetryCounter;
	private String _quickCurrency;
	private Context _ctx;

	private List<TransactionLogEntry> _transactionLog;
	private List<InfoKeyValuePair> _infoKeyValuePairs;

	/**
	 * Constructor
	 */
	public CardInfo(Context ctx) {
		// create empty list
		this._transactionLog = new ArrayList<TransactionLogEntry>();
		this._infoKeyValuePairs = new ArrayList<InfoKeyValuePair>();
		this._pinRetryCounter = -1;
		this._quickCurrency = "<unknown, or parsing error>";
		this._ctx = ctx;
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
		this.addKeyValuePair(new InfoKeyValuePair(_ctx.getResources()
				.getString(R.string.lbl_nfc_tag_id), "0x"
				+ bytesToHex(nfcTagId)));
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
	 * @return the _infoKeyValuePairs
	 */
	public List<InfoKeyValuePair> getInfoKeyValuePairs() {
		return _infoKeyValuePairs;
	}

	/**
	 * Add a info key-value pair
	 * 
	 * @param pair
	 */
	public void addKeyValuePair(InfoKeyValuePair pair) {
		_infoKeyValuePairs.add(pair);
	}

	/**
	 * Add a list of key-value pairs
	 * 
	 * @param pair
	 */
	public void addKeyValuePairs(List<InfoKeyValuePair> pairs) {
		_infoKeyValuePairs.addAll(pairs);
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
		this.addKeyValuePair(new InfoKeyValuePair(_ctx.getResources()
				.getString(R.string.lbl_is_quick_card), quickCard ? _ctx
				.getResources().getString(R.string.yes) : _ctx.getResources()
				.getString(R.string.no)));
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
		this.addKeyValuePair(new InfoKeyValuePair(_ctx.getResources()
				.getString(R.string.lbl_is_maestro_card), maestroCard ? _ctx
				.getResources().getString(R.string.yes) : _ctx.getResources()
				.getString(R.string.no)));
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
		this.addKeyValuePair(new InfoKeyValuePair(_ctx.getResources()
				.getString(R.string.lbl_quick_balance),
				formatBalance(quickBalance)));
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
		this.addKeyValuePair(new InfoKeyValuePair(_ctx.getResources()
				.getString(R.string.lbl_quick_currency), quickCurrency));
	}

	/**
	 * @return the _pinRetryCounter
	 */
	public int getPinRetryCounter() {
		return _pinRetryCounter;
	}

	/**
	 * @param _pinRetryCounter
	 *            the _pinRetryCounter to set
	 */
	public void setPinRetryCounter(int pinRetryCounter) {
		this._pinRetryCounter = pinRetryCounter;
		this.addKeyValuePair(new InfoKeyValuePair(_ctx.getResources()
				.getString(R.string.lbl_remaining_pin_retries), Integer
				.toString(pinRetryCounter)));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "CardInfo [_nfcTagId=" + Arrays.toString(_nfcTagId)
				+ ", _quickCard=" + _quickCard + ", _maestroCard="
				+ _maestroCard + ", _quickBalance=" + _quickBalance
				+ ", _pinRetryCounter=" + _pinRetryCounter
				+ ", _quickCurrency=" + _quickCurrency + ", _transactionLog="
				+ _transactionLog + "]";
	}

}
