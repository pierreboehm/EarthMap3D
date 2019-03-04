package org.pb.android.geomap3d;

import android.Manifest;
import android.app.ActivityManager;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.PermissionChecker;
import android.support.v7.app.AppCompatActivity;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.SystemService;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.pb.android.geomap3d.event.Events;
import org.pb.android.geomap3d.fragment.LoadingFragment;
import org.pb.android.geomap3d.fragment.LoadingFragment_;
import org.pb.android.geomap3d.fragment.TerrainFragment;
import org.pb.android.geomap3d.fragment.TerrainFragment_;
import org.pb.android.geomap3d.location.LocationManager;
import org.pb.android.geomap3d.widget.TerrainWidget;
import org.pb.android.geomap3d.widget.WidgetConfiguration;
import org.pb.android.geomap3d.widget.WidgetManager;

@EActivity(R.layout.activity_main)
public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 7321;

    @SystemService
    ActivityManager activityManager;

    @Bean
    LocationManager locationManager;

    @Bean
    WidgetManager widgetManager;

    @AfterViews
    public void init() {
        if (checkPermissions()) {
            initWidgetAfterPermissionCheck();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        EventBus.getDefault().register(this);
        locationManager.onResume();
    }

    @Override
    public void onPause() {
        EventBus.getDefault().unregister(this);
        locationManager.onPause();
        super.onPause();
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

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(Events.WidgetReady event) {
        TerrainFragment terrainFragment = TerrainFragment_.builder().widget(widgetManager.getWidget()).build();
        setFragment(terrainFragment, TerrainFragment.TAG);
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void onEvent(Events.FragmentLoaded event) {
        if (event.getTag().equals(LoadingFragment.TAG)) {
            setupTerrainWidget();
        }
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

    private void setupTerrainWidget() {
        if (widgetManager.getWidget() == null) {

            WidgetConfiguration widgetConfiguration = WidgetConfiguration.create()
                    .setLocation(locationManager.getLastKnownLocation())
                    .setHeightMapResourceId(R.drawable.kaufunger_wald_height_map)
                    .getConfiguration();

            widgetManager.setWidgetForInitiation(new TerrainWidget(this), widgetConfiguration);
        }
    }

    private void setFragment(Fragment fragment, String fragmentTag) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment, fragmentTag)
                .commit();
    }
}
