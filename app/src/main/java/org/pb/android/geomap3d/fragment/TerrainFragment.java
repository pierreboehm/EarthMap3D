package org.pb.android.geomap3d.fragment;

import android.content.res.Configuration;
import android.location.Location;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.FragmentArg;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;
import org.androidannotations.annotations.sharedpreferences.Pref;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.pb.android.geomap3d.AppPreferences_;
import org.pb.android.geomap3d.R;
import org.pb.android.geomap3d.compass.Compass;
import org.pb.android.geomap3d.data.PersistManager;
import org.pb.android.geomap3d.dialog.ConfirmDialog;
import org.pb.android.geomap3d.dialog.SettingsDialog;
import org.pb.android.geomap3d.event.Events;
import org.pb.android.geomap3d.location.LocationManager;
import org.pb.android.geomap3d.util.Util;
import org.pb.android.geomap3d.view.OpenGLSurfaceView;
import org.pb.android.geomap3d.view.OverlayView;
import org.pb.android.geomap3d.widget.TerrainWidget;
import org.pb.android.geomap3d.widget.Widget;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import java.util.Objects;

import static org.androidannotations.annotations.UiThread.Propagation.REUSE;

@EFragment(R.layout.fragment_terrain)
public class TerrainFragment extends Fragment {

    public static final String TAG = TerrainFragment.class.getSimpleName();

    @FragmentArg
    Widget widget;

    @Bean
    LocationManager locationManager;

    @Bean
    PersistManager persistManager;

    @Bean
    Compass compass;

    @Pref
    AppPreferences_ preferences;

    @ViewById(R.id.glSurfaceView)
    OpenGLSurfaceView openGLSurfaceView;

    @ViewById(R.id.overlayView)
    OverlayView overlayView;

    @ViewById(R.id.bionicEye)
    ImageView bionicEye;

    private boolean isInitiated = false;

