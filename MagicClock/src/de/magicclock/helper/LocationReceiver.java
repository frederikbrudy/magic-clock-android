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
package de.magicclock.helper;


import java.util.ArrayList;

import de.magicclock.LocationService;
import de.magicclock.MagicClock;

import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

public class LocationReceiver implements LocationListener {
	/** The Tag, describing this class. */
	private String TAG;
	private Location mCurrentBestLocation = null;
	private LocationManager mLocationManager;
	private boolean mIsRegistered = false;
	private Criteria mProviderCriteria;
	private float mDirection = 0;
	private ArrayList<LocationUpdateReceiver> mLocationUpdateReceiver;


	
	private static final int ONE_MINUTE = 1000 * 60;
	/** Variable indicating 2 Minutes in milliseconds. */
	private static final int TWO_MINUTES = ONE_MINUTE * 2;
	private static final int HALF_HOUR = ONE_MINUTE * 30;
	private static final int HOUR = ONE_MINUTE * 60;
	
	public LocationReceiver(LocationManager locationManager){
		this.TAG = this.getClass().toString();
		Log.d(TAG, "LocationReceiver called");
		mLocationManager = locationManager;
		
		mLocationUpdateReceiver = new ArrayList<LocationUpdateReceiver>();
		
		
		mProviderCriteria = new Criteria();
		mProviderCriteria.setAccuracy(Criteria.ACCURACY_COARSE);
		mProviderCriteria.setAccuracy(Criteria.ACCURACY_FINE);
		
		
		//get historical locations
		Location l = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
		if(isBetterLocation(l, null)){
			mCurrentBestLocation = l;
		}
		l = mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
		if(isBetterLocation(l, mCurrentBestLocation)){
			mCurrentBestLocation = l;
		}
	}
	
	public void startListeningLocations(){
		if(!mIsRegistered){
			mLocationManager.requestLocationUpdates(mLocationManager.getBestProvider(mProviderCriteria, true), HALF_HOUR, 0, this);
			mIsRegistered = true;
		}
	}
	
	public void stopListeningLocations(){
		if(mIsRegistered){
			mLocationManager.removeUpdates(this);
			mIsRegistered = false;
		}
	}

	@Override
	public void onLocationChanged(Location l) {
		if(isBetterLocation(l, mCurrentBestLocation)){
			mCurrentBestLocation = l;
			notifyLocationUpdateReceiver();
		}
	}
	
	private void notifyLocationUpdateReceiver() {
		for (int i = 0; i < mLocationUpdateReceiver.size(); i++) {
			mLocationUpdateReceiver.get(i).notifyLocation(this);
		}
	}

	public void addLocationUpdateReceiver(LocationService locationService){
		if(!mLocationUpdateReceiver.contains(locationService)){
			mLocationUpdateReceiver.add(locationService);
		}
	}
	
	public void rempveLocationUpdateReceiver(LocationService locationService){
		if(!mLocationUpdateReceiver.contains(locationService)){
			mLocationUpdateReceiver.remove(locationService);
		}
	}

	@Override
	public void onProviderDisabled(String arg0) {
		
	}

	@Override
	public void onProviderEnabled(String arg0) {
		
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		
	}
	
	/** 
	 * Determines whether one Location reading is better than the current Location fix.
	 * 
	 * @param location  The new Location that you want to evaluate
	 * @param currentBestLocation  The current Location fix, to which you want to compare the new one
	 * @see http://developer.android.com/guide/topics/location/obtaining-user-location.html
	 */
	protected boolean isBetterLocation(Location location, Location currentBestLocation) {
		if (currentBestLocation == null) {
			// A new location is always better than no location
			return true;
		}

		// Check whether the new location fix is newer or older
		long timeDelta = location.getTime() - currentBestLocation.getTime();
		boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
		boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;
		boolean isNewer = timeDelta > 0;

		// If it's been more than two minutes since the current location, use the new location
		// because the user has likely moved
		if (isSignificantlyNewer) {
			return true;
			// If the new location is more than two minutes older, it must be worse
		} else if (isSignificantlyOlder) {
			return false;
		}

		// Check whether the new location fix is more or less accurate
		int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
		boolean isLessAccurate = accuracyDelta > 0;
		boolean isMoreAccurate = accuracyDelta < 0;
		boolean isSignificantlyLessAccurate = accuracyDelta > 200;

		// Check if the old and new location are from the same provider
		boolean isFromSameProvider = isSameProvider(location.getProvider(),
				currentBestLocation.getProvider());
//		Log.d(TAG, "location provider: " + location.getProvider());
		
		// Determine location quality using a combination of timeliness and accuracy
		if (isMoreAccurate) {
			return true;
		} else if (isNewer && !isLessAccurate) {
			return true;
		} else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
			return true;
		}
		return false;
	}



	/**
	 * Checks whether two providers are the same
	 * 
	 * @param provider1 first provider or <code>null</code>
	 * @param provider2 second provider or <code>null</code>
	 * @return <code>true</code> if the two providers are the same as of the <code>equals</code> method,
	 * <code>false</code> otherwise
	 */
	private boolean isSameProvider(String provider1, String provider2) {
		if (provider1 == null) {
			return provider2 == null;
		}
		return provider1.equals(provider2);
	}
	
	/**
	 * Returns the direction the device is currently pointing.
	 * 
	 * @return
	 */
	public float getDirection(int i){
		return mDirection;
	}

	
	
	/**
	 * Returns the current best location. Locations may improve over time
	 * and may be more accurate between two calls of this function.
	 * Accuracy only improves and never deteriorates.
	 * 
	 * @return
	 */
	public Location getCurrentLocation(){
		if(mCurrentBestLocation == null){
			Location l = new Location("network");
			l.setLatitude(0.);
			l.setLongitude(0.);
			Log.d("LocatioNReceiver", "tempLocation returned. still waiting for new location...");
			return l;
		}else{
			Log.d("LocatioNReceiver", "actual location returned");
			return mCurrentBestLocation;
		}
	}

}
