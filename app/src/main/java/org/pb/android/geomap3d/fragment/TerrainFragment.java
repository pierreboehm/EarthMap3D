package org.pb.android.geomap3d.fragment;

import android.location.Location;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.FragmentArg;
import org.androidannotations.annotations.SeekBarProgressChange;
import org.androidannotations.annotations.SeekBarTouchStop;
import org.androidannotations.annotations.Touch;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;
import org.androidannotations.annotations.sharedpreferences.Pref;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.pb.android.geomap3d.AppPreferences_;
import org.pb.android.geomap3d.R;
import org.pb.android.geomap3d.compass.Compass;
import org.pb.android.geomap3d.data.GeoDatabaseManager;
import org.pb.android.geomap3d.event.Events;
import org.pb.android.geomap3d.location.LocationManager;
import org.pb.android.geomap3d.util.Util;
import org.pb.android.geomap3d.view.OpenGLSurfaceView;
import org.pb.android.geomap3d.widget.Widget;

import java.util.Locale;

import static org.androidannotations.annotations.UiThread.Propagation.REUSE;

@EFragment(R.layout.fragment_terrain)
public class TerrainFragment extends Fragment {

    public static final String TAG = TerrainFragment.class.getSimpleName();

    @FragmentArg
    Widget widget;

    @Bean
    LocationManager locationManager;

    @Bean
    GeoDatabaseManager geoDatabaseManager;

    @Bean
    Compass compass;

    @Pref
    AppPreferences_ preferences;

    @ViewById(R.id.glSurfaceView)
    OpenGLSurfaceView openGLSurfaceView;

    @ViewById(R.id.shiftMenu)
    LinearLayout shiftMenu;

    @ViewById(R.id.shiftButton)
    ImageView shiftButton;

    @ViewById(R.id.tvDevicePosition)
    TextView tvDevicePosition;

    @ViewById(R.id.switchCompass)
    Switch switchCompass;

    @ViewById(R.id.switchAutomaticTrack)
    Switch switchAutomaticTrack;

    @ViewById(R.id.sbTrackDistance)
    SeekBar seekBarTrackDistance;

    @ViewById(R.id.tvTrackDistanceInMeters)
    TextView tvTrackDistanceInMeters;

    private float initialMotionY;
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

        EventBus.getDefault().register(this);
        locationManager.setLocationUpdateListener(getLocationUpdateListener());

        boolean useCompass = preferences.useCompass().getOr(true);
        switchCompass.setText(useCompass ? R.string.compassOnText : R.string.compassOffText);
        switchCompass.setChecked(useCompass);
        if (useCompass) {
            compass.start();
        }

        boolean trackPosition = preferences.trackPosition().getOr(true);
        switchAutomaticTrack.setText(trackPosition ? R.string.trackOnText : R.string.trackOffText);
        switchAutomaticTrack.setChecked(trackPosition);

        if (openGLSurfaceView != null) {
            openGLSurfaceView.setTrackDistance(preferences.defaultTrackDistanceInMeters().getOr(250));
        }

