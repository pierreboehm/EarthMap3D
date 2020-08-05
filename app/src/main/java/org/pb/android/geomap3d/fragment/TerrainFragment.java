package org.pb.android.geomap3d.fragment;

import android.location.Location;
import android.util.Log;

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
import org.pb.android.geomap3d.dialog.SettingsDialog;
import org.pb.android.geomap3d.event.Events;
import org.pb.android.geomap3d.location.LocationManager;
import org.pb.android.geomap3d.view.OpenGLSurfaceView;
import org.pb.android.geomap3d.widget.TerrainWidget;
import org.pb.android.geomap3d.widget.Widget;

import androidx.fragment.app.Fragment;

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

    private boolean isInitiated = false;

    @AfterViews
    public void initViews() {
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

        EventBus.getDefault().register(this);
        locationManager.setLocationUpdateListener(getLocationUpdateListener());

        boolean useCompass = preferences.useCompass().getOr(true);
        if (useCompass) {
            compass.start();
        }

        if (openGLSurfaceView != null) {
            openGLSurfaceView.setTrackDistance(preferences.defaultTrackDistanceInMeters().getOr(250));
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

    @Click(R.id.screenSwitch)
    public void onScreenSwitchClick() {
        Location lastKnownLocation = ((TerrainWidget) widget).getLastKnownLocation();
        EventBus.getDefault().post(new Events.ShowMapFragment(lastKnownLocation));
    }

    @Click(R.id.bionicEye)
    public void onBionicEyeClick() {
        EventBus.getDefault().post(new Events.ShowBionicEyeFragment());
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

    @Subscribe(threadMode = ThreadMode.ASYNC, sticky = true)
    public void onEvent(Events.LocationUpdate event) {
        EventBus.getDefault().removeStickyEvent(event);
        Log.v(TAG, "collected locations: " + event.getLocations().size());

        for (Location location : event.getLocations()) {
            updateTrackedLocation(location);
        }
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

    private void updateUi() {
        boolean useCompass = preferences.useCompass().getOr(true);
        if (useCompass) {
            compass.start();
        } else {
            compass.stop();
        }

        if (openGLSurfaceView != null) {
            openGLSurfaceView.setTrackDistance(preferences.defaultTrackDistanceInMeters().getOr(250));
        }
    }

    private Compass.CompassListener getCompassListener() {
        return new Compass.CompassListener() {
            @Override
            public void onNewAzimuth(final float azimuth) {
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
