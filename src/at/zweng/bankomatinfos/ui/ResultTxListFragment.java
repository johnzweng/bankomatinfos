package at.zweng.bankomatinfos.ui;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import at.zweng.bankomatinfos.AppController;
import at.zweng.bankomatinfos.R;

/**
 * A simple Fragment subclass, showing the transaction list.
 */
public class ResultTxListFragment extends Fragment {

	private ListView _listView;
	private TextView _noEntriesText;
	private ListAdapterTransactions _listAdapter;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_result_tx_list, container,
				false);
		_listView = (ListView) v.findViewById(R.id.listviewTxList);
		_noEntriesText = (TextView) v.findViewById(R.id.lblNoEntriesAvailable);
		_listAdapter = new ListAdapterTransactions(getActivity());
		_listView.setAdapter(_listAdapter);
		_listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				_listAdapter.toggleItemExpandedState(position);
			}
		});
		showNoResultText(_listAdapter.getCount() == 0);
		return v;
	}

	/**
	 * Show or hide the no results text
	 * 
	 * @param show
	 */
	private void showNoResultText(boolean show) {
		AppController.getInstance().getCardInfoNullSafe(getActivity())
				.containsTxLogs();
		if (show) {
			if (!AppController.getInstance().getCardInfoNullSafe(getActivity())
					.containsTxLogs()) {
				_noEntriesText.setText(R.string.tx_list_no_tx_log_tag_found);
			} else {
				_noEntriesText.setText(R.string.tx_list_no_tx_found);
			}
			_listView.setVisibility(View.GONE);
			_noEntriesText.setVisibility(View.VISIBLE);
		} else {
			_listView.setVisibility(View.VISIBLE);
			_noEntriesText.setVisibility(View.GONE);
		}
	}
}
