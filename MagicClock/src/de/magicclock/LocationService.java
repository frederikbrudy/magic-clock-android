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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;
import de.magicclock.R;
import de.magicclock.helper.LocationReceiver;
import de.magicclock.helper.LocationUpdateReceiver;
import de.magicclock.helper.RequestTask;
import de.magicclock.helper.httpResponseReceiver;

public class LocationService extends Service implements LocationUpdateReceiver, httpResponseReceiver {
	

	private NotificationManager mNotificationManager;
	private int mNotificationIcon;
	private CharSequence mNotificationTickerText;
	private long mNotificationWhen;
	private CharSequence mNotificationContentTitle;
	private CharSequence mNotificationContentText;
	private LocationReceiver mLocationReceiver;
	private LocationManager mLocationManager;
	private boolean sensorRunning;
	private String TAG;
	private SharedPreferences mPreferences;
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		TAG = this.getClass().toString();
		mPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		
		mNotificationManager = (NotificationManager) getSystemService( Context.NOTIFICATION_SERVICE);
		mNotificationIcon = R.drawable.icon57;
		mNotificationTickerText = getString(R.string.notification_ticker_text);

		mNotificationContentTitle = getString(R.string.notification_content_title);
		mNotificationContentText = getString(R.string.notification_content_text);
	}

	@Override
	public void onDestroy() {
		if(sensorRunning){
			unregisterSensorListener();
		}
		mNotificationManager.cancel(MagicClock.NOTIFICATION_ID_LOCATION);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startid) {
		mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		mLocationReceiver  = new LocationReceiver(mLocationManager);
		mLocationReceiver.addLocationUpdateReceiver(this);
		
		checkLocationManager();
		
		registerSensorListener();
		showNotification(true);
		return START_STICKY;
	}
	
	private void checkLocationManager(){
		if(!mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) && !mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
			//show warning if location is disabled
			AlertDialog.Builder alertbox = new AlertDialog.Builder(this);
			alertbox.setTitle(R.string.location_warning_title);
			alertbox.setMessage(R.string.location_warning);
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
					Intent i = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
					startActivity(i);
					dialog.dismiss();
					return;
				}
			});
			alertbox.create().show();
		}
	}

	private void showNotification(boolean updateTime){
		
		if(updateTime){
			mNotificationWhen = System.currentTimeMillis();
		}

		Notification notification = new Notification(mNotificationIcon, mNotificationTickerText, mNotificationWhen);

		Context context = getApplicationContext();
		Intent notificationIntent = new Intent(this, MagicClock.class);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

		notification.setLatestEventInfo(context, mNotificationContentTitle, mNotificationContentText, contentIntent);
		notification.flags |= Notification.FLAG_ONGOING_EVENT;
		notification.flags |= Notification.FLAG_NO_CLEAR;
		mNotificationManager.notify(MagicClock.NOTIFICATION_ID_LOCATION, notification);

	}

	@Override
	public void notifyLocation(LocationReceiver locRec){
		storeSharedPrefsString("fakeLocation", null);
		storeSharedPrefsString("currentLatitude", ""+mLocationReceiver.getCurrentLocation().getLatitude());
		storeSharedPrefsString("currentLongitude", ""+mLocationReceiver.getCurrentLocation().getLongitude());
		log("sending request");
		String server1 = getString(R.string.server_api_url);
		server1 = server1.replace(getString(R.string.server_api_url_server), getString(R.string.server_api));
		server1 = server1.replace(getString(R.string.server_api_url_userid), getSharedPrefsString("id", null));
		server1 = server1.replace(getString(R.string.server_api_url_lon), ""+mLocationReceiver.getCurrentLocation().getLongitude());
		server1 = server1.replace(getString(R.string.server_api_url_lat), ""+mLocationReceiver.getCurrentLocation().getLatitude());
		log(server1);
		AsyncTask<String,String,String> task = new RequestTask().execute(server1);
		((RequestTask) task).setSuccessListener(this);
		log("request sent");
	}
	

	@Override
	public void httpRequestResponse(String response) {
		//uhrzeit der notification updaten?
		//text der notification updaten
		log("httpRequestResponse called");
		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		Date date = new Date();
		mNotificationContentText = getString(R.string.notification_last_updated) + dateFormat.format(date);
		showNotification(false);
	}
	
	/**
	 * Registers the needed SensorListener for this activity.
	 */
	private void registerSensorListener(){
		mLocationReceiver.startListeningLocations();
		sensorRunning = true;
	}

	/**
	 * Unregisters all Sensor Listener for this activity.
	 */
	private void unregisterSensorListener(){
		mLocationReceiver.stopListeningLocations();
		Log.d(TAG, "Sensor Listener unregistered");
	}
	
	private String getSharedPrefsString(String key, String defValue) {
		return mPreferences.getString(key, defValue);
	}
	
	private void storeSharedPrefsString(String key, String value) {
		SharedPreferences.Editor editor = mPreferences.edit();
		
		editor.putString(key, value);
		editor.commit();
	}
	
	private void log(String msg){
		Log.d(this.TAG, msg);
	}

	
}
