package org.pb.android.geomap3d.fragment;

import android.content.res.Configuration;
import android.util.Log;
import android.view.SurfaceView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.ViewById;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.pb.android.geomap3d.R;
import org.pb.android.geomap3d.camera.CameraPreviewManager;
import org.pb.android.geomap3d.compass.Compass;
import org.pb.android.geomap3d.event.Events;
import org.pb.android.geomap3d.util.Util;
import org.pb.android.geomap3d.view.BionicEyeView;

import java.util.Objects;

@EFragment(R.layout.fragment_bionic_eye)
public class BionicEyeFragment extends Fragment {

    public static final String TAG = BionicEyeFragment.class.getSimpleName();

    @ViewById(R.id.previewSurfaceView)
    SurfaceView previewSurfaceView;

    @ViewById(R.id.bionicEyeView)
    BionicEyeView bionicEyeView;

    @Bean
    CameraPreviewManager cameraPreviewManager;

    @Bean
    Compass compass;

    private Util.Orientation orientation;

    @AfterViews
    public void initViews() {
        compass.setListener(getGravityListener());
        //compass.setListener(getCompassListener());
    }

    @Override
    public void onResume() {
        super.onResume();

        setRetainInstance(true);
        EventBus.getDefault().register(this);

        orientation = Util.getOrientation(Objects.requireNonNull(getContext()));

        cameraPreviewManager.resume(previewSurfaceView);
        cameraPreviewManager.orientationChanged(orientation);

        compass.start();
    }

    @Override
    public void onPause() {
        EventBus.getDefault().unregister(this);
        compass.stop();
        cameraPreviewManager.pause();
        super.onPause();
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        setOrientation(newConfig);
        super.onConfigurationChanged(newConfig);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(Events.CameraStateEvent event) {
        bionicEyeView.setAutoFocusState(event.getAfState());
    }

    @Click(R.id.screenSwitch)
    public void onScreenSwitchClick() {
        EventBus.getDefault().post(new Events.ShowTerrainFragment());
    }

    private void setOrientation(Configuration newConfig) {
        if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            orientation = Util.Orientation.PORTRAIT;
        } else {
            orientation = Util.Orientation.LANDSCAPE;
        }

        cameraPreviewManager.orientationChanged(orientation);
        Log.d(TAG, "new orientation: " + orientation);
    }

    /*private Compass.CompassListener getCompassListener() {
        return new Compass.CompassListener() {
            @Override
            public void onNewAzimuth(float azimuth) {
                bionicEyeView.updateDeviceRotation(azimuth);
            }
        };
    }*/

    private Compass.GravityListener getGravityListener() {
        return new Compass.GravityListener() {
            @Override
            public void onNewGravityData(final float[] gravity) {
                bionicEyeView.updateDeviceOrientation(orientation, gravity);
            }
        };
    }
}
