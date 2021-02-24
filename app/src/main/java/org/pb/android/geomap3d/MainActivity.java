package org.pb.android.geomap3d;

import android.Manifest;
import android.app.ActivityManager;
import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Location;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
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
import org.pb.android.geomap3d.data.route.model.Route;
import org.pb.android.geomap3d.dialog.ConfirmDialog;
import org.pb.android.geomap3d.event.Events;
import org.pb.android.geomap3d.fragment.BionicEyeFragment;
import org.pb.android.geomap3d.fragment.BionicEyeFragment_;
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
    AudioManager audioManager;

    @SystemService
    KeyguardManager keyguardManager;

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
            // --> openPermissionsSettings ?
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // setup location service
        if (preferences.trackPosition().getOr(true)) {
            LocationService_.intent(getApplicationContext()).start();
        }

        registerDeviceLockReceiver();

        Log.d(TAG, "Activity created.");
    }

    @Override
    public void onResume() {
        super.onResume();

        // store current stream music volume. set stream music volume to maximum
        preferences.lastStreamVolume().put(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC));
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC), 0);

        /*
        // setup location service
        if (!preferences.trackPosition().getOr(true)) {
            LocationService_.intent(getApplicationContext()).start();
        }
         */

        EventBus.getDefault().register(this);

        Log.d(TAG, "Activity resumed.");
    }

    @Override
    public void onPause() {

        // restore stream music volume
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, preferences.lastStreamVolume().get(), 0);

        /*
        // cleanup location service
        if (!preferences.trackPosition().getOr(true)) {
            LocationService_.intent(getApplicationContext()).stop();
        }
         */

        EventBus.getDefault().unregister(this);

        Log.d(TAG, "Activity paused.");
        super.onPause();
    }

    @Override
    public void onDestroy() {
        unregisterDeviceLockReceiver();

        if (preferences.trackPosition().getOr(true)) {
            LocationService_.intent(getApplicationContext()).stop();
        }

        Log.d(TAG, "Activity terminated.");
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
            Log.d(TAG, "Activity termination requested.");
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

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(Events.ShowBionicEyeFragment event) {
        BionicEyeFragment bionicEyeFragment = BionicEyeFragment_.builder().build();
        setFragment(bionicEyeFragment, BionicEyeFragment.TAG);
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

        if (widgetManager.hasWidget()) {
            Log.d(TAG, ">> outside of map");

            new Thread(new Runnable() {
                @Override
                public void run() {
                    setupTerrainWidget(event.getLocation());
                }
            }).start();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(final Events.MapReadyEvent event) {
        Log.d(TAG, ">> map ready");
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

    private void registerDeviceLockReceiver() {
        IntentFilter deviceLockFilter = new IntentFilter();

        deviceLockFilter.addAction(Intent.ACTION_SCREEN_ON);
        deviceLockFilter.addAction(Intent.ACTION_SCREEN_OFF);
        deviceLockFilter.addAction(Intent.ACTION_USER_PRESENT);
        deviceLockFilter.addAction(Intent.ACTION_USER_BACKGROUND);
        deviceLockFilter.addAction(Intent.ACTION_USER_FOREGROUND);

        registerReceiver(deviceLockBroadcastReceiver, deviceLockFilter);
        Log.d(TAG, "Device lock receiver registered.");
    }

    private void unregisterDeviceLockReceiver() {
        unregisterReceiver(deviceLockBroadcastReceiver);
        Log.d(TAG, "Device lock receiver unregistered.");
    }

    private BroadcastReceiver deviceLockBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String intentAction = intent.getAction();
            boolean deviceLocked = keyguardManager.isDeviceLocked();

            if (intentAction.equals(Intent.ACTION_SCREEN_ON) || intentAction.equals(Intent.ACTION_SCREEN_OFF) || intentAction.equals(Intent.ACTION_USER_PRESENT)) {
                Log.i(TAG, String.format("action = %s locked = %b", intentAction, deviceLocked));
            }
        }
    };

    private boolean checkPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                &&  ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                        && ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            return true;
        } else {
            requestPermissions();
            return false;
        }
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE},
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
        FragmentManager fragmentManager = getSupportFragmentManager();
        if (fragmentManager.findFragmentByTag(MapFragment.TAG) == null) {
            MapFragment mapFragment = MapFragment_.builder().build();
            setFragment(mapFragment, MapFragment.TAG);
        }
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
        GeoArea geoArea = persistManager.findGeoAreaByLocation(GeoUtil.getLatLngFromLocation(location));
        if (geoArea == null) {
            Log.d(TAG, ">> no matching geo-area found");

            // FIXME: try calling loadMapForLocation(location) here

            return;
        }

        List<Route> routeList = Util.loadAvailableRoutes(this);
        Route route = routeList.isEmpty() ? null : routeList.get(0);
        // TODO: Getting first route is just for test. Otherwise route is selected by current location.

        WidgetConfiguration widgetConfiguration = WidgetConfiguration.create()
                .setLocation(geoArea.getCenterPoint())
                .setHeightMapBitmap(geoArea.getHeightMapBitmap())
                .setRoute(route)
                .getConfiguration();

        Widget terrainWidget = widgetManager.getWidget();

        if (terrainWidget == null) {
            terrainWidget = new TerrainWidget();
        }

        widgetManager.setWidgetForInitiationOrUpdate(terrainWidget, widgetConfiguration);

        if (NetworkAvailabilityUtil.isNetworkAvailable()) {
            findGeoPlacesForLocation(geoArea.getCenter());
        }
    }

    private void setFragment(Fragment fragment, String fragmentTag) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment, fragmentTag)
                .commit();
    }

    /*
        NOTE: separate loading geo-places from method loadMapForLocation()
        (because getting geo-places is currently just called if a new map is added)
        It should also be possible in other cases.
     */

    @Background
    public void loadMapForLocation(Location location) {
        Bitmap bitmap = terrainService.getMapForLocation(location);
        LoadingState loadingState = bitmap == null ? LoadingState.LOADING_FAILED : LoadingState.LOADING_SUCCESS;

        GeoArea geoArea = null;

        if (loadingState == LoadingState.LOADING_SUCCESS) {
            LatLng geoLocationCenter = GeoUtil.getLatLngFromLocation(location);
            geoArea = persistManager.storeGeoArea(bitmap, geoLocationCenter, terrainService.getLastTargetBounds());

            findGeoPlacesForLocation(geoLocationCenter);
        }

        notifyHeightMapLoaded(geoArea, location, loadingState);
    }

    @Background
    public void loadGeoPlacesForLocation(Location location) {
        LatLng geoLocationCenter = GeoUtil.getLatLngFromLocation(location);
        findGeoPlacesForLocation(geoLocationCenter);
    }

    @UiThread
    public void notifyHeightMapLoaded(GeoArea geoArea, Location location, LoadingState loadingState) {
        String geoModelName = geoArea == null ? null : geoArea.getName();
        EventBus.getDefault().post(new Events.HeightMapLoaded(geoModelName, location, loadingState));
    }

    private void findGeoPlacesForLocation(LatLng locationCenter) {
        List<GeoPlaceItem> geoPlaceItems = geoPlaceService.findGeoPlacesForLocation(locationCenter);
        if (geoPlaceItems.isEmpty()) {
            Log.d(TAG, "No geoplaces found for location.");
            return;
        }

        Log.d(TAG, "Found geoplaces for location:");
        // TODO: store found places related to freshly created geoLocationModel ...
        for (GeoPlaceItem geoPlaceItem : geoPlaceItems) {
            Log.d(TAG, String.format(">> %s (%s) lat=%.4f lng=%.4f dist=%.2f km",
                    geoPlaceItem.getCity(),
                    geoPlaceItem.getName(),
                    geoPlaceItem.getLatitude(),
                    geoPlaceItem.getLongitude(),
                    geoPlaceItem.getDistanceInKilometers())
            );
        }
    }
}
