/**
 * 
 */
package at.zweng.bankomatinfos.ui;

import static at.zweng.bankomatinfos.util.Utils.byte2Hex;
import static at.zweng.bankomatinfos.util.Utils.bytesToHex;
import static at.zweng.bankomatinfos.util.Utils.explainCryptogramInformationByte;
import static at.zweng.bankomatinfos.util.Utils.formatBalance;
import static at.zweng.bankomatinfos.util.Utils.formatDateOnly;
import static at.zweng.bankomatinfos.util.Utils.formatDateWithTime;
import static at.zweng.bankomatinfos.util.Utils.prettyPrintString;

import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.BaseAdapter;
import android.widget.TextView;
import at.zweng.bankomatinfos.AppController;
import at.zweng.bankomatinfos.model.TransactionLogEntry;
import at.zweng.bankomatinfos2.R;

/**
 * Custom list adapter for the transaction list
 * 
 * @author Johannes Zweng <johannes@zweng.at>
 */
public class ListAdapterTransactions extends BaseAdapter {

	private Context _context;
	private List<TransactionLogEntry> _txList;
	private SparseBooleanArray itemExpandedStateMap;
	private int expandedElementId = -1;

	/**
	 * Constructor
	 */
	public ListAdapterTransactions(Context ctx) {
		this._context = ctx;
		this._txList = AppController.getInstance().getCardInfoNullSafe(ctx)
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

		if (tx.hasTime()) {
			timeStamp.setText(formatDateWithTime(tx.getTransactionTimestamp()));
		} else {
			timeStamp.setText(formatDateOnly(tx.getTransactionTimestamp()));
		}
		amount.setText("-" + formatBalance(tx.getAmount()) + " "
				+ tx.getCurrency());

		// only if the stated is expanded full tx data
		if (showFullTxData) {
			TextView cryptogramInformation = (TextView) v
					.findViewById(R.id.txListItemCryptogramInformationData);
			TextView cryptogramInformationExplained = (TextView) v
					.findViewById(R.id.txListItemCryptogramInformationDataExplained);
			TextView atc = (TextView) v.findViewById(R.id.txListItemATC);

			TextView appDefaultAction = (TextView) v
					.findViewById(R.id.txListItemApplicationDefaultAction);
			TextView unknownByte = (TextView) v
					.findViewById(R.id.txListItemUnknownByte);
			TextView unknownByteLabel = (TextView) v
					.findViewById(R.id.txListItemUnknownByteLabel);
			TextView customerExclusive = (TextView) v
					.findViewById(R.id.txListItemCustomerExclusiveData);
			TextView customerExclusiveLabel = (TextView) v
					.findViewById(R.id.txListItemCustomerExclusiveDataLabel);
			TextView rawData = (TextView) v.findViewById(R.id.txListRawData);

			cryptogramInformation.setText("0x"
					+ byte2Hex(tx.getCryptogramInformationData()));
			cryptogramInformationExplained
					.setText(explainCryptogramInformationByte(
							tx.getCryptogramInformationData(), _context));
			atc.setText(Integer.toString(tx.getAtc()));
			appDefaultAction.setText(prettyPrintString(
					bytesToHex(tx.getApplicationDefaultAction()), 2));
			if (tx.getUnknownByte()!=null) {
			unknownByte.setText(byte2Hex(tx.getUnknownByte()));
			} else {
				unknownByte.setVisibility(View.GONE);
				unknownByteLabel.setVisibility(View.GONE);
			}
			if (tx.getCustomerExclusiveData() != null) {
				customerExclusive.setText(prettyPrintString(
						bytesToHex(tx.getCustomerExclusiveData()), 2));
			} else {
				customerExclusiveLabel.setVisibility(View.GONE);
				customerExclusive.setVisibility(View.GONE);
			}

			rawData.setText(prettyPrintString(bytesToHex(tx.getRawEntry()), 2));
		}

		Animation animation;
		if (position == expandedElementId) {
			expandedElementId = -1;
			animation = new AlphaAnimation(0, 1);
			animation.setDuration(90);
			v.startAnimation(animation);
			animation = null;
		}
		return v;
	}

	/**
	 * @param position
	 */
	public void toggleItemExpandedState(int position) {

		// EXPAND:
		if (itemExpandedStateMap.get(position, false) == false) {
			expandedElementId = position;
			// and if we expanded an element collapse all other elements (so
			// that only 1 elements is expanded)
			for (int i = 0; i < itemExpandedStateMap.size(); i++) {
				if (i != position) {
					itemExpandedStateMap.put(i, false);
				}
			}
			itemExpandedStateMap.put(position, true);
		}

		// COLLAPSE:
		else {
			expandedElementId = -1;
			itemExpandedStateMap.put(position, false);
		}

		super.notifyDataSetChanged();
	}
}
