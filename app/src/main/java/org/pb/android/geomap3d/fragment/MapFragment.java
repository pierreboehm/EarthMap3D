package org.pb.android.geomap3d.fragment;

import android.content.pm.ActivityInfo;
import android.location.Location;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.maps.model.LatLng;

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
import org.pb.android.geomap3d.data.map.model.TerrainMapData.LoadingState;
import org.pb.android.geomap3d.event.Events;
import org.pb.android.geomap3d.fragment.ui.MapView;
import org.pb.android.geomap3d.location.LocationManager;
import org.pb.android.geomap3d.view.ProgressView;

import java.util.Locale;

import androidx.fragment.app.Fragment;

@EFragment(R.layout.map_fragment)
public class MapFragment extends Fragment {

    public static final String TAG = MapFragment.class.getSimpleName();

    @ViewById(R.id.mapView)
    MapView mapView;

    @ViewById(R.id.tvCenterOfMap)
    TextView tvCenterOfMap;

    @ViewById(R.id.progressView)
    ProgressView progressView;

    @Bean
    LocationManager locationManager;

    @FragmentArg
    Location lastKnownLocation;

    @AfterViews
    public void initViews() {
        progressView.setStrokeWidth(10f);
        progressView.setColor(getContext().getColor(R.color.warm_blue));
        progressView.fireWidgetReadyEvent(false);

        if (lastKnownLocation != null) {
            Log.v(TAG, "Set last known location: " + lastKnownLocation);
            mapView.resetToInitialState();
            mapView.updateLocation(lastKnownLocation);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        EventBus.getDefault().register(this);
        locationManager.setLocationUpdateListener(getLocationUpdateListener());
    }

    @Override
    public void onPause() {
        locationManager.removeLocationUpdateListener();
        EventBus.getDefault().unregister(this);
        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        super.onPause();
    }

    @Click(R.id.screenSwitch)
    public void onScreenSwitchClick() {
        EventBus.getDefault().post(new Events.ShowTerrainFragment());
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(Events.MapCenterUpdate event) {
        String formattedLocation = getFormattedLocation(event.getCenterOfMap());
        tvCenterOfMap.setText(formattedLocation);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(Events.HeightMapLoaded event) {
        // TODO: get all saved GeoModels from database to update MapView (showing all heightmap bounds)
        // --> list of GeoModels?
//        mapView.updateMapAfterHeightMapLoaded();

        progressView.setVisibility(View.GONE);
        if (event.getLoadingState() == LoadingState.LOADING_SUCCESS) {
            mapView.addStoredArea(event.getAreaLocation());
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

    @UiThread
    public void progressViewUpdate(float progressValue) {
//        Log.v(TAG, String.format(Locale.getDefault(), "progressViewUpdate: %.2f %%", progressValue));
        progressView.stopBlink();
        progressView.update(progressValue);
    }

    private String getFormattedLocation(LatLng centerOfMap) {
        return String.format(Locale.US, "Breitengrad: %.06f\nLängengrad: %.06f", centerOfMap.latitude, centerOfMap.longitude);
    }

    private LocationManager.LocationUpdateListener getLocationUpdateListener() {
        return new LocationManager.LocationUpdateListener() {
            @Override
            public void onLocationUpdate(Location location) {
                mapView.updateLocation(location);
            }
        };
    }
}
