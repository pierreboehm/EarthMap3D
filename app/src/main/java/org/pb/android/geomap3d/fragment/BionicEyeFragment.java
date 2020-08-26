package org.pb.android.geomap3d.fragment;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.res.Configuration;
import android.util.Log;
import android.view.SurfaceView;

import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;

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
import org.pb.android.geomap3d.audio.AudioPlayer;
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

    @ViewById(R.id.ivHudLeft)
    ImageView ivHudLeft;

    @ViewById(R.id.ivHudRight)
    ImageView ivHudRight;

    @ViewById(R.id.switchZoom)
    ImageButton ibSwitchZoom;

    @Bean
    CameraPreviewManager cameraPreviewManager;

    @Bean
    Compass compass;

    @Bean
    AudioPlayer audioPlayer;

    private Util.Orientation orientation;
    private boolean zoomActive = false;

    @AfterViews
    public void initViews() {
        compass.setListener(getGravityListener());
        //compass.setListener(getCompassListener());
    }

    @Override
    public void onResume() {
        super.onResume();

        //setRetainInstance(true);
        EventBus.getDefault().register(this);

        orientation = Util.getOrientation(Objects.requireNonNull(getContext()));

        cameraPreviewManager.resume(previewSurfaceView);
        cameraPreviewManager.orientationChanged(orientation);

        compass.start();
    }

    @Override
    public void onPause() {
        EventBus.getDefault().unregister(this);
        audioPlayer.release();
        compass.stop();
        cameraPreviewManager.pause();
        super.onPause();
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        setOrientation(newConfig);
    }

    /*@Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(Events.CameraStateEvent event) {
        bionicEyeView.setAutoFocusState(event.getAfState());
    }*/

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(Events.BionicEyeReady event) {
        animateBionicEyeReady();
    }

    /*@Subscribe(threadMode = ThreadMode.ASYNC)
    public void onEvent(Events.ShowZoomedRegion event) {
        showZoomedRegion(event.getBitmap());
    }*/

    @Click(R.id.bionicEyeView)
    public void onBionicEyeViewClick() {
        cameraPreviewManager.captureImage();
    }

    @Click(R.id.screenSwitch)
    public void onScreenSwitchClick() {
        animateBionicEyeClose(new Events.ShowTerrainFragment());
    }

    @Click(R.id.switchZoom)
    public void onSwitchZoomClick() {
        zoomActive = !zoomActive;

        ibSwitchZoom.setImageResource(zoomActive ? R.drawable.icn_zoom_out_c00 : R.drawable.icn_zoom_in_c00);
        //overlayView.setVisibility(zoomActive ? View.VISIBLE : View.INVISIBLE);
        cameraPreviewManager.setZoomActive(zoomActive);

        //if (zoomActive) {
        //    cameraPreviewManager.captureImage();
        //}
    }

    /*@UiThread
    public void showZoomedRegion(Bitmap bitmap) {
        if (zoomActive) {
            //overlayView.bind(bitmap);
            cameraPreviewManager.captureImage();
        }
    }*/

    private void animateBionicEyeReady() {
        animateBionicEye(170f, 50f, null);
    }

    private void animateBionicEyeClose(Object event) {
        animateBionicEye(50f, 170f, event);
    }

    private void animateBionicEye(float startValue, float stopValue, final Object event) {
        ValueAnimator animator = ValueAnimator.ofFloat(startValue, stopValue);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                int animatedValue = (int) Util.convertDPToPixel(Objects.requireNonNull(getContext()), (float) valueAnimator.getAnimatedValue());

                RelativeLayout.LayoutParams layoutParamsHudLeft = (RelativeLayout.LayoutParams) ivHudLeft.getLayoutParams();
                layoutParamsHudLeft.setMarginStart(animatedValue);

                RelativeLayout.LayoutParams layoutParamsHudRight = (RelativeLayout.LayoutParams) ivHudRight.getLayoutParams();
                layoutParamsHudRight.setMarginEnd(animatedValue);

                ivHudLeft.requestLayout();
                ivHudRight.requestLayout();
            }
        });

        if (event != null) {
            animator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    EventBus.getDefault().post(event);
                }
            });
        }

        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);

                AudioPlayer.Track track = event == null ? AudioPlayer.Track.OPEN : AudioPlayer.Track.CLOSE;
                audioPlayer.play(track);
            }
        });

        animator.setDuration(500);
        animator.start();
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
