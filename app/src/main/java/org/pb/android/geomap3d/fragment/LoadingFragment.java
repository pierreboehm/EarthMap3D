package org.pb.android.geomap3d.fragment;

import android.support.v4.app.Fragment;

import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.ViewById;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.pb.android.geomap3d.R;
import org.pb.android.geomap3d.event.Events;
import org.pb.android.geomap3d.view.ProgressView;

@EFragment(R.layout.fragment_loading)
public class LoadingFragment extends Fragment {

    public static final String TAG = LoadingFragment.class.getSimpleName();

    @ViewById(R.id.progressView)
    ProgressView progressView;

    @Override
    public void onResume() {
        super.onResume();
        EventBus.getDefault().register(this);
        EventBus.getDefault().post(new Events.FragmentLoaded(TAG));
    }

    @Override
    public void onPause() {
        EventBus.getDefault().unregister(this);
        super.onPause();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(Events.ProgressUpdate event) {
        progressView.update(event.getProgressValue());
    }
}
