package at.zweng.bankomatinfos.ui;

import static at.zweng.bankomatinfos.util.Utils.*;

import java.io.IOException;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.TagLostException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import at.zweng.bankomatinfos.AppController;
import at.zweng.bankomatinfos.R;
import at.zweng.bankomatinfos.exceptions.NoSmartCardException;
import at.zweng.bankomatinfos.iso7816emv.NfcBankomatCardReader;
import at.zweng.bankomatinfos.model.CardInfo;
import at.zweng.bankomatinfos.util.ChangeLog;
import at.zweng.bankomatinfos.util.CustomAlertDialog;

/**
 * Startup activity
 * 
 * @author Johannes Zweng <johannes@zweng.at>
 * 
 */
public class MainActivity extends Activity {

	// for NFC stuff
	private PendingIntent _pendingIntent;
	private IntentFilter[] _filters;
	private String[][] _techLists;
	private NfcAdapter _nfcAdapter;

	// View elements
	private View _viewNfcLogo;
	private View _viewTextViewShowCard;
	private View _viewProgressStatus;

	private CardInfo _cardReadingResults;
	private ReadNfcCardTask _readCardTask;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// find view elements
		_viewProgressStatus = findViewById(R.id.read_card_status);
		_viewNfcLogo = findViewById(R.id.imageViewNfcLogo);
		_viewTextViewShowCard = findViewById(R.id.textViewYourCardPlease);

		// NFC stuff
		_pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this,
				getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
		_filters = new IntentFilter[] { new IntentFilter(
				NfcAdapter.ACTION_TECH_DISCOVERED) };
		_techLists = new String[][] { { "android.nfc.tech.NfcA" } };
		_nfcAdapter = NfcAdapter.getDefaultAdapter(this);

