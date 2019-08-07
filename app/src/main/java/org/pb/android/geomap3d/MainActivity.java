package org.pb.android.geomap3d;

import android.Manifest;
import android.app.ActivityManager;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.widget.Toast;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.SystemService;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.sharedpreferences.Pref;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.pb.android.geomap3d.data.config.TerrainConfig;
import org.pb.android.geomap3d.data.map.model.GeoPlace;
import org.pb.android.geomap3d.data.map.model.TerrainMapData.LoadingState;
import org.pb.android.geomap3d.data.map.service.GeoPlaceService;
import org.pb.android.geomap3d.data.map.service.TerrainService;
import org.pb.android.geomap3d.event.Events;
import org.pb.android.geomap3d.fragment.MapFragment;
import org.pb.android.geomap3d.fragment.MapFragment_;
import org.pb.android.geomap3d.fragment.TerrainFragment;
import org.pb.android.geomap3d.fragment.TerrainFragment_;
import org.pb.android.geomap3d.location.LocationService_;
import org.pb.android.geomap3d.util.GeoUtil;
import org.pb.android.geomap3d.util.NetworkAvailabilityUtil;
import org.pb.android.geomap3d.util.Util;
import org.pb.android.geomap3d.widget.TerrainWidget;
import org.pb.android.geomap3d.widget.Widget;
import org.pb.android.geomap3d.widget.WidgetConfiguration;
import org.pb.android.geomap3d.widget.WidgetManager;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.PermissionChecker;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