    @AfterViews
    public void initViews() {

        if (Util.getOrientation(Objects.requireNonNull(getContext())) == Util.Orientation.PORTRAIT) {
            bionicEye.setVisibility(View.GONE);
        }

        if (!isInitiated) {
            openGLSurfaceView.initRenderer(getActivity());
            openGLSurfaceView.setWidget(widget);
            compass.setListener(getCompassListener());
            isInitiated = true;
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        setRetainInstance(true);

        EventBus.getDefault().register(this);
        locationManager.setLocationUpdateListener(getLocationUpdateListener());

        boolean useCompass = preferences.useCompass().getOr(true);
        if (useCompass) {
            compass.start();
        }

        if (openGLSurfaceView != null) {
            openGLSurfaceView.setTrackEnabled(preferences.trackPosition().getOr(true));
            openGLSurfaceView.setShowGeoPlaces(preferences.showPlaces().getOr(true));
            openGLSurfaceView.setTrackDistance(preferences.defaultTrackDistanceInMeters().getOr(50));

            float campLatitude = preferences.campLatitude().get();
            float campLongitude = preferences.campLongitude().get();

            if (campLatitude == -1f || campLongitude == -1f) {
                openGLSurfaceView.setCampLocation(null);
            } else {
                Location campLocation = new Location("");
                campLocation.setLatitude(campLatitude);
                campLocation.setLongitude(campLongitude);
                openGLSurfaceView.setCampLocation(campLocation);
            }
        }
    }

    @Override
    public void onPause() {
        locationManager.removeLocationUpdateListener();

        EventBus.getDefault().unregister(this);

        boolean useCompass = preferences.useCompass().getOr(true);
        if (useCompass) {
            compass.stop();
        }

        super.onPause();
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        handleOrientation(newConfig);
    }

    @Click(R.id.screenSwitch)
    public void onScreenSwitchClick() {
        //Location lastKnownLocation = ((TerrainWidget) widget).getLastKnownLocation();
        Location lastKnownLocation = locationManager.getLastKnownLocation();
        EventBus.getDefault().post(new Events.ShowMapFragment(lastKnownLocation));
    }

    @Click(R.id.bionicEye)
    public void onBionicEyeClick() {
        EventBus.getDefault().post(new Events.ShowBionicEyeFragment());
    }

    @Click(R.id.markCamp)
    public void onMarkCampClick() {
        new ConfirmDialog.Builder(getContext())
            .setMessage("Set this location to camp location?")
            .setConfirmAction(new Runnable() {
                @Override
                public void run() {
                    Location lastKnownLocation = locationManager.getLastKnownLocation();
                    setAndStoreCampLocation(lastKnownLocation);
                }
            })
            .build()
            .show();
    }

    @Click(R.id.trackSettings)
    public void onTrackSettingsClick() {
        new SettingsDialog.Builder(getContext())
            .setSaveAction(new Runnable() {
                @Override
                public void run() {
                    updateUi();
                }
            })
            .build()
            .show();
    }

    @Click(R.id.overlayView)
    public void onOverlayViewClick() {
        overlayView.cleanup();
        overlayView.setVisibility(View.GONE);
    }

    @Subscribe(threadMode = ThreadMode.ASYNC, sticky = true)
    public void onEvent(Events.LocationUpdate event) {
        EventBus.getDefault().removeStickyEvent(event);
        Log.v(TAG, "collected locations: " + event.getLocations().size());

        for (Location location : event.getLocations()) {
            updateTrackedLocation(location);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(Events.TrackDistanceChanged event) {
        if (openGLSurfaceView != null) {
            openGLSurfaceView.setTrackEnabled(preferences.trackPosition().getOr(true));
            openGLSurfaceView.setTrackDistance(event.getTrackDistance());
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(Events.ShowGeoPlaces event) {
        if (openGLSurfaceView != null) {
            openGLSurfaceView.setShowGeoPlaces(event.show());
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    public void onEvent(Events.GeoPlacesAvailable event) {
        EventBus.getDefault().removeStickyEvent(event);
        if (openGLSurfaceView != null) {
            openGLSurfaceView.setGeoPlaces(event.getGeoPlaces());
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(Events.ShowGeoPlaceInfo event) {
        if (overlayView.getVisibility() == View.GONE) {
            overlayView.setVisibility(View.VISIBLE);
            int geoPlacesCount = ((TerrainWidget) widget).getGeoPlacesCount();
            overlayView.setMaxViewCount(geoPlacesCount);
        }

        overlayView.addInfoItem(event.getGeoPlace());
    }

    @UiThread(propagation = REUSE)
    void updateDeviceRotation(float azimuth) {
        if (openGLSurfaceView != null) {
            openGLSurfaceView.updateDeviceRotation(azimuth);
        }
    }

    @UiThread(propagation = REUSE)
    void updateDeviceLocation(Location location) {
        if (openGLSurfaceView != null) {
            openGLSurfaceView.updateDeviceLocation(location);
        }
    }

    @UiThread(propagation = REUSE)
    void updateTrackedLocation(Location location) {
        if (openGLSurfaceView != null) {
            openGLSurfaceView.updateTrackedLocation(location);
        }
    }

    private void setAndStoreCampLocation(Location location) {
        if (openGLSurfaceView != null) {
            openGLSurfaceView.setCampLocation(location);

            float latitude = location == null ? -1f : (float) location.getLatitude();
            float longitude = location == null ? -1f : (float) location.getLongitude();

            preferences.campLatitude().put(latitude);
            preferences.campLongitude().put(longitude);
        }
    }

    private void handleOrientation(Configuration configuration) {
        if (configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
            bionicEye.setVisibility(View.GONE);
        } else {
            bionicEye.setVisibility(View.VISIBLE);
        }
    }

    private void updateUi() {
        boolean useCompass = preferences.useCompass().getOr(true);
        if (useCompass) {
            compass.start();
        } else {
            compass.stop();
        }

        if (openGLSurfaceView != null) {
            openGLSurfaceView.setTrackEnabled(preferences.trackPosition().getOr(true));
            openGLSurfaceView.setTrackDistance(preferences.defaultTrackDistanceInMeters().getOr(50));
        }
    }

    private Compass.CompassListener getCompassListener() {
        return new Compass.CompassListener() {
            @Override
            public void onRotationChanged(float azimuth, float pitch, float roll) {
                updateDeviceRotation(azimuth);
            }
        };
    }

    private LocationManager.LocationUpdateListener getLocationUpdateListener() {
        return new LocationManager.LocationUpdateListener() {
            @Override
            public void onLocationUpdate(Location location) {
                updateDeviceLocation(location);
            }
        };
    }
}
