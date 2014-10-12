package at.zweng.bankomatinfos.ui;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;
import at.zweng.bankomatinfos.R;

/**
 * A simple Fragment subclass, showing the list of infos.
 */
public class ResultInfosListFragment extends Fragment {

	private ListView _listView;
	private TextView _noEntriesText;
	private ListAdapterInfos _listAdapter;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_result_tx_list, container,
				false);
		_listView = (ListView) v.findViewById(R.id.listviewTxList);
		_noEntriesText = (TextView) v.findViewById(R.id.lblNoEntriesAvailable);
		_listAdapter = new ListAdapterInfos(getActivity());
		_listView.setAdapter(_listAdapter);
		showNoResultText(_listAdapter.getCount() == 0);
		return v;
	}

	/**
	 * Show or hide the no results text
	 * 
	 * @param show
	 */
	private void showNoResultText(boolean show) {
		_listView.setVisibility(show ? View.GONE : View.VISIBLE);
		_noEntriesText.setVisibility(show ? View.VISIBLE : View.GONE);
	}

}
