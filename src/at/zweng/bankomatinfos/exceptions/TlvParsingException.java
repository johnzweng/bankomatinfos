package at.zweng.bankomatinfos.exceptions;

/**
 * Exception during BER-TLV parsing
 * 
 * @author Johannes Zweng <johannes@zweng.at>
 */
public class TlvParsingException extends Exception {

	private static final long serialVersionUID = -6402896717957462391L;

	public TlvParsingException() {
	}

	public TlvParsingException(String detailMessage) {
		super(detailMessage);
	}

	public TlvParsingException(Throwable throwable) {
		super(throwable);
	}

	public TlvParsingException(String detailMessage, Throwable throwable) {
		super(detailMessage, throwable);
	}

}
