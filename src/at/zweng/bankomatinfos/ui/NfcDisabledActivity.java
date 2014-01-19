package at.zweng.bankomatinfos.ui;

import static at.zweng.bankomatinfos.util.Utils.showAboutDialog;
import static at.zweng.bankomatinfos.util.Utils.showChangelogDialog;
import static at.zweng.bankomatinfos.util.Utils.showDonationDialog;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import at.zweng.bankomatinfos.R;

/**
 * Very simple activity, simply displays a no nfc logo (we show this if NFC is
 * not available)
 * 
 * @author Johannes Zweng <johannes@zweng.at>
 */
public class NfcDisabledActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_nfc_disabled);
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

}
