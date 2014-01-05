package at.zweng.bankomatinfos.ui;

import static at.zweng.bankomatinfos.util.Utils.showAboutDialog;

import java.util.Locale;

import android.app.ActionBar;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ShareActionProvider;
import at.zweng.bankomatinfos.AppController;
import at.zweng.bankomatinfos.R;

/**
 * Activity for displaying the results (hosts fragements in tabs).
 * 
 * @author Johannes Zweng <johannes@zweng.at>
 */
public class ResultActivity extends FragmentActivity implements
		ActionBar.TabListener {

	private Fragment _fragmentResultInfos;
	private Fragment _fragmentResultTxList;
	private Fragment _fragmentResultLog;

	/**
	 * The {@link android.support.v4.view.PagerAdapter} that will provide
	 * fragments for each of the sections. We use a
	 * {@link android.support.v4.app.FragmentPagerAdapter} derivative, which
	 * will keep every loaded fragment in memory. If this becomes too memory
	 * intensive, it may be best to switch to a
	 * {@link android.support.v4.app.FragmentStatePagerAdapter}.
	 */
	private SectionsPagerAdapter _sectionsPagerAdapter;

	/**
	 * The {@link ViewPager} that will host the section contents.
	 */
	private ViewPager _viewPager;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_result);

		_fragmentResultInfos = new ResultInfosFragment();
		_fragmentResultTxList = new ResultTxListFragment();
		_fragmentResultLog = new ResultLogFragment();

		// Set up the action bar.
		final ActionBar actionBar = getActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

		// Create the adapter that will return a fragment for each of the
		// primary sections of the app.
		_sectionsPagerAdapter = new SectionsPagerAdapter(
				getSupportFragmentManager());

		// Set up the ViewPager with the sections adapter.
		_viewPager = (ViewPager) findViewById(R.id.pager);
		_viewPager.setAdapter(_sectionsPagerAdapter);

		// When swiping between different sections, select the corresponding
		// tab. We can also use ActionBar.Tab#select() to do this if we have
		// a reference to the Tab.
		_viewPager
				.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
					@Override
					public void onPageSelected(int position) {
						actionBar.setSelectedNavigationItem(position);
					}
				});

		// For each of the sections in the app, add a tab to the action bar.
		for (int i = 0; i < _sectionsPagerAdapter.getCount(); i++) {
			// Create a tab with text corresponding to the page title defined by
			// the adapter. Also specify this Activity object, which implements
			// the TabListener interface, as the callback (listener) for when
			// this tab is selected.
			actionBar.addTab(actionBar.newTab()
					.setText(_sectionsPagerAdapter.getPageTitle(i))
					.setTabListener(this));
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.result, menu);
		// Locate MenuItem with ShareActionProvider
		MenuItem item = menu.findItem(R.id.action_share);
		// Fetch and store ShareActionProvider
		ShareActionProvider shareActionProvider = (ShareActionProvider) item
				.getActionProvider();

		// set the log content as share content
		Intent shareIntent = new Intent();
		shareIntent.setAction(Intent.ACTION_SEND);
		shareIntent.putExtra(android.content.Intent.EXTRA_SUBJECT,
				getResources().getString(R.string.action_share_subject));
		shareIntent.putExtra(Intent.EXTRA_TEXT, AppController.getInstance()
				.getLog());
		shareIntent.setType("text/plain");
		shareActionProvider.setShareIntent(shareIntent);
		return true;
	}

	/**
	 * Called whenever we call invalidateOptionsMenu()
	 */
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		// show share action only on Tab 2 (Log)
		// (tab index starts with 0)
		if (_viewPager.getCurrentItem() == 2) {
			menu.findItem(R.id.action_share).setVisible(true);
		} else {
			menu.findItem(R.id.action_share).setVisible(false);
		}
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_about:
			showAboutDialog(getFragmentManager());
			return true;
		}
		return false;
	}

	@Override
	public void onTabSelected(ActionBar.Tab tab,
			FragmentTransaction fragmentTransaction) {
		// When the given tab is selected, switch to the corresponding page in
		// the ViewPager.
		_viewPager.setCurrentItem(tab.getPosition());
		invalidateOptionsMenu(); // creates call to
		// onPrepareOptionsMenu()
	}

	@Override
	public void onTabUnselected(ActionBar.Tab tab,
			FragmentTransaction fragmentTransaction) {
	}

	@Override
	public void onTabReselected(ActionBar.Tab tab,
			FragmentTransaction fragmentTransaction) {
	}

	/**
	 * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
	 * one of the sections/tabs/pages.
	 */
	public class SectionsPagerAdapter extends FragmentPagerAdapter {

		public SectionsPagerAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public Fragment getItem(int position) {
			if (position == 0) {
				return _fragmentResultInfos;
			} else if (position == 1) {
				return _fragmentResultTxList;
			} else {
				return _fragmentResultLog;
			}
		}

		@Override
		public int getCount() {
			// Show 3 total pages.
			return 3;
		}

		@Override
		public CharSequence getPageTitle(int position) {
			Locale locale = Locale.getDefault();
			switch (position) {
			case 0:
				return getString(R.string.title_section1).toUpperCase(locale);
			case 1:
				return getString(R.string.title_section2).toUpperCase(locale);
			case 2:
				return getString(R.string.title_section3).toUpperCase(locale);
			}
			return null;
		}
	}

}
