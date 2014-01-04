package at.zweng.bankomatinfos.ui;

import static at.zweng.bankomatinfos.util.Utils.*;

import java.io.IOException;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.TagLostException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import at.zweng.bankomatinfos.AppController;
import at.zweng.bankomatinfos.R;
import at.zweng.bankomatinfos.exceptions.NoSmartCardException;
import at.zweng.bankomatinfos.iso7816emv.NfcBankomatCardReader;
import at.zweng.bankomatinfos.model.CardInfo;

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
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onResume()
	 */
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
			_nfcAdapter.enableForegroundDispatch(this, _pendingIntent,
					_filters, _techLists);
		}
		Intent intent = getIntent();
		if (intent != null
				&& NfcAdapter.ACTION_TECH_DISCOVERED.equals(intent.getAction())) {
			handleTag(intent);
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
	public void onNewIntent(Intent intent) {
		Log.d(TAG, "onNewIntent()");
		handleTag(intent);
	}

	/**
	 * @return <code>true</code> if NFC is available on this device and enabled
	 *         in Adnroid system settings
	 */
	private boolean isNfcAvailable() {
		return (_nfcAdapter != null && _nfcAdapter.isEnabled());
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
	private void handleTag(Intent intent) {
		showProgressAnimation(true);
		if (_readCardTask != null) {
			return;
		}
		Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
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
			try {
				NfcBankomatCardReader reader = new NfcBankomatCardReader(nfcTag);
				reader.connectIsoDep();
				_cardReadingResults = reader.readAllCardData();
				AppController.getInstance().setCardInfo(_cardReadingResults);
				reader.disconnectIsoDep();
			} catch (NoSmartCardException nsce) {
				error = ERROR_NO_SMARTCARD;
				return false;
			} catch (TagLostException tle) {
				error = ERROR_TAG_LOST;
				return false;
			} catch (IOException e) {
				error = ERROR_IO_EX;
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
						&& !_cardReadingResults.isQuickCard()) {
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
				} else if (error == ERROR_IO_EX) {
					displaySimpleAlertDialog(
							MainActivity.this,
							getResources().getString(
									R.string.dialog_title_error_ioexception),
							getResources().getString(
									R.string.dialog_text_error_ioexception));
				} else if (error == ERROR_NO_SMARTCARD) {
					displaySimpleAlertDialog(
							MainActivity.this,
							getResources().getString(
									R.string.dialog_title_error_no_smartcard),
							getResources().getString(
									R.string.dialog_text_error_no_smartcard));
				} else {
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