		// create last changes dialog if needed
		displayWhatsNew();

	}

	@Override
	protected void onResume() {
		super.onResume();
		if (!isNfcAvailable()) {
			Intent intent = new Intent(this, NfcDisabledActivity.class);
			startActivity(intent);
			this.finish();
			return;
		}

		if (_nfcAdapter != null) {
			Log.d(TAG, "enabling foreground NFC dispatch");
			// TESTING new ReaderMode:
			// Log.i(TAG, "enableReaderMode without P2P only NFC A");
			// _nfcAdapter
			// .enableReaderMode(
			// this,
			// this,
			// (NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK |
			// NfcAdapter.FLAG_READER_NFC_A),
			// null);
			_nfcAdapter.enableForegroundDispatch(this, _pendingIntent,
					_filters, _techLists);
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (_nfcAdapter != null) {
			Log.d(TAG, "disabling foreground NFC dispatch");
			_nfcAdapter.disableForegroundDispatch(this);
		}
	}

	@Override
	protected void onRestart() {
		super.onRestart();
		showProgressAnimation(false);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_about:
			showAboutDialog(getFragmentManager());
			return true;
		case R.id.action_donate:
			showDonationDialog(getFragmentManager());
			return true;
		case R.id.action_changelog:
			showChangelogDialog(getFragmentManager(), true);
			return true;
		case R.id.action_settings:
			Intent i = new Intent();
			i.setComponent(new ComponentName(getApplicationContext(),
					SettingsActivity.class));
			startActivity(i);
			return true;
		}
		return false;
	}

	@Override
	public void onNewIntent(Intent intent) {
		Log.d(TAG, "onNewIntent()");
		Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
		if (tag != null) {
			handleTag(tag);
		}
	}

	/**
	 * @return <code>true</code> if NFC is available on this device and enabled
	 *         in Adnroid system settings
	 */
	private boolean isNfcAvailable() {
		return (_nfcAdapter != null && _nfcAdapter.isEnabled());
	}

	/**
	 * display changelog dialog (if not seen yet)
	 */
	private void displayWhatsNew() {
		ChangeLog cl = new ChangeLog(this);
		if (cl.firstRun()) {
			showChangelogDialog(getFragmentManager(), false);
		}
	}

	/**
	 * Show or hide the progress animation..
	 * 
	 * @param show
	 */
	private void showProgressAnimation(final boolean show) {
		_viewProgressStatus.setVisibility(show ? View.VISIBLE : View.GONE);

		_viewNfcLogo.setVisibility(show ? View.GONE : View.VISIBLE);
		_viewTextViewShowCard.setVisibility(show ? View.GONE : View.VISIBLE);
	}

	/**
	 * Called whenever we detect a NFC Tag
	 * 
	 * @param intent
	 */
	private void handleTag(Tag tag) {
		showProgressAnimation(true);
		if (_readCardTask != null) {
			return;
		}
		showProgressAnimation(true);
		_readCardTask = new ReadNfcCardTask(tag);
		_readCardTask.execute((Void) null);
	}

	/**
	 * Represents an asynchronous task (reading the card)
	 */
	public class ReadNfcCardTask extends AsyncTask<Void, Void, Boolean> {
		private final static int ERROR_TAG_LOST = -1;
		private final static int ERROR_IO_EX = -2;
		private final static int ERROR_NO_SMARTCARD = -3;
		private Tag nfcTag;
		private int error;

		/**
		 * Constructor
		 * 
		 * @param pNfcTag
		 */
		public ReadNfcCardTask(Tag pNfcTag) {
			super();
			this.nfcTag = pNfcTag;
		}

		@Override
		protected Boolean doInBackground(Void... params) {
			AppController ctl = AppController.getInstance();
			ctl.clearLog();
			try {
				ctl.log(getResources().getString(R.string.app_name)
						+ " version " + getAppVersion(MainActivity.this));
				NfcBankomatCardReader reader = new NfcBankomatCardReader(
						nfcTag, MainActivity.this);
				reader.connectIsoDep();
				// read setting value
				SharedPreferences prefs = PreferenceManager
						.getDefaultSharedPreferences(MainActivity.this);
				_cardReadingResults = reader.readAllCardData(prefs.getBoolean(
						"perform_full_file_scan", false));
				ctl.setCardInfo(_cardReadingResults);
				reader.disconnectIsoDep();
			} catch (NoSmartCardException nsce) {
				Log.w(TAG,
						"Catched NoSmartCardException during reading the card",
						nsce);
				error = ERROR_NO_SMARTCARD;
				return false;
			} catch (TagLostException tle) {
				Log.w(TAG, "Catched TagLostException during reading the card",
						tle);
				error = ERROR_TAG_LOST;
				return false;
			} catch (IOException e) {
				Log.e(TAG, "Catched IOException during reading the card", e);
				error = ERROR_IO_EX;
				ctl.log("-----------------------------------------------");
				ctl.log("ERROR ERROR ERROR:");
				ctl.log("Catched IOException during reading the card:");
				ctl.log(getStacktrace(e));
				ctl.log("-----------------------------------------------");
				return false;
			}
			return true;
		}

		@Override
		protected void onPostExecute(final Boolean success) {
			_readCardTask = null;

			if (success) {
				Log.d(TAG, "reading card finished successfully. isMaestro: "
						+ _cardReadingResults.isMaestroCard() + ", isQuick: "
						+ _cardReadingResults.isQuickCard());
				if (!_cardReadingResults.isMaestroCard()
						&& !_cardReadingResults.isQuickCard()
						&& !_cardReadingResults.isVisaCard()) {
					showProgressAnimation(false);
					displaySimpleAlertDialog(
							MainActivity.this,
							getResources()
									.getString(
											R.string.dialog_title_error_no_maestro_or_quick),
							getResources()
									.getString(
											R.string.dialog_text_error_no_maestro_or_quick));
				} else {
					// show results page
					Intent intent = new Intent(MainActivity.this,
							ResultActivity.class);
					startActivity(intent);
				}
			} else {
				showProgressAnimation(false);
				if (error == ERROR_TAG_LOST) {
					displaySimpleAlertDialog(
							MainActivity.this,
							getResources().getString(
									R.string.dialog_title_error_card_lost),
							getResources().getString(
									R.string.dialog_text_error_card_lost));
				} else if (error == ERROR_NO_SMARTCARD) {
					displaySimpleAlertDialog(
							MainActivity.this,
							getResources().getString(
									R.string.dialog_title_error_no_smartcard),
							getResources().getString(
									R.string.dialog_text_error_no_smartcard));
				}
				// In this case we still open the result Activity for allowing
				// the user to inspect the stacktrace in the Log tab
				else if (error == ERROR_IO_EX) {
					new CustomAlertDialog(MainActivity.this,
							getResources().getString(
									R.string.dialog_title_error_ioexception),
							getResources().getString(
									R.string.dialog_text_error_ioexception)) {

						/**
						 * First show the alert dialog, and when user clicks ok,
						 * show the result
						 */
						@Override
						public void onOkClick() {
							// show results page
							Intent intent = new Intent(MainActivity.this,
									ResultActivity.class);
							startActivity(intent);
						}
					}.show();
				}

				else {
					displaySimpleAlertDialog(
							MainActivity.this,
							getResources().getString(
									R.string.dialog_title_error_unknown),
							getResources().getString(
									R.string.dialog_text_error_unknown));
				}
			}
			// and hide the progress animation
			// showProgressAnimation(false);
		}

		@Override
		protected void onCancelled() {
			_readCardTask = null;
			showProgressAnimation(false);
		}
	}
}
