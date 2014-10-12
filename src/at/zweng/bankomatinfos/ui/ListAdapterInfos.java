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
import at.zweng.bankomatinfos.model.InfoKeyValuePair;

/**
 * Custom list adapter for the card infos list (first tab page
 * 
 * @author Johannes Zweng <johannes@zweng.at>
 */
public class ListAdapterInfos extends BaseAdapter {

	private Context _context;
	private List<InfoKeyValuePair> _infoList;

	/**
	 * Constructor
	 */
	public ListAdapterInfos(Context ctx) {
		this._context = ctx;
		this._infoList = AppController.getInstance().getCardInfoNullSafe(ctx)
				.getInfoKeyValuePairs();
	}

	@Override
	public int getCount() {
		return _infoList.size();
	}

	@Override
	public Object getItem(int position) {
		return _infoList.get(position);
	}

	@Override
	public long getItemId(int position) {
		// we simply use position in list as ID for events
		return position;
	}

	@Override
	public View getView(int position, View v, ViewGroup parent) {
		InfoKeyValuePair infoItem = _infoList.get(position);
		if (infoItem.isSectionHeader()) {
			LayoutInflater mInflater = (LayoutInflater) _context
					.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
			v = mInflater.inflate(R.layout.list_item_general_info_header, null);
		} else {
			LayoutInflater mInflater = (LayoutInflater) _context
					.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
			v = mInflater.inflate(R.layout.list_item_general_info, null);
		}

		TextView infoLabel = (TextView) v.findViewById(R.id.infoListItemName);
		infoLabel.setText(infoItem.getName());

		if (!infoItem.isSectionHeader()) {
			TextView infoValue = (TextView) v
					.findViewById(R.id.infoListItemValue);
			infoValue.setText(infoItem.getValue());
		}
		return v;
	}

}
