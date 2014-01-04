package at.zweng.bankomatinfos.ui;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import at.zweng.bankomatinfos.AppController;
import at.zweng.bankomatinfos.R;
import at.zweng.bankomatinfos.model.CardInfo;
import static at.zweng.bankomatinfos.util.Utils.*;

/**
 * A simple fragment subclass, showing the general result tab.
 */
public class ResultInfosFragment extends Fragment {

	private TextView _tvNfcTagId;
	private TextView _tvIsQuickCard;
	private TextView _tvQuickBalance;
	private TextView _tvQuickCurrency;
	private TextView _tvIsMaestroCard;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_result_infos, container,
				false);
		_tvNfcTagId = (TextView) v.findViewById(R.id.valueNfcTagId);
		_tvIsQuickCard = (TextView) v.findViewById(R.id.valueIsQuickCard);
		_tvQuickBalance = (TextView) v.findViewById(R.id.valueQuickBalance);
		_tvQuickCurrency = (TextView) v.findViewById(R.id.valueQuickCurrency);
		_tvIsMaestroCard = (TextView) v.findViewById(R.id.valueIsMaestroCard);

		loadDataIntoUi();
		return v;
	}

	/**
	 * load values into UI
	 */
	private void loadDataIntoUi() {
		AppController controller = AppController.getInstance();
		CardInfo cardInfo = controller.getCardInfo();
		if (cardInfo == null) {
			Log.e(TAG, "card info object is null");
			return;
		}
		_tvNfcTagId.setText("0x" + bytesToHex(cardInfo.getNfcTagId()));

		if (cardInfo.isQuickCard()) {
			_tvIsQuickCard.setText(getResources().getString(R.string.yes));
			_tvQuickBalance.setText(formatBalance(cardInfo.getQuickBalance()));
			_tvQuickCurrency.setText(cardInfo.getQuickCurrency());
		} else {
			_tvIsQuickCard.setText(getResources().getString(R.string.no));
			_tvQuickBalance.setText("-");
			_tvQuickCurrency.setText("-");
		}

		if (cardInfo.isMaestroCard()) {
			_tvIsMaestroCard.setText(getResources().getString(R.string.yes));
		} else {
			_tvIsMaestroCard.setText(getResources().getString(R.string.no));
		}

	}

}
