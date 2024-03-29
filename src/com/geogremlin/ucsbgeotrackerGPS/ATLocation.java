/*
 * Project: UCSBGeoTracker V1
 * Author: Grant McKenzie (UCSB)
 * Date: May 2011
 */

package com.geogremlin.ucsbgeotrackerGPS;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.IBinder;
import android.telephony.TelephonyManager;
import android.widget.Toast;

public class ATLocation extends Service{
	private NotificationManager mNM;
	private int NOTIFICATION = R.string.local_service_started;
	
	private LocationManager locationManager;
	private LocationListener locationListener;
	private String best;

	
	Criteria crit = null;
	
	private TelephonyManager tm;
	private ConnectivityManager connectivity;
    private String deviceId;
    
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	@Override
	public void onCreate() {
		mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
		
		showNotification();
		
		tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
		connectivity = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		// Get unique device ID
		String tmDevice, tmSerial, androidId;
	    tmDevice = "" + tm.getDeviceId();
	    tmSerial = "" + tm.getSimSerialNumber();
	    androidId = "" + android.provider.Settings.Secure.getString(getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);
	    UUID deviceUuid = new UUID(androidId.hashCode(), ((long)tmDevice.hashCode() << 32) | tmSerial.hashCode());
	    deviceId = deviceUuid.toString();

		locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
		crit = new Criteria();
		crit.setAccuracy(Criteria.ACCURACY_FINE);
		best = locationManager.getBestProvider(crit, true);
	    locationListener = new MyLocationListener();
	}
	
	@Override
	public void onDestroy() {
		mNM.cancel(NOTIFICATION);
		locationManager.removeUpdates(locationListener);
	}
	
	@Override
	public void onStart(Intent intent, int startid) {
		// Attempt to get fix every minute as recommended by Android documentation
		locationManager.requestLocationUpdates(best, 60000, 0, locationListener);
	}
	private void showNotification() {
	      
	      CharSequence text = getText(R.string.local_service_started);
	      // Set the icon, scrolling text and timestamp
	      Notification notification = new Notification(R.drawable.iconnotification, text, System.currentTimeMillis());
	
	      Intent notifyIntent = new Intent(Intent.ACTION_MAIN);
	      notifyIntent.setClass(getApplicationContext(), UCSBGeoTrackerGPSActivity.class);
	      notifyIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
	      
	      // The PendingIntent to launch our activity if the user selects this notification
	      PendingIntent contentIntent = PendingIntent.getActivity(getApplicationContext(), 0, notifyIntent, PendingIntent.FLAG_CANCEL_CURRENT);
	
	      // Set the info for the views that show in the notification panel.
	      notification.setLatestEventInfo(this, getText(R.string.local_service_label), text, contentIntent);
	
	      notification.flags|=Notification.FLAG_NO_CLEAR;
	      // Send the notification
	      startForeground(1337, notification);
	}
	
	public class MyLocationListener implements LocationListener {
		
		@Override
		public void onLocationChanged(Location loc) {
			Long tsLong = System.currentTimeMillis()/1000;
			String ts = tsLong.toString();	
			storeData(""+loc.getLatitude(), ""+loc.getLongitude(), ""+ts);
			
		}

		@Override
		public void onProviderDisabled(String provider) {
			// Toast.makeText( getApplicationContext(),"Gps Disabled",Toast.LENGTH_SHORT ).show();
		}

		@Override
		public void onProviderEnabled(String provider) {
			// Toast.makeText( getApplicationContext(),"Gps Enabled",Toast.LENGTH_SHORT).show();
		}

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {
			// Toast.makeText( getApplicationContext(),"Location Service changed to "+provider,Toast.LENGTH_SHORT).show();
		}

	}

	
	private void storeData(String lat, String lon, String timest) {
		 if (isNetworkAvailable()) {
			 String response = sendLocation(deviceId, lat, lon, timest);
			 try {
				 int resultint = Integer.parseInt(response.replace("\n","").trim());
				 if (resultint == 1) {
					 Toast.makeText( getApplicationContext(),"GPS fix successfully stored in the database.",Toast.LENGTH_SHORT).show();
				 } else {
					 Toast.makeText( getApplicationContext(),"There was an error pushing your GPS fix to the database.",Toast.LENGTH_SHORT).show();
				 }
			 } catch(Exception e) {
				 Toast.makeText( getApplicationContext(),"There was an error pushing your GPS fix to the database.",Toast.LENGTH_SHORT).show();
			 }
			 
		 } else {
			 Toast.makeText( getApplicationContext(),"No Data Connection.\nData not sent to server.",Toast.LENGTH_SHORT).show();
		 }
		 
	}
	

	
	private boolean isNetworkAvailable() {
		ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
	    NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
	    if (activeNetworkInfo != null && activeNetworkInfo.isConnectedOrConnecting()) {
	        return true;
	    }
	    return false;
	}
	
	private String sendLocation(String uid, String lat, String lon, String timest) {
	   String handler = "http://geogremlin.geog.ucsb.edu/android/tracker-gps/store_fix.php";
	   WebService webService = new WebService(handler);
		   
	   //Pass params to db
	   Map<String, String> params = new HashMap<String, String>();
	   params.put("devid", uid);
	   params.put("lat", lat);
	   params.put("lng", lon);
	   params.put("t", timest);
	   params.put("source", best);
	   params.put("app", "Test");
			   
	   try {
		   String response = webService.webGet("", params);
		   return response;
	   } catch(Exception e) {
		   return "error";
	   }
	}
	
}

