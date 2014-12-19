package at.zweng.bankomatinfos.ui;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import at.zweng.bankomatinfos2.R;

/**
 * A simple Fragment subclass, showing the quick tx list.
 */
public class ResultQuickTxListFragment extends Fragment {

	private ListView _listView;
	private ListAdapterQuickTransactions _listAdapter;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_result_tx_list, container,
				false);
		_listView = (ListView) v.findViewById(R.id.listviewTxList);
		_listAdapter = new ListAdapterQuickTransactions(getActivity());
		_listView.setAdapter(_listAdapter);
		_listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				_listAdapter.toggleItemExpandedState(position);
			}
		});
		return v;
	}
}
