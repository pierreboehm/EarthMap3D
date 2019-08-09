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
import android.widget.TextView;

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
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;

import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EViewGroup;
import org.androidannotations.annotations.ViewById;
import org.greenrobot.eventbus.EventBus;
import org.pb.android.geomap3d.R;
import org.pb.android.geomap3d.dialog.ConfirmDialog;
import org.pb.android.geomap3d.event.Events;
import org.pb.android.geomap3d.util.GeoUtil;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;

@EViewGroup(R.layout.map_view)
public class MapView extends FrameLayout implements OnMapReadyCallback, GoogleMap.OnMyLocationButtonClickListener, GoogleMap.OnPolygonClickListener {

    public static final String TAG = MapView.class.getSimpleName();
    public static final double MINIMUM_GEO_FENCE_SIZE_IN_METER = 8000;

    private static final int TOGGLE_WIDTH = 400;
    private static final int STANDARD_ZOOM_PADDING = 500;
    private static final float INITIAL_ZOOM = -1f;

    @ViewById(R.id.tvCenterOfMap)
    TextView tvCenterOfMap;

    @ViewById(R.id.tvCenterOfMapOutside)
    TextView tvCenterOfMapOutside;

    @ViewById(R.id.rectangleShape)
    View rectangleShape;

    protected GoogleMap googleMap;
    private LatLng currentLocation;
    private LatLngBounds mapBounds;

    private float currentZoom = INITIAL_ZOOM;
    private boolean initialZooming = true;

    private int areaCount = 0;
    private Map<String, Polygon> areas = new HashMap<>();

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
//        googleMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);

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

//                    Pair<Long, Long> rectAngleGeoSize = getRectangleGeoSize();
//                    Log.v(TAG, "cameraMove: currentZoom = " + currentZoom + ", W x H = " + rectAngleGeoSize.first + ", " + rectAngleGeoSize.second + " meters");

                    if (!initialZooming) {
                        adjustRectangle();
                    }
                }

                updateCenterOfMapView(cameraPosition.target);
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

//                Log.v(TAG, "onCameraIdle(): currentZoom = " + currentZoom);
            }
        });

        // TODO: shift up again if possible (if setMapType does not override custom style)
        try {
            boolean successfulStyled = this.googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(getContext(), R.raw.map_style_night));
            Log.v(TAG, "map styling " + (successfulStyled ? "success" : "failed"));
        } catch (Exception exception) {
            // not implemented
            Log.v(TAG, "map styling failed. (" + exception.getLocalizedMessage() + ")");
        }
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

    @Override
    public void onPolygonClick(final Polygon polygon) {
        new ConfirmDialog.Builder(getContext())
                .setMessage("Remove this area from map?")
                .setConfirmAction(new Runnable() {
                    @Override
                    public void run() {
                        String areaId = polygon.getTag().toString();
                        areas.get(areaId).remove();
                        areas.remove(areaId);
                    }
                })
                .build()
                .show();
    }

    @Click(R.id.rectangleShape)
    public void rectangleShapeClick() {
        if (googleMap == null) {
            return;
        }

        new ConfirmDialog.Builder(getContext())
                .setMessage("Load and add this area to map?")
                .setConfirmAction(new Runnable() {
                    @Override
                    public void run() {
                        LatLng targetLocation = googleMap.getCameraPosition().target;
                        Log.i(TAG, String.format(Locale.US, "Load high-map from: %.6f %.6f", targetLocation.latitude, targetLocation.longitude));
                        EventBus.getDefault().post(new Events.HeightMapLoadStart());
                        EventBus.getDefault().post(new Events.LoadHeightMap(targetLocation));
                    }
                })
                .build()
                .show();
    }

    public void resetToInitialState() {
        initialZooming = true;
    }

    public void updateLocation(Location location) {
        Log.v(TAG, "updateLocation(): " + location.toString());
        currentLocation = GeoUtil.getLatLngFromLocation(location);

        if (initialZooming && googleMap != null) {
            currentZoom = calculateMaximumZoomLevel(currentLocation);
            initialZooming = false;
            EventBus.getDefault().post(new Events.MapReadyEvent(location));
        }
    }

    public void addStoredArea(LatLng areaLocation) {
        LatLngBounds areaBounds = GeoUtil.getRectangleLatLngBounds(areaLocation, 8);

        LatLng northwest = new LatLng(areaBounds.northeast.latitude, areaBounds.southwest.longitude);
        LatLng southeast = new LatLng(areaBounds.southwest.latitude, areaBounds.northeast.longitude);

//        Log.i(TAG, "ne:" + areaBounds.northeast.toString() + ", se:" + southeast.toString() + ", sw:" + areaBounds.southwest.toString() + ", nw:" + northwest.toString());

        Polygon area = googleMap.addPolygon(new PolygonOptions()
                .clickable(true)
                .add(areaBounds.northeast)
                .add(southeast)
                .add(areaBounds.southwest)
                .add(northwest)
        );

        String areaId = "area#" + areaCount++;

        area.setStrokeWidth(2);
        area.setStrokeColor(getContext().getColor(R.color.warm_orange));
        area.setFillColor(getContext().getColor(R.color.warm_orange_with_alpha_35));
        area.setTag(areaId);

        areas.put(areaId, area);

        googleMap.setOnPolygonClickListener(this);
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

    private void updateCenterOfMapView(LatLng centerOfMap) {
        if (rectangleShape.getVisibility() != VISIBLE) {
            return;
        }

        String positionText = String.format(Locale.US, "%.06f\n%.06f", centerOfMap.latitude, centerOfMap.longitude);
        ViewGroup.LayoutParams layoutParams = rectangleShape.getLayoutParams();

        if (layoutParams.width > TOGGLE_WIDTH) {
            tvCenterOfMap.setText(positionText);
            tvCenterOfMap.setVisibility(VISIBLE);
            tvCenterOfMapOutside.setVisibility(GONE);
        } else {
            tvCenterOfMapOutside.setText(positionText);
            tvCenterOfMap.setVisibility(GONE);
            tvCenterOfMapOutside.setVisibility(VISIBLE);
        }
    }

    private LatLng snapToNearestArea(LatLng targetLocation) {

        if (areas.isEmpty()) {
            return targetLocation;
        }

        LatLng snapLocation = new LatLng(targetLocation.latitude, targetLocation.longitude);

        // loop through polygons
//        for (Polygon area : areas.values()) {
//            List<LatLng> points = area.getPoints();
//
//        }

        return snapLocation;
    }
}
