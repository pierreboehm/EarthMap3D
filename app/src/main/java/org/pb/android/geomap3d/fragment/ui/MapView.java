package org.pb.android.geomap3d.fragment.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;

import org.androidannotations.annotations.EViewGroup;
import org.pb.android.geomap3d.R;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;


@EViewGroup(R.layout.map_view)
public class MapView extends FrameLayout implements OnMapReadyCallback {

    protected GoogleMap googleMap;

    public MapView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        //adding alpha animation on start up
        setAlpha(0);
        animate().alpha(1.0f).setDuration(500)
                .setInterpolator(new DecelerateInterpolator())
                .start();

        addMapFragmentToContainer();
    }

    @Override
    protected void onDetachedFromWindow() {
//        locationManager.removeLocationChangedListener(this);
        super.onDetachedFromWindow();
    }

    /**
     * MapFragment can be nested in fragments, but has to be removed
     * if app goes to background. That's why we remove the map in onPause()
     * and add it back in onStart()
     */
    private void addMapFragmentToContainer() {
        FragmentManager fragmentManager = ((AppCompatActivity) getContext()).getSupportFragmentManager();
        SupportMapFragment mapFragment = SupportMapFragment.newInstance();
        fragmentManager.beginTransaction()
                .replace(R.id.mapContainer, mapFragment)
                .commit();
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.googleMap = googleMap;

        UiSettings uiSettings = googleMap.getUiSettings();
        uiSettings.setIndoorLevelPickerEnabled(false);
        uiSettings.setZoomControlsEnabled(false);
        uiSettings.setMapToolbarEnabled(false);
        uiSettings.setMyLocationButtonEnabled(false);
//        updateMapPreferencesIfNecessary();

//        googleMap.setOnMarkerClickListener(this);
        googleMap.setIndoorEnabled(false);

//        onMainMapReady();

//        if (hasCallback()) {
//            callback.onMapViewReady();
//        }

//        if (ActivityCompat.checkSelfPermission(getContext(), ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//            requestAccessFineLocationPermission(false);
//        } else {
//            startLocationListener();
//        }
    }

}
