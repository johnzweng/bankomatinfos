package at.zweng.bankomatinfos.ui;

import static at.zweng.bankomatinfos.util.Utils.getAboutDialogText;
import android.app.DialogFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import at.zweng.bankomatinfos.R;

/**
 * Fragment for about dialog
 * 
 * @author Johannes Zweng <johannes@zweng.at>
 */
public class AboutDialogFragment extends DialogFragment {

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_about_dialog, container,
				false);
		getDialog().setTitle(R.string.about_dialog_title);
		TextView aboutText = (TextView) v.findViewById(R.id.creditDialog_text);
		aboutText.setTextColor(getResources().getColor(
				android.R.color.primary_text_light));
		aboutText.setText(getAboutDialogText(getActivity()));
		// close button
		Button close = (Button) v.findViewById(R.id.btnCreditDialogOk);
		close.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// When button is clicked, dismiss this dialog
				AboutDialogFragment.this.dismiss();
			}
		});
		return v;
	}
}
