package at.zweng.bankomatinfos.ui;

import static android.nfc.NfcAdapter.FLAG_READER_NFC_A;
import static android.nfc.NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK;
import static at.zweng.bankomatinfos.iso7816emv.EmvUtils.APPLICATION_ID_EMV_MAESTRO_BANKOMAT;
import static at.zweng.bankomatinfos.util.Utils.TAG;
import static at.zweng.bankomatinfos.util.Utils.displaySimpleAlertDialog;
import static at.zweng.bankomatinfos.util.Utils.getAppVersion;
import static at.zweng.bankomatinfos.util.Utils.getStacktrace;
import static at.zweng.bankomatinfos.util.Utils.showAboutDialog;
import static at.zweng.bankomatinfos.util.Utils.showChangelogDialog;

import java.io.IOException;

import org.simalliance.openmobileapi.Channel;
import org.simalliance.openmobileapi.Reader;
import org.simalliance.openmobileapi.SEService;
import org.simalliance.openmobileapi.Session;

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
import android.widget.Button;
import at.zweng.bankomatinfos.AppController;
import at.zweng.bankomatinfos.exceptions.NoSmartCardException;
import at.zweng.bankomatinfos.iso7816emv.ITag;
import at.zweng.bankomatinfos.iso7816emv.IsoDepTag;
import at.zweng.bankomatinfos.iso7816emv.NfcBankomatCardReader;
import at.zweng.bankomatinfos.iso7816emv.OmapiSessionTag;
import at.zweng.bankomatinfos.model.CardInfo;
import at.zweng.bankomatinfos.util.ChangeLog;
import at.zweng.bankomatinfos.util.CustomAlertDialog;
import at.zweng.bankomatinfos2.R;

/**
 * Startup activity
 * 
 * @author Johannes Zweng <johannes@zweng.at>
 * 
 */
public class MainActivity extends Activity implements NfcAdapter.ReaderCallback {

	// for NFC stuff
	private PendingIntent _pendingIntent;
	private IntentFilter[] _filters;
	private String[][] _techLists;
	private NfcAdapter _nfcAdapter;

	// openmobile API
	private SEService _seService;

