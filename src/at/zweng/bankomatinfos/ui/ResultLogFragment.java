package at.zweng.bankomatinfos.ui;

import static at.zweng.bankomatinfos.util.Utils.TAG;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.prefs.PreferenceChangeEvent;

import at.zweng.bankomatinfos.AppController;
import at.zweng.bankomatinfos.model.CardInfo;
import at.zweng.bankomatinfos2.R;

/**
 * A simple fragment subclass, showing the result log tab.
 */
public class ResultLogFragment extends Fragment {

	private TextView _tvLog;
	private View _cachedView;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		// try to return a cached view
		if (_cachedView != null) {
			ViewParent parent = _cachedView.getParent();
			if (parent != null && parent instanceof ViewGroup) {
				((ViewGroup) parent).removeView(_cachedView);
				return _cachedView;
			}
		}
		// otherwise create new view
		View v = inflater.inflate(R.layout.fragment_result_log, container,
				false);
		_tvLog = (TextView) v.findViewById(R.id.textViewLog);

		loadDataIntoUi();
		// cache view (for quicker rebuild when we come back to this tab)
		_cachedView = v;
		return v;
	}

	/**
	 * load values into UI
	 */
	private void loadDataIntoUi() {
		AppController controller = AppController.getInstance();
		CardInfo cardInfo = controller.getCardInfoNullSafe(getActivity());
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());

        if (prefs.getBoolean("pref_public_logfile_write", true) ) {
            try {
            // Log result to file, for further analysis
                File logfile = new File(Environment.getExternalStorageDirectory(), "bankomatinfos.log");
                FileOutputStream outputStream = new FileOutputStream(logfile);
                outputStream.write(controller.getLog().getBytes());
                outputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

		if (cardInfo == null) {
			Log.e(TAG, "card info object is null");
			return;
		}
		_tvLog.setText(controller.getLog());
	}

}
