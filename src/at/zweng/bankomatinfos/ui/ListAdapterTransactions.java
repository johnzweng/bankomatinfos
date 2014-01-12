/**
 * 
 */
package at.zweng.bankomatinfos.ui;

import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import at.zweng.bankomatinfos.AppController;
import at.zweng.bankomatinfos.R;
import at.zweng.bankomatinfos.model.TransactionLogEntry;
import static at.zweng.bankomatinfos.util.Utils.*;

/**
 * Custom list adapter for the transaction list
 * 
 * @author Johannes Zweng <johannes@zweng.at>
 */
public class ListAdapterTransactions extends BaseAdapter {

	private Context _context;
	private List<TransactionLogEntry> _txList;
	private SparseBooleanArray itemExpandedStateMap;

	/**
	 * Constructor
	 */
	public ListAdapterTransactions(Context ctx) {
		this._context = ctx;
		this._txList = AppController.getInstance().getCardInfo()
				.getTransactionLog();
		itemExpandedStateMap = new SparseBooleanArray();
	}

	@Override
	public int getCount() {
		return _txList.size();
	}

	@Override
	public Object getItem(int position) {
		return _txList.get(position);
	}

	@Override
	public long getItemId(int position) {
		// we simply use position in list as ID for events
		return position;
	}

	@Override
	public View getView(int position, View v, ViewGroup parent) {
		TransactionLogEntry tx;
		tx = _txList.get(position);
		// read setting value
		boolean showFullTxData = (itemExpandedStateMap.get(position, false));

		// TODO: maybe don't recreate view every time. But we need to check if
		// the layout is still the one we need

		// if (v == null) {
		LayoutInflater mInflater = (LayoutInflater) _context
				.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
		if (showFullTxData) {
			v = mInflater
					.inflate(R.layout.list_item_transaction_expanded, null);
		} else {
			v = mInflater.inflate(R.layout.list_item_transaction_collapsed,
					null);
		}
		// }

		TextView timeStamp = (TextView) v
				.findViewById(R.id.txListItemTimestamp);
		TextView amount = (TextView) v.findViewById(R.id.txListItemAmount);

		timeStamp.setText(formatDateWithTime(tx.getTransactionTimestamp()));
		amount.setText("-" + formatBalance(tx.getAmount()) + " "
				+ tx.getCurrency());

		// only if the stated is expanded full tx data
		if (showFullTxData) {
			TextView cryptogramInformation = (TextView) v
					.findViewById(R.id.txListItemCryptogramInformationData);
			TextView atc = (TextView) v.findViewById(R.id.txListItemATC);

			TextView appDefaultAction = (TextView) v
					.findViewById(R.id.txListItemApplicationDefaultAction);
			TextView unknownByte = (TextView) v
					.findViewById(R.id.txListItemUnknownByte);
			TextView customerEsclusive = (TextView) v
					.findViewById(R.id.txListItemCustomerExclusiveData);
			TextView rawData = (TextView) v.findViewById(R.id.txListRawData);

			cryptogramInformation.setText("0x"
					+ byte2Hex(tx.getCryptogramInformationData()));
			atc.setText(Integer.toString(tx.getAtc()));
			appDefaultAction.setText(prettyPrintHexString(bytesToHex(tx
					.getApplicationDefaultAction())));
			unknownByte.setText(byte2Hex(tx.getUnknownByte()));
			customerEsclusive.setText(prettyPrintHexString(bytesToHex(tx
					.getCustomerExclusiveData())));

			rawData.setText(prettyPrintHexString(bytesToHex(tx.getRawEntry())));
		}
		return v;
	}

	/**
	 * @param position
	 */
	public void toggleItemExpandedState(int position) {
		Log.w(TAG, "toggleItemExpandedState position: " + position);
		if (itemExpandedStateMap.get(position, false) == false) {
			itemExpandedStateMap.put(position, true);
		} else {
			itemExpandedStateMap.put(position, false);
		}
		Log.w(TAG, "toggleItemExpandedState new expanded state: "
				+ itemExpandedStateMap.get(position));
		super.notifyDataSetChanged();
	}
}
