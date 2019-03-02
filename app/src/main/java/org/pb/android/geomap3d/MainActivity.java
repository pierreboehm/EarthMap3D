package org.pb.android.geomap3d;

import android.app.ActivityManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
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
import org.pb.android.geomap3d.widget.WidgetManager;

@EActivity(R.layout.activity_main)
public class MainActivity extends AppCompatActivity {

    @SystemService
    ActivityManager activityManager;

    @Bean
    LocationManager locationManager;

    @Bean
    WidgetManager widgetManager;

    @AfterViews
    public void init() {
        if (widgetManager.getWidget() == null) {
            LoadingFragment loadingFragment = LoadingFragment_.builder().build();
            setFragment(loadingFragment, LoadingFragment.TAG);
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

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(Events.WidgetReady event) {
        TerrainFragment terrainFragment = TerrainFragment_.builder().widget(widgetManager.getWidget()).build();
        setFragment(terrainFragment, TerrainFragment.TAG);
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void onEvent(Events.FragmentLoaded event) {
        if (event.getTag().equals(LoadingFragment.TAG)) {
            if (widgetManager.getWidget() == null) {
                widgetManager.setWidgetForInitiation(new TerrainWidget(this));
            }
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
