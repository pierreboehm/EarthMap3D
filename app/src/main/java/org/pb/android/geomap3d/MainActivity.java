package org.pb.android.geomap3d;

import android.Manifest;
import android.app.ActivityManager;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.PermissionChecker;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.SystemService;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.sharedpreferences.Pref;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.pb.android.geomap3d.data.config.TerrainConfig;
import org.pb.android.geomap3d.event.Events;
import org.pb.android.geomap3d.fragment.LoadingFragment;
import org.pb.android.geomap3d.fragment.LoadingFragment_;
import org.pb.android.geomap3d.fragment.TerrainFragment;
import org.pb.android.geomap3d.fragment.TerrainFragment_;
import org.pb.android.geomap3d.location.LocationService_;
import org.pb.android.geomap3d.util.Util;
import org.pb.android.geomap3d.widget.TerrainWidget;
import org.pb.android.geomap3d.widget.Widget;
import org.pb.android.geomap3d.widget.WidgetConfiguration;
import org.pb.android.geomap3d.widget.WidgetManager;

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

    private Toast closeAppToast;

    @AfterViews
    public void init() {

        if (!Util.isGPSEnabled(this)) {
            Util.openLocationSourceSettings(this);
            // TODO: handle result
        }

        if (checkPermissions()) {
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
    public void onEvent(Events.WidgetReady event) {
        TerrainFragment terrainFragment = TerrainFragment_.builder().widget(widgetManager.getWidget()).build();
        setFragment(terrainFragment, TerrainFragment.TAG);
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void onEvent(Events.FragmentLoaded event) {
        if (event.getTag().equals(LoadingFragment.TAG)) {
            setupTerrainWidget(new Location(""));
        }
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

    private void initWidgetAfterPermissionCheck() {
        if (widgetManager.getWidget() == null) {
            LoadingFragment loadingFragment = LoadingFragment_.builder().build();
            setFragment(loadingFragment, LoadingFragment.TAG);
        }
    }

    private void setupTerrainWidget(@NonNull Location location) {
            // 51.281761,9.685705
            TerrainConfig terrainConfig = TerrainConfig.getConfigForLocation(location.getLatitude(), location.getLongitude());
            Location mockLocation = terrainConfig.getLocation();

            WidgetConfiguration widgetConfiguration = WidgetConfiguration.create()
                    .setLocation(mockLocation)
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
}
