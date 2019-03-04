package org.pb.android.geomap3d.location;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.GeomagneticField;
import android.location.Location;
import android.os.Looper;
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

@EBean(scope = EBean.Scope.Singleton)
public class LocationManager {

    private static final String TAG = LocationManager.class.getSimpleName();

    public interface LocationUpdateListener {
        void onLocationUpdate(Location location);
    }

    @RootContext
    Context context;

    private FusedLocationProviderClient fusedLocationProviderClient;
    private static final long UPDATE_INTERVAL = 5000, FASTEST_INTERVAL = 5000;

    private LocationUpdateListener locationUpdateListener;
    private GeomagneticField geomagneticField;

    private Location lastKnownLocation;
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

    @Nullable
    public Location getLastKnownLocation() {
        return lastKnownLocation;
    }

    public synchronized GeomagneticField getGeomagneticField() {
        return geomagneticField;
    }

    private void startLocationUpdates() {
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
                    public void onFailure(@NonNull Exception e) {
                        Log.d("MapDemoActivity", "Error trying to get last GPS location");
                        e.printStackTrace();
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

            lastKnownLocation = location;

            if (locationUpdateListener != null) {
                locationUpdateListener.onLocationUpdate(location);
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
}
