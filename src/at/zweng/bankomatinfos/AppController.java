package at.zweng.bankomatinfos;

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
		_cardInfo = null;
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

}
