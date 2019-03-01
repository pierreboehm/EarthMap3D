package org.pb.android.geomap3d;

import android.app.ActivityManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EActivity;

import org.androidannotations.annotations.SystemService;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.pb.android.geomap3d.compass.Compass;
import org.pb.android.geomap3d.event.Events;
import org.pb.android.geomap3d.location.LocationManager;
import org.pb.android.geomap3d.view.OpenGLSurfaceView;
import org.pb.android.geomap3d.view.ProgressView;
import org.pb.android.geomap3d.widget.TerrainWidget;

import java.util.Locale;

import static org.androidannotations.annotations.UiThread.Propagation.REUSE;

@EActivity(R.layout.activity_main)
public class MainActivity extends AppCompatActivity {

    @SystemService
    ActivityManager activityManager;

    @Bean
    LocationManager locationManager;

    @ViewById(R.id.glSurfaceView)
    OpenGLSurfaceView openGLSurfaceView;

    @ViewById(R.id.progressView)
    ProgressView progressView;

    @ViewById(R.id.tvInfo)
    TextView tvInfo;

    @Bean
    Compass compass;

    private boolean isInitiated = false;

    @AfterViews
    public void init() {
//        progressView.setVisibility(View.VISIBLE);
//        progressView.bringToFront();
    }

    @Override
    public void onResume() {
        super.onResume();

        EventBus.getDefault().register(this);

        initSurfaceViewAndCompass();

        locationManager.onResume();
        openGLSurfaceView.onResume();
        compass.start();

        startWidgetDelayed();
    }

    @Override
    public void onPause() {
        EventBus.getDefault().unregister(this);

        compass.stop();
        openGLSurfaceView.onPause();
        locationManager.onPause();

        super.onPause();
    }

    @Subscribe(threadMode = ThreadMode.ASYNC, sticky = true)
    public void onEvent(Events.ProgressUpdate event) {
        EventBus.getDefault().removeStickyEvent(event);
        Log.v("XXX", ">> progressUpdate: " + ((int) event.getProgressValue()) + "%");

//        progressView.update(event.getProgressValue());
//        progressView.invalidate();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(Events.ProgressFinished event) {
        //
    }

    @UiThread(propagation = REUSE)
    void updateDeviceRotation(float azimuth) {
        openGLSurfaceView.updateDeviceRotation(azimuth);
    }

    @UiThread(delay = 200)
    void startWidgetDelayed() {
        openGLSurfaceView.startWidget();
    }

    private void initSurfaceViewAndCompass() {
        if (!isInitiated) {
            isInitiated = true;
            openGLSurfaceView.initRenderer(this, activityManager);
            compass.setListener(getCompassListener());
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

}
