package at.zweng.bankomatinfos.ui;

import android.app.DialogFragment;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
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
public class DonateDialogFragment extends DialogFragment {

	private final static String DONATE_WEBPAGE_URL_BITCOIN_FALLBACK = "http://johannes.zweng.at/donations.html#bitcoin";
	private final static String DONATE_WEBPAGE_URL_BANK = "http://johannes.zweng.at/donations.html#bank";
	private final static String DONATE_WEBPAGE_URL_FLATTR = "http://johannes.zweng.at/donations.html#flattr";
	private final static String DONATE_WEBPAGE_URL_PAYPAL = "http://johannes.zweng.at/donations.html#paypal";

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_donation_dialog, container,
				false);
		getDialog().setTitle(R.string.donate_dialog_title);
		TextView aboutText = (TextView) v.findViewById(R.id.donate_dialog_text);
		aboutText.setTextColor(getResources().getColor(
				android.R.color.primary_text_light));
		aboutText.setText(Html.fromHtml(getResources().getString(
				R.string.donate_text)));

		// close button
		Button bitcoin = (Button) v.findViewById(R.id.btnDonateDialogBitcoin);
		bitcoin.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				try {
					Intent bitcoinIntent = new Intent();
					bitcoinIntent.setAction(Intent.ACTION_VIEW);
					bitcoinIntent.setData(Uri
							.parse("bitcoin:19bLDxjsV63oF14P38LhDZmfKUApNeqFi6"));
					startActivity(bitcoinIntent);
				} catch (ActivityNotFoundException anfe) {
					// if no app for handling bitcoin URLs is installed:
					// go to webpage instead
					Intent donateIntent = new Intent();
					donateIntent.setAction(Intent.ACTION_VIEW);
					donateIntent.setData(Uri
							.parse(DONATE_WEBPAGE_URL_BITCOIN_FALLBACK));
					startActivity(donateIntent);
				}
			}
		});
		Button paypal = (Button) v.findViewById(R.id.btnDonateDialogPaypal);
		paypal.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent donateIntent = new Intent();
				donateIntent.setAction(Intent.ACTION_VIEW);
				donateIntent.setData(Uri.parse(DONATE_WEBPAGE_URL_PAYPAL));
				startActivity(donateIntent);
			}
		});
		Button flattr = (Button) v.findViewById(R.id.btnDonateDialogFlattr);
		flattr.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent donateIntent = new Intent();
				donateIntent.setAction(Intent.ACTION_VIEW);
				donateIntent.setData(Uri.parse(DONATE_WEBPAGE_URL_FLATTR));
				startActivity(donateIntent);
			}
		});
		Button euro = (Button) v.findViewById(R.id.btnDonateDialogEuros);
		euro.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent donateIntent = new Intent();
				donateIntent.setAction(Intent.ACTION_VIEW);
				donateIntent.setData(Uri.parse(DONATE_WEBPAGE_URL_BANK));
				startActivity(donateIntent);
			}
		});
		return v;
	}
}
