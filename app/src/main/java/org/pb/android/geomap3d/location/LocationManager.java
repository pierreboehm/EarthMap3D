package org.pb.android.geomap3d.location;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.GeomagneticField;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import org.androidannotations.annotations.AfterInject;
import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.RootContext;

import java.util.concurrent.atomic.AtomicReference;

@EBean(scope = EBean.Scope.Singleton)
public class LocationManager implements com.google.android.gms.location.LocationListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = LocationManager.class.getSimpleName();

    public interface LocationUpdateListener {
        void onLocationUpdate(Location location);
    }

    @RootContext
    Context context;

    private AtomicReference<GoogleApiClient> googleApiClient = new AtomicReference<>();
    private static final long UPDATE_INTERVAL = 5000, FASTEST_INTERVAL = 5000;

    private LocationUpdateListener locationUpdateListener;
    private GeomagneticField geomagneticField;

    @AfterInject
    void init() {
        googleApiClient = new AtomicReference<>(new GoogleApiClient.Builder(context)
            .addApi(LocationServices.API)
            .addConnectionCallbacks(this)
            .addOnConnectionFailedListener(this)
            .build());
    }

    @Override
    public void onLocationChanged(Location location) {
        if (location != null) {

            geomagneticField = new GeomagneticField(
                    (float) location.getLatitude(),
                    (float) location.getLongitude(),
                    (float) location.getAltitude(),
                    location.getTime()
            );

            if (locationUpdateListener != null) {
                locationUpdateListener.onLocationUpdate(location);
            }
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            Log.v(TAG, "locationManager >> onConnected. Permission check failed.");
            return;
        }

        // Permissions ok, we get last location
        onLocationChanged(LocationServices.FusedLocationApi.getLastLocation(googleApiClient.get()));
        startLocationUpdates();
    }

    @Override
    public void onConnectionSuspended(int i) {
        // not implemented
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        // not implemented
    }

    public void onResume() {
        GoogleApiClient apiClient = googleApiClient.get();
        if (apiClient != null) {
            apiClient.connect();
        }
    }

    public void onPause() {
        GoogleApiClient apiClient = googleApiClient.get();
        if (apiClient != null && apiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(apiClient, this);
            apiClient.disconnect();
        }
    }

    public void setLocationUpdateListener(LocationUpdateListener locationUpdateListener) {
        this.locationUpdateListener = locationUpdateListener;
    }

    @Nullable
    public Location getLastKnownLocation() {
        GoogleApiClient apiClient = googleApiClient.get();
        if (apiClient == null || !apiClient.isConnected()) {
            return null;
        }

        try {
            return LocationServices.FusedLocationApi.getLastLocation(apiClient);
        } catch (SecurityException securityException) {
            return null;
        }
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
        }

        LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient.get(), locationRequest, this, Looper.getMainLooper());
    }
}