	// View elements
	private View _viewNfcLogo;
	private View _viewTextViewShowCard;
	private View _viewProgressStatus;
	private Button _btnUseOmapi;
	private boolean _hasOmapi = false;

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
		_btnUseOmapi = (Button) findViewById(R.id.btnUseOmapi);
		_btnUseOmapi.setVisibility(View.GONE);
		_btnUseOmapi.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				openOmapiSession();
			}
		});

		// NFC stuff
		_pendingIntent = PendingIntent.getActivity(this, 0,
				new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
		_filters = new IntentFilter[] { new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED) };
		_techLists = new String[][] { { "android.nfc.tech.NfcA" } };
		_nfcAdapter = NfcAdapter.getDefaultAdapter(this);
		// create last changes dialog if needed
		displayWhatsNew();
		checkForOmapi();
	}

	/**
	 * Check if we have OMAPI available and are allowed to select the maestro
	 * AID
	 */
	private void checkForOmapi() {
		// check if we have OpenMobile API in classpath:
		try {
			Class.forName("org.simalliance.openmobileapi.SEService");
		} catch (ClassNotFoundException e1) {
			Log.d(TAG, "Couldn't find class 'org.simalliance.openmobileapi.SEService'. "
					+ "This devices seems not to have OpenMobile API installed.");
			_hasOmapi = false;
			return;
		}
		try {
			// check if we can connect to it:
			_seService = new SEService(this, new SEService.CallBack() {
				@Override
				public void serviceConnected(SEService seService) {
					checkForMaestroAid(seService);
				}
			});
		} catch (Exception e) {
			Log.w(TAG, "Exception while trying to create OMAPI SEService. Will not use OMAPI.", e);
		}
	}

	/**
	 * Connect to first reader over OMAPI
	 */
	private void openOmapiSession() {
		Log.d(TAG, "start OMAPI session..");
		if (_seService == null || !_seService.isConnected()) {
			Log.d(TAG, "OMAPI service not connected...");
			return;
		}
		Reader simReader = getFirstSimReader(_seService.getReaders());
		if (simReader == null) {
			Log.d(TAG, "OMAPI no sim reader found...");
			return;
		}
		Session sess;
		try {
			sess = simReader.openSession();
			handleOmapiTag(sess);
		} catch (Exception e) {
			Log.w(TAG, "Exception while opening OMAPI session", e);
			return;
		}
	}

	/**
	 * Tries to find the first SIM card reader
	 * 
	 * @return first reader that starts with "SIM" or null in every other case
	 */
	private Reader getFirstSimReader(Reader[] readers) {
		if (readers == null) {
			return null;
		}
		for (Reader r : readers) {
			if (r.getName().startsWith("SIM")) {
				return r;
			}
		}
		return null;
	}

	/**
	 * Check if we have a MAESTRO card accessible via OpenMobileAPI, and only
	 * display the button if its accessible
	 * 
	 * @param se
	 */
	private void checkForMaestroAid(SEService se) {
		if (se == null || !se.isConnected()) {
			return;
		}
		Reader simReader = getFirstSimReader(_seService.getReaders());
		if (simReader == null) {
			return;
		}
		Session sess;
		try {
			sess = simReader.openSession();
		} catch (Exception e) {
			Log.w(TAG, "Exception while checking for OMAPI, ignore it and dont use OMAPI", e);
			return;
		}
		try {
			Channel chan = sess.openLogicalChannel(APPLICATION_ID_EMV_MAESTRO_BANKOMAT);
			_hasOmapi = true;
			_btnUseOmapi.setVisibility(View.VISIBLE);
			chan.close();
			sess.close();
		} catch (Exception e) {
			sess.close();
			Log.w(TAG, "Exception while checking for OMAPI, ignore it and dont use OMAPI", e);
			return;
		}
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
		// use the reader mode, this will completly disable Peer-2-peer mode
		// (Android Beam) so we can also scan anotther phone which runs
		// cardemulation
		if (_nfcAdapter != null) {
			Log.i(TAG, "enableReaderMode without P2P only NFC A");
			_nfcAdapter.enableReaderMode(this, this, (FLAG_READER_SKIP_NDEF_CHECK | FLAG_READER_NFC_A), null);
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (_nfcAdapter != null) {
			Log.d(TAG, "disable reader mode");
			_nfcAdapter.disableReaderMode(this);
		}
		if (_seService != null && _seService.isConnected()) {
			_seService.shutdown();
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
		case R.id.action_changelog:
			showChangelogDialog(getFragmentManager(), true);
			return true;
		case R.id.action_settings:
			Intent i = new Intent();
			i.setComponent(new ComponentName(getApplicationContext(), SettingsActivity.class));
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
			handleNfcTag(tag);
		}
	}

	/**
	 * Callback when using reader mode.
	 */
	@Override
	public void onTagDiscovered(Tag tag) {
		if (tag != null) {
			handleNfcTag(tag);
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
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Log.d(TAG, "showProgressAnimation: " + show);
				_viewProgressStatus.setVisibility(show ? View.VISIBLE : View.GONE);
				_viewNfcLogo.setVisibility(show ? View.GONE : View.VISIBLE);
				_viewTextViewShowCard.setVisibility(show ? View.GONE : View.VISIBLE);
				if (show) {
					_btnUseOmapi.setVisibility(View.GONE);
				} else {
					if (_hasOmapi) {
						_btnUseOmapi.setVisibility(View.VISIBLE);
					}
				}
			}
		});
	}

	/**
	 * Called whenever we detect a NFC Tag
	 * 
	 * @param intent
	 */
	private void handleNfcTag(Tag tag) {
		if (_readCardTask != null) {
			return;
		}
		showProgressAnimation(true);
		_readCardTask = new ReadNfcCardTask(new IsoDepTag(tag));
		_readCardTask.execute((Void) null);
	}

	/**
	 * Called whenever we start with omapi tag (session)
	 * 
	 * @param intent
	 */
	private void handleOmapiTag(Session session) {
		if (_readCardTask != null) {
			return;
		}
		showProgressAnimation(true);
		_readCardTask = new ReadNfcCardTask(new OmapiSessionTag(session));
		_readCardTask.execute((Void) null);
	}

	/**
	 * Represents an asynchronous task (reading the card)
	 */
	public class ReadNfcCardTask extends AsyncTask<Void, Void, Boolean> {
		private final static int ERROR_TAG_LOST = -1;
		private final static int ERROR_IO_EX = -2;
		private final static int ERROR_NO_SMARTCARD = -3;
		private ITag nfcTag;
		private int error;

		/**
		 * Constructor
		 * 
		 * @param pNfcTag
		 */
		public ReadNfcCardTask(ITag pNfcTag) {
			super();
			this.nfcTag = pNfcTag;
		}

		@Override
		protected Boolean doInBackground(Void... params) {
			AppController ctl = AppController.getInstance();
			ctl.clearLog();
			try {
				ctl.log(getResources().getString(R.string.app_name) + " version " + getAppVersion(MainActivity.this));
				nfcTag.connectIsoDep();
				NfcBankomatCardReader reader = new NfcBankomatCardReader(nfcTag, MainActivity.this);
				reader.connect();
				// read setting value
				SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
				_cardReadingResults = reader.readAllCardData(prefs.getBoolean("perform_full_file_scan", false));
				ctl.setCardInfo(_cardReadingResults);
				reader.disconnect();
			} catch (NoSmartCardException nsce) {
				Log.w(TAG, "Catched NoSmartCardException during reading the card", nsce);
				error = ERROR_NO_SMARTCARD;
				return false;
			} catch (TagLostException tle) {
				Log.w(TAG, "Catched TagLostException during reading the card", tle);
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
				Log.d(TAG, "reading card finished successfully");
				if (!_cardReadingResults.isSupportedCard()) {
					showProgressAnimation(false);
					displaySimpleAlertDialog(MainActivity.this,
							getResources().getString(R.string.dialog_title_error_unsupported_card),
							getResources().getString(R.string.dialog_text_error_unsupported_card));
				} else {
					// show results page
					Intent intent = new Intent(MainActivity.this, ResultActivity.class);
					startActivity(intent);
				}
			} else {
				showProgressAnimation(false);
				if (error == ERROR_TAG_LOST) {
					displaySimpleAlertDialog(MainActivity.this,
							getResources().getString(R.string.dialog_title_error_card_lost),
							getResources().getString(R.string.dialog_text_error_card_lost));
				} else if (error == ERROR_NO_SMARTCARD) {
					displaySimpleAlertDialog(MainActivity.this,
							getResources().getString(R.string.dialog_title_error_no_smartcard),
							getResources().getString(R.string.dialog_text_error_no_smartcard));
				}
				// In this case we still open the result Activity for allowing
				// the user to inspect the stacktrace in the Log tab
				else if (error == ERROR_IO_EX) {
					new CustomAlertDialog(MainActivity.this,
							getResources().getString(R.string.dialog_title_error_ioexception),
							getResources().getString(R.string.dialog_text_error_ioexception)) {

						/**
						 * First show the alert dialog, and when user clicks ok,
						 * show the result
						 */
						@Override
						public void onOkClick() {
							// show results page
							Intent intent = new Intent(MainActivity.this, ResultActivity.class);
							startActivity(intent);
						}
					}.show();
				}

				else {
					displaySimpleAlertDialog(MainActivity.this,
							getResources().getString(R.string.dialog_title_error_unknown),
							getResources().getString(R.string.dialog_text_error_unknown));
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
