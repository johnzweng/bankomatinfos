package at.zweng.bankomatinfos.model;

import static at.zweng.bankomatinfos.util.Utils.byte2Hex;
import static at.zweng.bankomatinfos.util.Utils.formatBalance;
import static at.zweng.bankomatinfos.util.Utils.formatDateWithTime;

/**
 * represents transaction logs for Quick (Austrian e-purse system)
 * 
 * @author john
 */
public class QuickTransactionLogEntry extends AbstractTransactionLogEntry {

	// in superclass:
	// protected Date _transactionTimestamp;
	// protected long _amount;
	// protected int _atc;
	// protected String _currency;
	// private byte[] _rawEntry;
	// protected boolean _hasTime;

	private long _amount2;
	private long _remainingBalance;
	private long _terminalInfos1;
	private long _terminalInfos2;
	private Byte _unknownByte1;
	private Byte _unknownByte2;

	public long getAmount2() {
		return _amount2;
	}

	public void setAmount2(long _amount2) {
		this._amount2 = _amount2;
	}

	public Byte getUnknownByte1() {
		return _unknownByte1;
	}

	public void setUnknownByte1(Byte _unknownByte1) {
		this._unknownByte1 = _unknownByte1;
	}

	public Byte getUnknownByte2() {
		return _unknownByte2;
	}

	public void setUnknownByte2(Byte _unknownByte2) {
		this._unknownByte2 = _unknownByte2;
	}

	public long getRemainingBalance() {
		return _remainingBalance;
	}

	public void setRemainingBalance(long _remainingBalance) {
		this._remainingBalance = _remainingBalance;
	}

	public long getTerminalInfos1() {
		return _terminalInfos1;
	}

	public void setTerminalInfos1(long _terminalInfos1) {
		this._terminalInfos1 = _terminalInfos1;
	}

	public long getTerminalInfos2() {
		return _terminalInfos2;
	}

	public void setTerminalInfos2(long _terminalInfos2s) {
		this._terminalInfos2 = _terminalInfos2s;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(
				"QuickTransactionLogEntry [\n  - transactionTimestamp: ");
		sb.append(formatDateWithTime(_transactionTimestamp));
		sb.append("\n  - includes time: " + _hasTime);
		sb.append("\n  - amount: ");
		sb.append(formatBalance(_amount));
		sb.append("\n  - amount2: ");
		sb.append(formatBalance(_amount2));
		sb.append("\n  - remaining balance: ");
		sb.append(formatBalance(_remainingBalance));
		sb.append("\n  - atc: " + _atc);
		sb.append("\n  - currency: " + _currency);
		if (_unknownByte1 != null) {
			sb.append("\n  - unknown byte 1: ");
			sb.append(byte2Hex(_unknownByte1));
		}
		if (_unknownByte2 != null) {
			sb.append("\n  - unknown byte 2: ");
			sb.append(byte2Hex(_unknownByte2));
		}
		sb.append("\n  - terminal info 1: " + _terminalInfos1);
		sb.append("\n  - terminal info 2: " + _terminalInfos2);
		sb.append("\n");
		return sb.toString();
	}

}
