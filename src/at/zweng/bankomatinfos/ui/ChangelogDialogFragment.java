package at.zweng.bankomatinfos.ui;

import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import at.zweng.bankomatinfos.util.ChangeLog;

/**
 * Dialog fragment for showing changelog dialog
 * 
 * @author Johannes Zweng <johannes@zweng.at>
 */
public class ChangelogDialogFragment extends DialogFragment {

	private static final String ARG_KEY_SHOW_FULL_CHANGELOG = "show_full_changelog";
	private boolean _showFullChangelog;

	/**
	 * Static method for creating a new instance
	 * 
	 * @param fullChangelog
	 *            <code>true</code> if full changelog should be shown,
	 *            <code>false</code> if only last chaanges should be shown
	 * @return
	 */
	public static ChangelogDialogFragment newInstance(boolean fullChangelog) {
		ChangelogDialogFragment f = new ChangelogDialogFragment();
		Bundle args = new Bundle();
		args.putBoolean(ARG_KEY_SHOW_FULL_CHANGELOG, fullChangelog);
		f.setArguments(args);
		return f;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (getArguments() != null) {
			_showFullChangelog = getArguments().getBoolean(
					ARG_KEY_SHOW_FULL_CHANGELOG);
		} else {
			_showFullChangelog = true;
		}
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		if (_showFullChangelog) {
			return new ChangeLog(getActivity()).getFullLogDialog();
		} else {
			return new ChangeLog(getActivity()).getLogDialog();
		}
	}
}
