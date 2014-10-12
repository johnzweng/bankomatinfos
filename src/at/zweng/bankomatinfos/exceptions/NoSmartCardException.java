package at.zweng.bankomatinfos.exceptions;

/**
 * Simple exception
 * 
 * @author Johannes Zweng <johannes@zweng.at>
 */
public class NoSmartCardException extends Exception {

	private static final long serialVersionUID = -2228117684538514835L;

	public NoSmartCardException() {
	}

	public NoSmartCardException(String detailMessage) {
		super(detailMessage);
	}

	public NoSmartCardException(Throwable throwable) {
		super(throwable);
	}

	public NoSmartCardException(String detailMessage, Throwable throwable) {
		super(detailMessage, throwable);
	}

}
