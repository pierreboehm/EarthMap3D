package org.pb.android.geomap3d.location;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EService;

import androidx.annotation.Nullable;

@EService
public class LocationService extends Service {

    private static final String TAG = LocationService.class.getSimpleName();

    @Bean
    LocationManager locationManager;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.v(TAG, "onCreate()");
        locationManager.onResume();
    }

    @Override
    public void onDestroy() {
        Log.v(TAG, "onDestroy()");
        locationManager.onPause();
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        Log.v(TAG, intent.toString() + ", flags = " + flags + ", id = " + startId);
        return START_STICKY;
    }
}
