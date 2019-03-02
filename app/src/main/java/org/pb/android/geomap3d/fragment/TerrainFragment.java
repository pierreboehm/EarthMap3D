package org.pb.android.geomap3d.fragment;

import android.support.v4.app.Fragment;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.FragmentArg;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;
import org.greenrobot.eventbus.EventBus;
import org.pb.android.geomap3d.R;
import org.pb.android.geomap3d.compass.Compass;
import org.pb.android.geomap3d.event.Events;
import org.pb.android.geomap3d.view.OpenGLSurfaceView;
import org.pb.android.geomap3d.widget.Widget;

import static org.androidannotations.annotations.UiThread.Propagation.REUSE;

@EFragment(R.layout.fragment_terrain)
public class TerrainFragment extends Fragment {

    public static final String TAG = TerrainFragment.class.getSimpleName();

    @FragmentArg
    Widget widget;

    @Bean
    Compass compass;

    @ViewById(R.id.glSurfaceView)
    OpenGLSurfaceView openGLSurfaceView;

    private boolean isInitiated = false;

    @AfterViews
    public void initViews() {
        if (!isInitiated) {
            openGLSurfaceView.initRenderer(getActivity());
            openGLSurfaceView.setWidget(widget);
            compass.setListener(getCompassListener());
            isInitiated = true;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        compass.start();
        EventBus.getDefault().post(new Events.FragmentLoaded(TAG));
    }

    @Override
    public void onPause() {
        compass.stop();
        super.onPause();
    }

    @UiThread(propagation = REUSE)
    void updateDeviceRotation(float azimuth) {
        openGLSurfaceView.updateDeviceRotation(azimuth);
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
