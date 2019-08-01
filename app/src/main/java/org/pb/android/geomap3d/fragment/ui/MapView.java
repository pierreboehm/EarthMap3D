package org.pb.android.geomap3d.fragment.ui;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.location.Location;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EViewGroup;
import org.androidannotations.annotations.ViewById;
import org.greenrobot.eventbus.EventBus;
import org.pb.android.geomap3d.R;
import org.pb.android.geomap3d.event.Events;
import org.pb.android.geomap3d.util.GeoUtil;

import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;

@EViewGroup(R.layout.map_view)
public class MapView extends FrameLayout implements OnMapReadyCallback, GoogleMap.OnMyLocationButtonClickListener {

    public static final String TAG = MapView.class.getSimpleName();
    public static final double MINIMUM_GEO_FENCE_SIZE_IN_METER = 8000;

    private static final int STANDARD_ZOOM_PADDING = 50;
    private static final float STANDARD_ZOOM_LEVEL = 8f;   // relates to 11km side length
    private static final float INITIAL_ZOOM = -1f;

    @ViewById(R.id.rectangleShape)
    View rectangleShape;

    protected GoogleMap googleMap;
    private LatLng currentLocation;
    private LatLngBounds mapBounds;

    private float currentZoom = INITIAL_ZOOM;
    private boolean initialZooming = true;

