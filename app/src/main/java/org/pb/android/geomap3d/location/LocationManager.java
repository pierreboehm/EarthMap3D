package org.pb.android.geomap3d.location;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.GeomagneticField;
import android.location.Location;
import android.os.Build;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import org.androidannotations.annotations.AfterInject;
import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.RootContext;
import org.androidannotations.annotations.SystemService;
import org.androidannotations.annotations.sharedpreferences.Pref;
import org.greenrobot.eventbus.EventBus;
import org.pb.android.geomap3d.AppPreferences_;
import org.pb.android.geomap3d.event.Events;
import org.pb.android.geomap3d.util.GeoUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@EBean(scope = EBean.Scope.Singleton)
public class LocationManager {

    private static final String TAG = LocationManager.class.getSimpleName();

    public interface LocationUpdateListener {
        void onLocationUpdate(Location location);
    }

    @RootContext
    Context context;

    @SystemService
    Vibrator vibrator;

    @Pref
    AppPreferences_ preferences;

    private FusedLocationProviderClient fusedLocationProviderClient;
    private static final long UPDATE_INTERVAL = 5000, FASTEST_INTERVAL = 5000;

    private LocationUpdateListener locationUpdateListener;
    private GeomagneticField geomagneticField;

    private Location lastKnownLocation;
    private List<Location> trackedLocations = new ArrayList<>();
    private LocationCallback locationCallback;

    @AfterInject
    void init() {
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context);
        requestLastLocation();
        locationCallback = getLocationCallback();
    }

    public void onResume() {
        startLocationUpdates();
    }

    public void onPause() {
        stopLocationUpdates();
    }

    public void setLocationUpdateListener(LocationUpdateListener locationUpdateListener) {
        this.locationUpdateListener = locationUpdateListener;
    }

    public void removeLocationUpdateListener() {
        locationUpdateListener = null;
    }

    @Nullable
    public Location getLastKnownLocation() {
        return lastKnownLocation;
    }

    public synchronized GeomagneticField getGeomagneticField() {
        return geomagneticField;
    }

    private void startLocationUpdates() {
        Log.v(TAG, "startLocationUpdates()");
        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(UPDATE_INTERVAL);
        locationRequest.setFastestInterval(FASTEST_INTERVAL);

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                &&  ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(context, "You need to enable permissions to display location !", Toast.LENGTH_SHORT).show();
            return;
        }

        if (fusedLocationProviderClient != null) {
            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        }
    }

    private void stopLocationUpdates() {
        Log.v(TAG, "stopLocationUpdates()");
        if (fusedLocationProviderClient != null) {
            fusedLocationProviderClient.removeLocationUpdates(locationCallback);
        }
    }

    private void requestLastLocation() {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                &&  ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(context, "You need to enable permissions to display location !", Toast.LENGTH_SHORT).show();
            return;
        }

        fusedLocationProviderClient.getLastLocation()
                .addOnSuccessListener(new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        setLocationResult(location);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        Log.v(TAG, exception.getLocalizedMessage());
                    }
                });
    }

    private void setLocationResult(Location location) {
        if (location != null) {

            geomagneticField = new GeomagneticField(
                    (float) location.getLatitude(),
                    (float) location.getLongitude(),
                    (float) location.getAltitude(),
                    location.getTime()
            );

            if (locationUpdateListener != null) {

                if (!trackedLocations.isEmpty()) {
                    EventBus.getDefault().postSticky(new Events.LocationUpdate(trackedLocations));
                    trackedLocations.clear();
                }

                lastKnownLocation = location;
                locationUpdateListener.onLocationUpdate(location);
            } else {

                if (preferences.trackPosition().getOr(true) && lastKnownLocation != null) {
                    Location lastKnownTrackedLocation = trackedLocations.isEmpty() ? lastKnownLocation : trackedLocations.get(trackedLocations.size() - 1);
                    if (GeoUtil.getDistanceBetweenTwoPointsInMeter(lastKnownTrackedLocation, location) >= preferences.defaultTrackDistanceInMeters().getOr(250)) {
                        Log.v(TAG, String.format(Locale.US, "new location tracked: lat=%.06f, lng=%.06f", location.getLatitude(), location.getLongitude()));
                        trackedLocations.add(location);
                        vibrateTrackedPositionFound();
                    }
                }
            }
        }
    }

    private LocationCallback getLocationCallback() {
        return new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                setLocationResult(locationResult.getLastLocation());
            }

            @Override
            public void onLocationAvailability(LocationAvailability var1) {
            }
        };
    }

    private void vibrateTrackedPositionFound() {
        if (Build.VERSION.SDK_INT >= 26) {
            vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            vibrator.vibrate(50);
        }
    }
}
