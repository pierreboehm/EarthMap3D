package org.pb.android.geomap3d.fragment;

import android.content.pm.ActivityInfo;
import android.location.Location;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.FragmentArg;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.pb.android.geomap3d.R;
import org.pb.android.geomap3d.data.PersistManager;
import org.pb.android.geomap3d.data.map.model.TerrainMapData.LoadingState;
import org.pb.android.geomap3d.data.persist.geoarea.GeoArea;
import org.pb.android.geomap3d.event.Events;
import org.pb.android.geomap3d.fragment.ui.MapView;
import org.pb.android.geomap3d.location.LocationManager;
import org.pb.android.geomap3d.util.GeoUtil;
import org.pb.android.geomap3d.view.ProgressView;
import org.pb.android.geomap3d.widget.WidgetManager;

import java.util.List;

import androidx.fragment.app.Fragment;

@EFragment(R.layout.map_fragment)
public class MapFragment extends Fragment {

    public static final String TAG = MapFragment.class.getSimpleName();

    @ViewById(R.id.mapView)
    MapView mapView;

    @ViewById(R.id.progressView)
    ProgressView progressView;

    @ViewById(R.id.screenSwitch)
    ImageButton screenSwitch;

    @Bean
    LocationManager locationManager;

    @Bean
    WidgetManager widgetManager;

    @Bean
    PersistManager persistManager;

    @FragmentArg
    Location lastKnownLocation;

    @AfterViews
    void initViews() {
        progressView.setStrokeWidth(10f);
        progressView.setColor(getContext().getColor(R.color.warm_blue));

        if (lastKnownLocation != null) {
            Log.v(TAG, "Set last known location: " + lastKnownLocation);
            mapView.resetToInitialState();
            mapView.updateLocation(lastKnownLocation);
        } else if (locationManager.getLastKnownLocation() != null) {
            Location lastKnownLocation = locationManager.getLastKnownLocation();
            Log.v(TAG, "Set last known location: " + lastKnownLocation);
            mapView.resetToInitialState();
            mapView.updateLocation(lastKnownLocation);
        } else {
            Log.v(TAG, "No last known location available. Waiting for incoming location update.");
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        EventBus.getDefault().register(this);
        locationManager.setLocationUpdateListener(getLocationUpdateListener());

        updateUi();
    }

    @Override
    public void onPause() {
        locationManager.removeLocationUpdateListener();
        EventBus.getDefault().unregister(this);
        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        super.onPause();
    }

    @Click(R.id.screenSwitch)
    void onScreenSwitchClick() {
        EventBus.getDefault().post(new Events.ShowTerrainFragment());
    }

    @Subscribe(threadMode = ThreadMode.ASYNC, sticky = true)
    public void onEvent(Events.WidgetReady event) {
        EventBus.getDefault().removeStickyEvent(event);
        updateUi();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(Events.HeightMapLoaded event) {
        progressView.setVisibility(View.GONE);

        if (event.getLoadingState() == LoadingState.LOADING_SUCCESS) {
            mapView.addStoredArea(event.getAreaName(), event.getAreaLocation());
        } else {
            progressView.stopBlink();
        }
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void onEvent(Events.ProgressUpdate event) {
        progressViewUpdate(event.getProgressValue());
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(Events.HeightMapLoadStart event) {
        progressView.setVisibility(View.VISIBLE);
        progressView.startBlink();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(final Events.MapReadyEvent event) {
        List<GeoArea> storedGeoAreas = persistManager.getAllGeoModels();
        for (GeoArea geoArea : storedGeoAreas) {
            mapView.addStoredArea(geoArea.getName(), geoArea.getCenter());
        }
    }

    @UiThread
    public void updateUi() {
        screenSwitch.setVisibility(widgetManager.hasWidget() ? View.VISIBLE : View.GONE);
    }

    @UiThread
    public void progressViewUpdate(float progressValue) {
        progressView.stopBlink();
        progressView.update(progressValue);
    }

    @UiThread
    public void checkIfLocationOutsideOfMap(Location location) {
        GeoArea geoArea = persistManager.findGeoModelByLocation(GeoUtil.getLatLngFromLocation(location));
        boolean isLocationOutsideOfMap = geoArea != null && !GeoUtil.isLocationOnMap(location, geoArea);

        if (!widgetManager.hasWidget() || isLocationOutsideOfMap) {
            EventBus.getDefault().post(new Events.OutsideOfMap(location));
        }
    }

    private LocationManager.LocationUpdateListener getLocationUpdateListener() {
        return new LocationManager.LocationUpdateListener() {
            @Override
            public void onLocationUpdate(Location location) {
                mapView.updateLocation(location);
                checkIfLocationOutsideOfMap(location);
            }
        };
    }
}
