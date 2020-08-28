package org.pb.android.geomap3d.fragment;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.res.Configuration;
import android.location.Location;
import android.util.Log;
import android.view.SurfaceView;

import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

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
import org.pb.android.geomap3d.data.PersistManager;
import org.pb.android.geomap3d.event.Events;
import org.pb.android.geomap3d.location.LocationManager;
import org.pb.android.geomap3d.util.Util;
import org.pb.android.geomap3d.view.BionicEyeView;

import java.util.Locale;
import java.util.Objects;

@EFragment(R.layout.fragment_bionic_eye)
public class BionicEyeFragment extends Fragment {

    public static final String TAG = BionicEyeFragment.class.getSimpleName();

    @ViewById(R.id.previewSurfaceView)
    SurfaceView previewSurfaceView;

    @ViewById(R.id.bionicEyeView)
    BionicEyeView bionicEyeView;

    @ViewById(R.id.tvAzimuth)
    TextView tvAzimuth;

    @ViewById(R.id.ivHudLeft)
    ImageView ivHudLeft;

    @ViewById(R.id.ivHudRight)
    ImageView ivHudRight;

    @ViewById(R.id.switchZoom)
    ImageButton ibSwitchZoom;

    @Bean
    CameraPreviewManager cameraPreviewManager;

    @Bean
    LocationManager locationManager;    // for getting current location

    @Bean
    PersistManager persistManager;      // for getting current GeoArea

    @Bean
    Compass compass;

    @Bean
    AudioPlayer audioPlayer;

    private Util.Orientation orientation;
    private boolean zoomActive = false;

    @AfterViews
    public void initViews() {
        compass.setListener(getCompassListener());
    }

    @Override
    public void onResume() {
        super.onResume();

        //setRetainInstance(true);
        EventBus.getDefault().register(this);

        orientation = Util.getOrientation(Objects.requireNonNull(getContext()));

        cameraPreviewManager.resume(previewSurfaceView);
        cameraPreviewManager.orientationChanged(orientation);

        locationManager.setLocationUpdateListener(getLocationUpdateListener());
        compass.start();
    }

    @Override
    public void onPause() {
        EventBus.getDefault().unregister(this);

        locationManager.removeLocationUpdateListener();
        compass.stop();
        audioPlayer.release();
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

    /*
    private float getGradientToNextRoutePointInDegrees() {
        if (lastKnownLocation == null) {
            return 0f;
        }

        Location routePointLocation = findNearestRoutePointLocation();
        if (routePointLocation == null) {
            return 0f;
        }

        GeoUtil.PositionOffsets currentLocationOffsets = GeoUtil.getPositionOffsets(lastKnownLocation, terrainGeoArea);
        GeoUtil.PositionOffsets routePointLocationOffsets = GeoUtil.getPositionOffsets(routePointLocation, terrainGeoArea);

        float gradient = (routePointLocationOffsets.zOffset - currentLocationOffsets.zOffset) / (routePointLocationOffsets.xOffset - currentLocationOffsets.xOffset);
        double degree = Math.toDegrees(Math.atan(gradient));

        return (float) Util.roundScale(degree);
    }

    private Location findNearestRoutePointLocation() {
        if (lastKnownLocation == null) {
            return null;
        }

        RouteLayer routePoint = null;
        float lastDistance = GeoUtil.RADIUS_OF_EARTH_IN_KILOMETER * 1000f;

        for (Layer layer : layers) {
            if (layer instanceof RouteLayer) {
                float distance = GeoUtil.getDistanceBetweenTwoPointsInMeter(((RouteLayer) layer).getLocation(), lastKnownLocation);
                if (distance < lastDistance) {
                    lastDistance = distance;
                    routePoint = (RouteLayer) layer;
                }
            }
        }

        return routePoint == null ? null : routePoint.getLocation();
    }
    */

    private Compass.CompassListener getCompassListener() {
        return new Compass.CompassListener() {
            @Override
            public void onRotationChanged(float azimuth, float pitch, float roll) {
                bionicEyeView.updateDeviceRotation(azimuth, pitch, roll);
                String azimuthText = String.format(Locale.getDefault(),"%d° %d° %d°", (int) azimuth, (int) pitch, (int) roll);
                tvAzimuth.setText(azimuthText);
            }
        };
    }

    private LocationManager.LocationUpdateListener getLocationUpdateListener() {
        return new LocationManager.LocationUpdateListener() {
            @Override
            public void onLocationUpdate(Location location) {
                bionicEyeView.updateDeviceLocation(location);
            }
        };
    }
}