        EventBus.getDefault().post(new Events.FragmentLoaded(TAG));

//        shiftButton.setImageResource(R.drawable.shifter_menu_btn_up);
//        shiftMenu.animate()
//                .translationY(830f)
//                .setDuration(0)
//                .start();

//        Log.v(TAG, "shiftMenu.getTop() = " + shiftMenu.getTop());
    }

    @Override
    public void onPause() {
        locationManager.removeLocationUpdateListener();

        EventBus.getDefault().unregister(this);

        boolean useCompass = preferences.useCompass().getOr(true);
        if (useCompass) {
            compass.stop();
        }

        super.onPause();
    }

    @Subscribe(threadMode = ThreadMode.ASYNC, sticky = true)
    public void onEvent(Events.LocationUpdate event) {
        EventBus.getDefault().removeStickyEvent(event);
        Log.v(TAG, "collected locations: " + event.getLocations().size());

        for (Location location : event.getLocations()) {
            updateTrackedLocation(location);
        }
    }

    @UiThread(propagation = REUSE)
    void updateDeviceRotation(float azimuth) {
        if (openGLSurfaceView != null) {
            openGLSurfaceView.updateDeviceRotation(azimuth);
        }
    }

    @UiThread(propagation = REUSE)
    void updateDeviceLocation(Location location) {
        tvDevicePosition.setText(String.format(Locale.US, getString(R.string.locationDataFormat), location.getLatitude(), location.getLongitude()));
        if (openGLSurfaceView != null) {
            openGLSurfaceView.updateDeviceLocation(location);
        }
    }

    @UiThread(propagation = REUSE)
    void updateTrackedLocation(Location location) {
        if (openGLSurfaceView != null) {
            openGLSurfaceView.updateTrackedLocation(location);
        }
    }

    @Touch(R.id.shiftButton)
    public boolean onTouchShiftButton(View view, MotionEvent motionEvent) {
        boolean touched = false;

        switch (motionEvent.getAction()) {
            case MotionEvent.ACTION_DOWN:
                final float motionEventY = motionEvent.getY();
                if ((int) motionEventY == -1) {
                    return false;
                }

                initialMotionY = shiftMenu.getY() - motionEvent.getRawY();
                touched = true;
                break;

            case MotionEvent.ACTION_MOVE:
                float shiftYDelta = motionEvent.getRawY() + initialMotionY;
                if (shiftYDelta >= shiftMenu.getTop()) {
                    shiftMenu.animate()
                            .y(shiftYDelta)
                            .setDuration(0)
                            .start();
                }

                touched = true;
                break;

            case MotionEvent.ACTION_UP:
                float deltaShiftY = shiftMenu.getY() - shiftMenu.getTop();

                if (deltaShiftY < 500f) {
                    shiftButton.setImageResource(R.drawable.shifter_menu_btn_down);
                    shiftMenu.animate()
                            .translationYBy(-deltaShiftY)
                            .setDuration(0)
                            .start();
                } else {
                    shiftButton.setImageResource(R.drawable.shifter_menu_btn_up);
                    shiftMenu.animate()
                            .translationY(830f)
                            .setDuration(0)
                            .start();
                }

                touched = true;
                break;
        }

        return touched;
    }

    @Click(R.id.switchCompass)
    public void onSwitchCompassClicked() {
        boolean useCompass = switchCompass.isChecked();
        preferences.useCompass().put(useCompass);

        switchCompass.setText(useCompass ? R.string.compassOnText : R.string.compassOffText);
        if (useCompass) {
            compass.start();
        } else {
            compass.stop();
        }
    }

    @Click(R.id.switchAutomaticTrack)
    public void onSwitchAutomaticTrackClicked() {
        boolean isTrackOn = switchAutomaticTrack.isChecked();
        preferences.trackPosition().put(isTrackOn);

        seekBarTrackDistance.setEnabled(isTrackOn);
        switchAutomaticTrack.setText(isTrackOn ? R.string.trackOnText : R.string.trackOffText);
        openGLSurfaceView.setTrackDistance(isTrackOn ? preferences.defaultTrackDistanceInMeters().getOr(250) : 0);
    }

    @SeekBarProgressChange(R.id.sbTrackDistance)
    public void onTrackDistanceChange(SeekBar seekBar, int progress) {
        int value = Util.roundUp(progress, 50);
        tvTrackDistanceInMeters.setText(String.format(Locale.getDefault(), "%d m", value));
    }

    @SeekBarTouchStop(R.id.sbTrackDistance)
    public void onTrackDistanceChanged(SeekBar seekBar) {
        if (openGLSurfaceView != null) {
            int progressValue = Util.roundUp(seekBar.getProgress(), 50);
            preferences.defaultTrackDistanceInMeters().put(progressValue);
            openGLSurfaceView.setTrackDistance(progressValue);
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

    private LocationManager.LocationUpdateListener getLocationUpdateListener() {
        return new LocationManager.LocationUpdateListener() {
            @Override
            public void onLocationUpdate(Location location) {
                updateDeviceLocation(location);
            }
        };
    }
}