@EActivity(R.layout.activity_main)
public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int PERMISSION_REQUEST_CODE = 7321;

    @SystemService
    ActivityManager activityManager;

    @SystemService
    Vibrator vibrator;

    @Pref
    AppPreferences_ preferences;

    @Bean
    WidgetManager widgetManager;

    @Bean
    TerrainService terrainService;

    @Bean
    GeoPlaceService geoPlaceService;

    private Toast closeAppToast;

    @AfterViews
    public void init() {
        if (!Util.isGPSEnabled(this)) {
            Util.openLocationSourceSettings(this);
            // TODO: handle result
        }

        if (checkPermissions()) {
            initNetworkAvailabilityUtil();
            initWidgetAfterPermissionCheck();
        } else {
            // TODO: handle negative result. (user does not grant permissions)
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (preferences.trackPosition().getOr(true)) {
            LocationService_.intent(getApplicationContext()).start();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (!preferences.trackPosition().getOr(true)) {
            LocationService_.intent(getApplicationContext()).start();
        }

        EventBus.getDefault().register(this);
    }

    @Override
    public void onPause() {
        if (!preferences.trackPosition().getOr(true)) {
            LocationService_.intent(getApplicationContext()).stop();
        }

        EventBus.getDefault().unregister(this);
        super.onPause();
    }

    @Override
    public void onDestroy() {
        if (preferences.trackPosition().getOr(true)) {
            LocationService_.intent(getApplicationContext()).stop();
        }

        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allPermissionsGranted = true;

            for (int index = 0; index < permissions.length; index++) {
                int result = grantResults[index];

                if (result != PermissionChecker.PERMISSION_GRANTED) {
                    allPermissionsGranted = false;
                }
            }

            if (allPermissionsGranted) {
                initWidgetAfterPermissionCheck();
            }
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onBackPressed() {
        if (closeAppToast != null) {
            closeAppToast.cancel();
            finish();
        } else {
            closeAppToast = Toast.makeText(this, R.string.backPressedHintText, Toast.LENGTH_SHORT);
            closeAppToast.show();
            resetCloseAppToast();
        }
    }

    @UiThread(delay = 2000)
    void resetCloseAppToast() {
        closeAppToast = null;
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(Events.ShowMapFragment event) {
        MapFragment mapFragment = MapFragment_.builder().lastKnownLocation(event.getLocation()).build();
        setFragment(mapFragment, MapFragment.TAG);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(Events.ShowTerrainFragment event) {
        TerrainFragment terrainFragment = TerrainFragment_.builder().widget(widgetManager.getWidget()).build();
        setFragment(terrainFragment, TerrainFragment.TAG);
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void onEvent(Events.WidgetReady event) {
        Log.v(TAG, "Widget is ready");
//        TerrainFragment terrainFragment = TerrainFragment_.builder().widget(widgetManager.getWidget()).build();
//        setFragment(terrainFragment, TerrainFragment.TAG);
    }

    @Deprecated
    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void onEvent(Events.FragmentLoaded event) {
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(Events.VibrationEvent event) {
        if (Build.VERSION.SDK_INT >= 26) {
            vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            vibrator.vibrate(100);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(Events.ShowToast event) {
        Toast.makeText(this, event.getMessage(), Toast.LENGTH_LONG).show();
    }

    @Subscribe(threadMode = ThreadMode.ASYNC, sticky = true)
    public void onEvent(Events.OutsideOfMap event) {
        EventBus.getDefault().removeStickyEvent(event);
        setupTerrainWidget(event.getLocation());
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(final Events.MapReadyEvent event) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                setupTerrainWidget(event.getCurrentLocation());
            }
        }).start();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(Events.LoadHeightMap event) {
        preloadMapForLocation(event.getTargetLocation());
    }

    private boolean checkPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                &&  ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            return true;
        } else {
            requestPermissions();
            return false;
        }
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION},
                PERMISSION_REQUEST_CODE);
    }

    private void initNetworkAvailabilityUtil() {
        NetworkAvailabilityUtil.setNetworkAvailabilityCheck(new NetworkAvailabilityUtil.NetworkAvailabilityCheck() {
            @Override
            public boolean isNetworkAvailable() {
                return Util.isNetworkAvailable(getBaseContext());
            }
        });
    }

    private void initWidgetAfterPermissionCheck() {
        MapFragment mapFragment = MapFragment_.builder().build();
        setFragment(mapFragment, MapFragment.TAG);
    }

    private void preloadMapForLocation(@NonNull Location location) {
        if (NetworkAvailabilityUtil.isNetworkAvailable()) {
            loadMapForLocation(location);
        }
    }

    private void setupTerrainWidget(@NonNull Location location) {
        TerrainConfig terrainConfig = TerrainConfig.getConfigForLocation(location.getLatitude(), location.getLongitude());
        Location terrainLocation = terrainConfig.getLocation();

        WidgetConfiguration widgetConfiguration = WidgetConfiguration.create()
                .setLocation(terrainLocation)
                .setHeightMapBitmapFromResource(this, terrainConfig.getHeightMapResourceId())
                .getConfiguration();

        Widget terrainWidget = widgetManager.getWidget();

        if (terrainWidget == null) {
            terrainWidget = new TerrainWidget();
        }

        widgetManager.setWidgetForInitiation(terrainWidget, widgetConfiguration);
    }

    private void setFragment(Fragment fragment, String fragmentTag) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment, fragmentTag)
                .commit();
    }

    @Background
    public void loadMapForLocation(Location location) {
        // store that data as part of new TerrainConfig
        List<GeoPlace> geoPlaces = geoPlaceService.findGeoPlacesForLocation(GeoUtil.getLatLngFromLocation(location));

        // store that data as part of new TerrainConfig
        Bitmap bitmap = terrainService.getMapForLocation(location);

        // (just) signal that a new TerrainConfig is available. (Stored data will be reloaded then and map-UI will be updated)
        LoadingState loadingState = bitmap == null ? LoadingState.LOADING_FAILED : LoadingState.LOADING_SUCCESS;
        notifyHeightMapLoaded(location, loadingState);
    }

    @UiThread
    public void notifyHeightMapLoaded(Location location, LoadingState loadingState) {
        EventBus.getDefault().post(new Events.HeightMapLoaded(location, loadingState));
    }
}
