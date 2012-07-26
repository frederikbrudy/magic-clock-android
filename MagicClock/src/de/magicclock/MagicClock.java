/**
 * MagicClock, a clock that displays people's locations
 * Copyright (C) 2012 www.magicclock.de
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * You can contact the authors via authors@magicclock.de or www.magicclock.de
 */
package de.magicclock;

import java.security.acl.NotOwnerException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import de.magicclock.R;
import de.magicclock.helper.Constants;
import de.magicclock.helper.FakeLocationItem;
import de.magicclock.helper.RequestTask;
import de.magicclock.helper.httpResponseReceiver;

public class MagicClock extends Activity implements OnClickListener, OnCheckedChangeListener, httpResponseReceiver {
	private String TAG;
	//	private LocationManager mLocationManager;
	private ConnectivityManager mConnManager;
	//	private LocationReceiver mLocationReceiver;
	private SharedPreferences mPreferences;
	private Button mRegister;
	private Button mStartMonitoring;
	private Button mStopMonitoring;
	private boolean sensorRunning;
	private TextView mLocationViewLat;
	private ToggleButton mMonitoringToggle;
	private TextView mLocationViewLong;
	private CharSequence mLocationViewLatString;
	private CharSequence mLocationViewLongString;
	private NotificationManager mNotificationManager;
	private int mNotificationIcon;
	private String mNotificationTickerText;
	private String mNotificationContentTitle;
	private String mNotificationContentText;
	private long mNotificationWhen;

	public static final int NOTIFICATION_ID_LOCATION = 1; 


	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		log("on create called");

		TAG = this.getClass().toString();
		setContentView(R.layout.main);

		mPreferences = PreferenceManager.getDefaultSharedPreferences(this);


		mConnManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);

		mRegister = (Button) findViewById(R.id.register);
		mRegister.setOnClickListener(this);

		mMonitoringToggle = (ToggleButton) findViewById(R.id.monitoring_toggle);
		if(isMyServiceRunning()){
			mMonitoringToggle.setChecked(true);
		}
		mMonitoringToggle.setOnCheckedChangeListener(this);

		mRegister.setOnClickListener(this);

		mLocationViewLat = (TextView) findViewById(R.id.current_location_lat);
		mLocationViewLatString = mLocationViewLat.getText();
		mLocationViewLong = (TextView) findViewById(R.id.current_location_long);
		mLocationViewLongString = mLocationViewLong.getText();

		if(Constants.DEBUG){

		}

		mNotificationManager = (NotificationManager) getSystemService( Context.NOTIFICATION_SERVICE);
		mNotificationIcon = R.drawable.icon57;
		mNotificationTickerText = getString(R.string.notification_ticker_text);

		mNotificationContentTitle = getString(R.string.notification_content_title);
		mNotificationContentText = getString(R.string.notification_content_text);

	}

	private boolean isMyServiceRunning() {
		ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
		for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)){
			if ("de.magicclock.LocationService".equals(service.service.getClassName())) {
				return true;
			}
		}
		return false;
	}

	private void log(String msg){
		Log.d(this.TAG, msg);
	}

	private String getSharedPrefsString(String key, String defValue) {
		return mPreferences.getString(key, defValue);
	}

	private boolean firstTime() {

		String first = getSharedPrefsString("first", null);
		return (first == null);
	}

	private void storeSharedPrefsString(String key, String value) {
		SharedPreferences.Editor editor = mPreferences.edit();

		editor.putString(key, value);
		editor.commit();
	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.optionsmenu, menu);
		return true;
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		if(item.getItemId() == R.id.fake){
			
			final FakeLocationItem[] items2 = {
					new FakeLocationItem("home", "Home"), //a third argument with a drawable is accepted. 
					new FakeLocationItem("uni", "Uni"),
					new FakeLocationItem("sport", "Sport"),
					new FakeLocationItem("unterwegs", "Unterwegs"),
					new FakeLocationItem("prison", "Gefängnis"),
					new FakeLocationItem("feiern", "Feiern"),
					new FakeLocationItem("essen", "Essen"),
					new FakeLocationItem("chillen", "Relaxen"),
					new FakeLocationItem("aufreisen", "Auf Reisen"),
					new FakeLocationItem("krankenhaus", "Krankenhaus"),
					new FakeLocationItem("work", "Arbeit"),
			};

			ListAdapter adapter = new ArrayAdapter<FakeLocationItem>(this, android.R.layout.select_dialog_item, android.R.id.text1, items2){
				public View getView(int position, View convertView, ViewGroup parent) {
					//User super class to create the View
					View v = super.getView(position, convertView, parent);
					TextView tv = (TextView)v.findViewById(android.R.id.text1);

					//Put the image on the TextView
					tv.setCompoundDrawablesWithIntrinsicBounds(items2[position].icon, 0, 0, 0); 

					//Add margin between image and text (support various screen densities)
					int dp5 = (int) (5 * getResources().getDisplayMetrics().density + 0.5f);
					tv.setCompoundDrawablePadding(dp5);

					return v;
				}
			};
			
			AlertDialog.Builder builder2 = new AlertDialog.Builder(this);
			builder2.setTitle("Choose a location");
			builder2.setAdapter(adapter, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int item) {
					Toast.makeText(getApplicationContext(), "Fake location sent: " + items2[item].text, Toast.LENGTH_SHORT).show();
					saveFakeLocation((String) items2[item].value);
					mMonitoringToggle.setChecked(false);
				}
		    });
			AlertDialog alert2 = builder2.create();
			alert2.show();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	public void saveFakeLocation(String fakeLocation){
		storeSharedPrefsString("fakeLocation", fakeLocation);
		storeSharedPrefsString("currentLatitude", null);
		storeSharedPrefsString("currentLongitude", null);
		log("sending fake location: " + fakeLocation);
		String server1 = getString(R.string.server_api_url_fake);
		server1 = server1.replace(getString(R.string.server_api_url_server), getString(R.string.server_api));
		server1 = server1.replace(getString(R.string.server_api_url_userid), getSharedPrefsString("id", null));
		server1 = server1.replace(getString(R.string.server_api_url_loc), fakeLocation);
		AsyncTask<String,String,String> task = new RequestTask().execute(server1);
		((RequestTask) task).setSuccessListener(this);
		showNotification(true);
		log(server1);
		log("fake location sent");
	}

	@Override
	public void httpRequestResponse(String response) {
		log("httpRequestResponse called");
		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		Date date = new Date();
		mNotificationContentText = getString(R.string.notification_fake) + dateFormat.format(date);
		showNotification(false);
	}

	private void showNotification(boolean updateTime){

		if(updateTime){
			mNotificationWhen = System.currentTimeMillis();
		}

		Notification notification = new Notification(mNotificationIcon, mNotificationContentTitle, mNotificationWhen);
		Context context = getApplicationContext();
		Intent notificationIntent = new Intent(this, MagicClock.class);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

		notification.setLatestEventInfo(context, mNotificationContentTitle, mNotificationContentText, contentIntent);
		notification.flags |= Notification.FLAG_AUTO_CANCEL;
		mNotificationManager.notify(MagicClock.NOTIFICATION_ID_LOCATION, notification);

	}

	@Override
	public void onBackPressed(){
		super.onBackPressed();

	}

	@Override
	public void onCheckedChanged(CompoundButton arg0, boolean isChecked) {
		if(isChecked){
			Intent myIntent = new Intent(getApplicationContext(), LocationService.class);
			myIntent.putExtra("de.magicclock.locService", "start");
			startService(myIntent);
		}
		else{
			Intent myIntent = new Intent(getApplicationContext(), LocationService.class);
			myIntent.putExtra("de.magicclock.locService", "stop");
			stopService(myIntent);
		}
	}

	@Override
	public void onClick(View v) {
		if(v.getId() == R.id.register){

			final EditText user_id = (EditText) findViewById(R.id.user_id);

			AlertDialog.Builder alertbox = new AlertDialog.Builder(this);
			alertbox.setTitle(R.string.save_id_title);
			alertbox.setMessage(R.string.save_id_text);
			alertbox.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
					return;
				}
			});
			alertbox.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					storeSharedPrefsString("id", user_id.getText().toString());
					dialog.dismiss();
					return;
				}
			});

			alertbox.create().show();
		}

	} 


	@Override
	protected void onResume() {
		super.onResume();
		mLocationViewLat.setText(mLocationViewLatString + " " + getSharedPrefsString("currentLatitude", null));
		mLocationViewLong.setText(mLocationViewLongString + " " + getSharedPrefsString("currentLongitude", null));
		String userId = getSharedPrefsString("id", null);
		if(userId != null){
			EditText text = (EditText) findViewById(R.id.user_id);
			text.setEnabled(false);
			text.setText(userId);
			mRegister.setEnabled(false);
		}
	}

	@Override
	protected void onPause() {
		sensorRunning = false;
		super.onPause();		
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}


	private int checkConnectivity(){
		if(!mConnManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).isConnected() 
				&& !mConnManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).isConnected()){
			//Warnung anzeigen, dass Internet ausgeschaltet
			AlertDialog.Builder alertbox = new AlertDialog.Builder(this);
			alertbox.setTitle(R.string.internet_warning_title);
			alertbox.setMessage(R.string.internet_warning);
			alertbox.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
					return;
				}
			});
			alertbox.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					Intent i = new Intent(Settings.ACTION_WIFI_SETTINGS);
					startActivity(i);
					dialog.dismiss();
					return;
				}
			});
			alertbox.create().show();
			return 2;
		}

		return 0;
	}

}