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
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;

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
import org.pb.android.geomap3d.data.PersistManager;
import org.pb.android.geomap3d.data.map.model.GeoPlaceItem;
import org.pb.android.geomap3d.data.map.model.TerrainMapData.LoadingState;
import org.pb.android.geomap3d.data.map.service.GeoPlaceService;
import org.pb.android.geomap3d.data.map.service.TerrainService;
import org.pb.android.geomap3d.data.persist.geoarea.GeoArea;
import org.pb.android.geomap3d.dialog.ConfirmDialog;
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

    @Bean
    PersistManager persistManager;

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
            // TODO: handle negative result. --> grantPermissions()? (e.g. user does not grant permissions)
            // FIXME: activity needs to be restarted. show confirm-dialog, so user knows what happens.
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
    public void onEvent(final Events.OutsideOfMap event) {
        EventBus.getDefault().removeStickyEvent(event);
        new Thread(new Runnable() {
            @Override
            public void run() {
                setupTerrainWidget(event.getLocation());
            }
        }).start();
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
        } else {
            notifyHeightMapLoaded(null, location, LoadingState.LOADING_FAILED);

            new ConfirmDialog.Builder(this)
                    .setMessage(getString(R.string.noInternet))
                    .build()
                    .show();
        }
    }

    private void setupTerrainWidget(@NonNull Location location) {
        GeoArea geoArea = persistManager.findGeoModelByLocation(GeoUtil.getLatLngFromLocation(location));
        if (geoArea == null) {
            return;
        }

        WidgetConfiguration widgetConfiguration = WidgetConfiguration.create()
                .setLocation(geoArea.getCenterPoint())
                .setHeightMapBitmap(geoArea.getHeightMapBitmap())
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
        Bitmap bitmap = terrainService.getMapForLocation(location);
        LoadingState loadingState = bitmap == null ? LoadingState.LOADING_FAILED : LoadingState.LOADING_SUCCESS;

        GeoArea geoArea = null;

        if (loadingState == LoadingState.LOADING_SUCCESS) {
            LatLng geoLocationCenter = GeoUtil.getLatLngFromLocation(location);
            geoArea = persistManager.storeGeoModel(bitmap, geoLocationCenter, terrainService.getLastTargetBounds());

            List<GeoPlaceItem> geoPlaceItems = geoPlaceService.findGeoPlacesForLocation(geoLocationCenter);
            // TODO: store found places related to freshly created geoLocationModel ...
        }

        notifyHeightMapLoaded(geoArea, location, loadingState);
    }

    @UiThread
    public void notifyHeightMapLoaded(GeoArea geoArea, Location location, LoadingState loadingState) {
        String geoModelName = geoArea == null ? null : geoArea.getName();
        EventBus.getDefault().post(new Events.HeightMapLoaded(geoModelName, location, loadingState));
    }
}
