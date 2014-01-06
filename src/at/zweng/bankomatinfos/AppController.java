package at.zweng.bankomatinfos;


import static at.zweng.bankomatinfos.util.Utils.*;
import at.zweng.bankomatinfos.model.CardInfo;

/**
 * Very simple dummy-style controller class of this app. At the moment simply
 * used to pass the reading result around..
 * 
 * @author Johannes Zweng <johannes@zweng.at>
 */
public class AppController {

	/**
	 * singleton instance
	 */
	private static volatile AppController _instance = null;

	private CardInfo _cardInfo;
	private StringBuilder _log;

	/**
	 * Get singleton object
	 * 
	 * @param context
	 * @return
	 */
	public static synchronized AppController getInstance() {
		if (_instance == null) {
			_instance = new AppController();
		}
		return _instance;
	}

	/**
	 * Private consructor
	 * 
	 * @param ctx
	 */
	private AppController() {
		this._cardInfo = null;
		this._log = new StringBuilder();
	}

	/**
	 * @return the _cardInfo
	 */
	public CardInfo getCardInfo() {
		return _cardInfo;
	}

	/**
	 * @param _cardInfo
	 *            the _cardInfo to set
	 */
	public void setCardInfo(CardInfo cardInfo) {
		this._cardInfo = cardInfo;
	}

	/**
	 * Append line to log
	 * 
	 * @param msg
	 */
	public void log(String msg) {
		_log.append(getFullTimestampString());
		_log.append(": ");
		_log.append(msg);
		_log.append("\n");
	}

	/**
	 * @return full log
	 */
	public String getLog() {
		return _log.toString();
	}

	/**
	 * clear log
	 */
	public void clearLog() {
		_log = new StringBuilder();
	}
}
