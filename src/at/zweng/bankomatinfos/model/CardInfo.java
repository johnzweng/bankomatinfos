package at.zweng.bankomatinfos.model;

import static at.zweng.bankomatinfos.util.Utils.bytesToHex;
import static at.zweng.bankomatinfos.util.Utils.formatBalance;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.content.Context;
import at.zweng.bankomatinfos2.R;

/**
 * Represents the data read from a bankomat card.
 * 
 * @author Johannes Zweng <johannes@zweng.at>
 */
public class CardInfo {

	private byte[] _nfcTagId;
	private boolean _quickCard;
	private boolean _maestroCard;
	private boolean _containsTxLogs;
	private boolean _visaCard;
	private boolean _masterCard;
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
	 * @param headerName
	 */
	public void addSectionHeader(String headerName) {
		_infoKeyValuePairs.add(new InfoKeyValuePair(headerName));
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
		if (quickCard) {
			this.addKeyValuePair(new InfoKeyValuePair(_ctx.getResources()
					.getString(R.string.lbl_is_quick_card), quickCard ? _ctx
					.getResources().getString(R.string.yes) : _ctx
					.getResources().getString(R.string.no)));
		}
	}

	/**
	 * @return <code>true</code> if is a maestro card
	 */
	public boolean isMaestroCard() {
		return _maestroCard;
	}

	/**
	 * @return true if is a VISA card
	 */
	public boolean isVisaCard() {
		return _visaCard;
	}

	/**
	 * @return true if is a VISA card
	 */
	public boolean isMasterCard() {
		return _masterCard;
	}

	/**
	 * @return true if is one of the supported card types
	 */
	public boolean isSupportedCard() {
		return _quickCard || _maestroCard || _masterCard || _visaCard;
	}

	/**
	 * @return true card contains TX logs
	 */
	public boolean containsTxLogs() {
		return _containsTxLogs;
	}

	/**
	 * @param containsTxLogs
	 *            true if card seems to contain TX logs
	 */
	public void setContainsTxLogs(boolean containsTxLogs) {
		this._containsTxLogs = containsTxLogs;
		this.addKeyValuePair(new InfoKeyValuePair(_ctx.getResources()
				.getString(R.string.lbl_contains_emv_log_entry_tag),
				containsTxLogs ? _ctx.getResources().getString(R.string.yes)
						: _ctx.getResources().getString(R.string.no)));
	}

	/**
	 * @param maestroCard
	 *            true if is a maestro card
	 */
	public void setMaestroCard(boolean maestroCard) {
		this._maestroCard = maestroCard;
		if (maestroCard) {
			this.addKeyValuePair(new InfoKeyValuePair(_ctx.getResources()
					.getString(R.string.lbl_is_maestro_card),
					maestroCard ? _ctx.getResources().getString(R.string.yes)
							: _ctx.getResources().getString(R.string.no)));
		}
	}

	/**
	 * @param visaCard
	 *            true if is a VISA creditcard
	 */
	public void setVisaCard(boolean visaCard) {
		this._visaCard = visaCard;
		// do not show this label, if it is no VISA card
		if (visaCard) {
			this.addKeyValuePair(new InfoKeyValuePair(_ctx.getResources()
					.getString(R.string.lbl_is_visa_card), visaCard ? _ctx
					.getResources().getString(R.string.yes) : _ctx
					.getResources().getString(R.string.no)));
		}
	}

	/**
	 * @param masterCarrd
	 *            true if is a Mastercard creditcard
	 */
	public void setMasterCard(boolean masterCarrd) {
		this._masterCard = masterCarrd;
		// do not show this label, if it is no Mastercard
		if (masterCarrd) {
			this.addKeyValuePair(new InfoKeyValuePair(_ctx.getResources()
					.getString(R.string.lbl_is_mastercard), masterCarrd ? _ctx
					.getResources().getString(R.string.yes) : _ctx
					.getResources().getString(R.string.no)));
		}
	}

	/**
	 * @return the _quickBalance
	 */
	public long getQuickBalance() {
		return _quickBalance;
	}

	/**
	 * @param quickBalance
	 *            the _quickBalance to set
	 */
	public void setQuickBalance(long quickBalance) {
		this._quickBalance = quickBalance;
		this.addKeyValuePair(new InfoKeyValuePair(_ctx.getResources()
				.getString(R.string.lbl_quick_balance),
				formatBalance(quickBalance)));
	}

	/**
	 * @return the quick currency
	 */
	public String getQuickCurrency() {
		return _quickCurrency;
	}

	/**
	 * @param quickCurrency
	 *            the _quickCurrency to set
	 */
	public void setQuickCurrency(String quickCurrency) {
		this._quickCurrency = quickCurrency;
		this.addKeyValuePair(new InfoKeyValuePair(_ctx.getResources()
				.getString(R.string.lbl_quick_currency), quickCurrency));
	}

	/**
	 * @return the pin retry counter
	 */
	public int getPinRetryCounter() {
		return _pinRetryCounter;
	}

	/**
	 * @param pinRetryCounter
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
				+ _maestroCard + ", _visaCard=" + _visaCard
				+ ", _quickBalance=" + _quickBalance + ", _pinRetryCounter="
				+ _pinRetryCounter + ", _quickCurrency=" + _quickCurrency
				+ ", _ctx=" + _ctx + ", _transactionLog=" + _transactionLog
				+ ", _infoKeyValuePairs=" + _infoKeyValuePairs + "]";
	}

}
