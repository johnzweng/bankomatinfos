/**
 * 
 */
package at.zweng.bankomatinfos.ui;

import java.util.List;

import android.app.Activity;
import android.content.Context;
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

	/**
	 * Constructor
	 */
	public ListAdapterTransactions(Context ctx) {
		this._context = ctx;
		this._txList = AppController.getInstance().getCardInfo()
				.getTransactionLog();
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
		if (v == null) {
			LayoutInflater mInflater = (LayoutInflater) _context
					.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
			v = mInflater.inflate(R.layout.list_item_transaction, null);
		}

		TextView timeStamp = (TextView) v
				.findViewById(R.id.txListItemTimestamp);
		TextView amount = (TextView) v.findViewById(R.id.txListItemAmount);
		TextView rawData = (TextView) v.findViewById(R.id.txListRawData);

		timeStamp.setText(formatDateWithTime(tx.getTransactionTimestamp()));
		amount.setText("-" + formatBalance(tx.getAmount()) + " "
				+ tx.getCurrency());
		rawData.setText(prettyPrintHexString(bytesToHex(tx.getRawEntry())));

		return v;
	}
}
