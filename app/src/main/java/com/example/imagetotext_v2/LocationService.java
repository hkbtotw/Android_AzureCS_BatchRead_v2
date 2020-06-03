package com.example.imagetotext_v2;

import android.Manifest;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimerTask;

// ref: https://www.youtube.com/watch?v=lvcGh2ZgHeA
// Get update locations from services

public class LocationService extends Service {
    private static final int TIME = 5000;
    private static final int FASTTIME = 3000;
    private static final int DISTANCE = 5;

    private LocationListener listener;
    private LocationManager locationManager;
    public Location lastLocation=null;
    private double lastDistance=0;
    public double sumDistance;

    Context context;

    public LocationService(Context applicationContext) {
        super();
        context = applicationContext;
        Log.i("HERE", "here service created!");
    }

    public LocationService() {
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        return START_STICKY;
    }

    @Override
    public void onCreate() {

        listener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                updateWithNewLocation(location);

                Log.i("distance","---> "+sumDistance);
                Intent i=new Intent("location_update");
                i.putExtra("Lat",location.getLatitude());
                i.putExtra("Lng",location.getLongitude());
                i.putExtra("distance",sumDistance);
                sendBroadcast(i);

            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {
                Intent i = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(i);
            }
        };
        locationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        Criteria criteria=new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);

        String provider = locationManager.getBestProvider(criteria, true);
        Log.d("Location"," best provider "+ provider);

        locationManager.requestLocationUpdates(provider, TIME, DISTANCE, listener);

    } //onCreate

    private void updateWithNewLocation(Location location) {
        String latLongString="";
        if(location!=null){
            double lat=location.getLatitude();
            double lng=location.getLongitude();
            latLongString="Lat: "+lat+" ::  Long : "+lng;

            if(lastLocation!=null){
                double elaspedTime=(location.getTime()-lastLocation.getTime())/1000;
                lastDistance=lastLocation.distanceTo(location);
                sumDistance=sumDistance+(lastDistance/1000);
            }
            this.lastLocation=location;
            Log.d("Speed"," distance "+lastDistance+" sumDist "+sumDistance);


        } else {
            latLongString= " No location found ";
        } // if

    }//updateWithNewLocation

    @Override
    public void onDestroy() {
        super.onDestroy();
        Intent broadcastIntent=new Intent("com.example.imagetotext_v2.RestartService");
        sendBroadcast(broadcastIntent);
        if(locationManager!=null){
            locationManager.removeUpdates(listener);
        }
    }// onDestroy
}// service


