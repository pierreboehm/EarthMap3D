package org.pb.android.geomap3d.fragment;

import android.view.SurfaceView;
import androidx.fragment.app.Fragment;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.ViewById;
import org.greenrobot.eventbus.EventBus;
import org.pb.android.geomap3d.R;
import org.pb.android.geomap3d.camera.CameraManager;
import org.pb.android.geomap3d.event.Events;

@EFragment(R.layout.fragment_bionic_eye)
public class BionicEyeFragment extends Fragment {

    public static final String TAG = BionicEyeFragment.class.getSimpleName();

    @ViewById(R.id.bionicEye)
    SurfaceView bionicEyeView;

    @Bean
    CameraManager cameraManager;

    @AfterViews
    public void initViews() {

    }

    @Override
    public void onResume() {
        super.onResume();
        //EventBus.getDefault().register(this);
    }

    @Override
    public void onPause() {

        //EventBus.getDefault().unregister(this);
        super.onPause();
    }

    @Click(R.id.screenSwitch)
    public void onScreenSwitchClick() {
        EventBus.getDefault().post(new Events.ShowMapFragment(null));
    }
}