    public MapView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        //adding alpha animation on start up
        setAlpha(0);
        animate().alpha(1.0f).setDuration(500).setInterpolator(new DecelerateInterpolator()).start();
        addMapFragmentToContainer();
    }

    @Override
    protected void onDetachedFromWindow() {
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
        fragmentManager.beginTransaction().replace(R.id.mapContainer, mapFragment).commit();
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(final GoogleMap googleMap) {
        this.googleMap = googleMap;

        UiSettings uiSettings = googleMap.getUiSettings();
        uiSettings.setIndoorLevelPickerEnabled(true);
        uiSettings.setZoomControlsEnabled(true);
        uiSettings.setZoomGesturesEnabled(true);
        uiSettings.setMapToolbarEnabled(true);
        uiSettings.setMyLocationButtonEnabled(true);
        uiSettings.setCompassEnabled(true);

        googleMap.setIndoorEnabled(false);
        googleMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);

        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            googleMap.setMyLocationEnabled(true);
            googleMap.setOnMyLocationButtonClickListener(this);
        }

        googleMap.setOnCameraMoveListener(new GoogleMap.OnCameraMoveListener() {
            @Override
            public void onCameraMove() {
                //check zoom level and show text label
                CameraPosition cameraPosition = googleMap.getCameraPosition();

                if (cameraPosition.zoom != currentZoom) {
                    currentZoom = cameraPosition.zoom;
                    rectangleShape.setVisibility(VISIBLE);

                    Pair<Long, Long> rectAngleGeoSize = getRectangleGeoSize();
                    Log.v(TAG, "cameraMove: currentZoom = " + currentZoom + ", W x H = " + rectAngleGeoSize.first + ", " + rectAngleGeoSize.second + " meters");

                    if (!initialZooming) {
                        adjustRectangle();
                    }
                }
            }
        });

        googleMap.setOnCameraIdleListener(new GoogleMap.OnCameraIdleListener() {
            @Override
            public void onCameraIdle() {
                CameraPosition cameraPosition = googleMap.getCameraPosition();
                currentZoom = cameraPosition.zoom;
//                if (currentZoom > STANDARD_ZOOM_LEVEL) {
//                    googleMap.setMaxZoomPreference(currentZoom);
//                }

                Log.v(TAG, "onCameraIdle(): currentZoom = " + currentZoom);
            }
        });
    }

    @Override
    public boolean onMyLocationButtonClick() {
        if (googleMap != null && currentLocation != null) {
            currentZoom = calculateMaximumZoomLevel(currentLocation);
            initialZooming = false;
            return true;
        }
        return false;
    }

    @Click(R.id.rectangleShape)
    public void rectangleShapeClick() {
        if (googleMap == null) {
            return;
        }

        LatLng targetLocation = googleMap.getCameraPosition().target;
//        Toast.makeText(getContext(), String.format(Locale.US, "Load high-map from: %.6f %.6f", targetLocation.latitude, targetLocation.longitude), Toast.LENGTH_SHORT).show();

        EventBus.getDefault().post(new Events.LoadHeightMap(targetLocation));
        // TODO: show loading icon (top-left?)
    }

    public void updateLocation(Location location) {
        Log.v(TAG, "updateLocation(): " + location.toString());
        currentLocation = GeoUtil.getLatLngFromLocation(location);

        if (initialZooming && googleMap != null) {
            currentZoom = calculateMaximumZoomLevel(currentLocation);
            initialZooming = false;
        }
    }

    public void updateMapAfterHeightMapLoaded() {
        // TODO: hide loading icon again
    }

    private void zoomToBoundary(LatLngBounds boundary, boolean animate, int paddingOfBoundary) {
        zoomToBoundary(boundary, animate, getWidth(), getHeight(), paddingOfBoundary);
    }

    private void zoomToBoundary(LatLngBounds boundary, boolean animate, int width, int height, int paddingOfBoundary) {
        if (googleMap == null) {
            return;
        }

        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngBounds(boundary, width, height, paddingOfBoundary);
        if (animate) {
            googleMap.animateCamera(cameraUpdate);
        } else {
            googleMap.moveCamera(cameraUpdate);
        }
    }

    @VisibleForTesting
    private boolean isBoundaryInCurrentMapProjection(LatLngBounds boundary) {
        if (googleMap == null || googleMap.getProjection() == null) {
            Log.e(TAG, "Checking Boundary on google map null");
            return false;
        }
        LatLngBounds visibleBoundary = googleMap.getProjection().getVisibleRegion().latLngBounds;
        return visibleBoundary.contains(boundary.northeast) && visibleBoundary.contains(boundary.southwest);
    }

    @VisibleForTesting
    private Pair<Long, Long> getRectangleGeoSize() {
        int halfHeight = ((int) rectangleShape.getY() + rectangleShape.getHeight() / 2);
        int halfWidth = (int) rectangleShape.getX() + rectangleShape.getWidth() / 2;

        Point pointLeftToCenter = new Point(rectangleShape.getLeft(), halfHeight);
        Point pointRightToCenter = new Point(rectangleShape.getRight(), halfHeight);
        Point pointTopOfCenter = new Point(halfWidth, rectangleShape.getTop());
        Point pointBottomOfCenter = new Point(halfWidth, rectangleShape.getBottom());

        Projection projection = googleMap.getProjection();
        LatLng eastLatLng = projection.fromScreenLocation(pointRightToCenter);
        LatLng westLatLng = projection.fromScreenLocation(pointLeftToCenter);
        LatLng northLatLng = projection.fromScreenLocation(pointTopOfCenter);
        LatLng southLatLng = projection.fromScreenLocation(pointBottomOfCenter);

        long width = (long) GeoUtil.getDistanceBetweenTwoPointsInMeter(westLatLng, eastLatLng);
        long height = (long) GeoUtil.getDistanceBetweenTwoPointsInMeter(northLatLng, southLatLng);

        if (width < MINIMUM_GEO_FENCE_SIZE_IN_METER) {
            width = (long) MINIMUM_GEO_FENCE_SIZE_IN_METER;
        }
        if (height < MINIMUM_GEO_FENCE_SIZE_IN_METER) {
            height = (long) MINIMUM_GEO_FENCE_SIZE_IN_METER;
        }

        return new Pair<>(width, height);
    }

    private float calculateMaximumZoomLevel(LatLng center) {
        double halfOffSetInKilometer = MINIMUM_GEO_FENCE_SIZE_IN_METER / 2 / 1000;

        LatLng centerNorth = GeoUtil.computeOffset(center, halfOffSetInKilometer, 0);
        LatLng centerEast = GeoUtil.computeOffset(center, halfOffSetInKilometer, 90);
        LatLng centerSouth = GeoUtil.computeOffset(center, halfOffSetInKilometer, 180);
        LatLng centerWest = GeoUtil.computeOffset(center, halfOffSetInKilometer, 270);

        mapBounds = LatLngBounds.builder().
                include(centerNorth).
                include(centerEast).
                include(centerSouth).
                include(centerWest).build();

        zoomToBoundary(mapBounds, true, STANDARD_ZOOM_PADDING);

        return googleMap.getCameraPosition().zoom;
    }

    private void adjustRectangle() {
        Point northEast = googleMap.getProjection().toScreenLocation(mapBounds.northeast);
        Point southWest = googleMap.getProjection().toScreenLocation(mapBounds.southwest);

        int areaWidth = northEast.x - southWest.x;
        int areaHeight = southWest.y - northEast.y;

        ViewGroup.LayoutParams layoutParams = rectangleShape.getLayoutParams();
        layoutParams.width = areaWidth;
        layoutParams.height = areaHeight;
        rectangleShape.setLayoutParams(layoutParams);
    }
}
