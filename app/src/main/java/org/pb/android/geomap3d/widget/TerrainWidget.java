package org.pb.android.geomap3d.widget;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.location.Location;
import android.os.Parcel;
import android.util.Log;
import android.view.MotionEvent;

import org.greenrobot.eventbus.EventBus;
import org.pb.android.geomap3d.compass.LowPassFilter;
import org.pb.android.geomap3d.data.persist.geoarea.GeoArea;
import org.pb.android.geomap3d.data.route.model.Route;
import org.pb.android.geomap3d.data.route.model.RoutePoint;
import org.pb.android.geomap3d.event.Events;
import org.pb.android.geomap3d.renderer.RendererOpenGL;
import org.pb.android.geomap3d.util.GeoUtil;
import org.pb.android.geomap3d.util.Util;
import org.pb.android.geomap3d.widget.layer.Layer;
import org.pb.android.geomap3d.widget.layer.PositionLayer;
import org.pb.android.geomap3d.widget.layer.RouteLayer;
import org.pb.android.geomap3d.widget.layer.TerrainLayer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import javax.microedition.khronos.opengles.GL10;

import androidx.annotation.Nullable;
import androidx.core.util.Pair;

import static org.pb.android.geomap3d.util.Util.roundScale;

@SuppressLint("ParcelCreator")
public class TerrainWidget extends Widget {

    public static final int BITMAP_DIMENSION = 1081;
    public static final double XZ_DIMENSION = 5.4;
    public static final double XZ_STRIDE = 0.01;

    private static final String TAG = TerrainWidget.class.getSimpleName();

    private List<Layer> layers;

    private FloatBuffer vertices;
    private int numberOfPoints;

    private float xRotation;
    private float yRotation;

    private Util.PointF3D touch;
    private int lastKnownProgressValue = 0;

    private GeoArea terrainGeoArea;
    private Location lastKnownLocation;

    private boolean trackEnabled = true;
    private int trackDistanceInMeters = 250;

    public TerrainWidget() {
        touch = new Util.PointF3D(0f, 0f, 0f);
        initLayers();
    }

    @Override
    public synchronized void draw(GL10 gl) {
        gl.glPushMatrix();

        gl.glRotatef(xRotation, 1f, 0f, 0f);
        gl.glRotatef(yRotation, 0f, 1f, 0f);

        if (isInitialized()) {
            for (Layer layer : layers) {
                if (layer instanceof PositionLayer) {
                    // filter all positionLayer visible in this terrain
                    if (GeoUtil.isLocationOnMap(((PositionLayer) layer).getLocation(), terrainGeoArea) &&
                            (trackEnabled || layer.getLayerType() == Layer.LayerType.CDP)) {
                        layer.draw(gl, vertices, numberOfPoints);
                    }
                } else {
                    layer.draw(gl, vertices, numberOfPoints);
                }
            }
        }

        gl.glPopMatrix();
    }

    @Override
    public void initWidget(WidgetConfiguration widgetConfiguration) {
        new InitiationThread(widgetConfiguration).run();
    }

    @Override
    public void updateWidget(WidgetConfiguration widgetConfiguration) {
        initLayers();
        new InitiationThread(widgetConfiguration).run();
    }

    @Override
    public boolean isInitialized() {
        return vertices != null;
    }

    @Override
    public void updateTouch(MotionEvent motionEvent) {

        switch (motionEvent.getAction()) {

            case MotionEvent.ACTION_DOWN: {
                touch.x = motionEvent.getX();
                touch.y = motionEvent.getY();
                touch.z = 0f;

                break;
            }

            case MotionEvent.ACTION_MOVE: {
                Util.PointF3D currentTouch = new Util.PointF3D(motionEvent.getX(), motionEvent.getY(), 0f);

                float diffX = currentTouch.x - touch.x;
                float diffY = currentTouch.y - touch.y;

                if (Math.abs(diffX) > Math.abs(diffY)) {
                    yRotation = (yRotation + diffX / 10f) % 360f;
                } else if (Math.abs(diffY) > Math.abs(diffX)) {
                    float rotationX = (xRotation + diffY / 10f) % 360f;

                    // ignore: "Warning Can be replaced with 'Math.min' call"
                    if (rotationX < 0f) {
                        xRotation = 0f;
                    } else if (rotationX > 30f) {
                        xRotation = 30f;
                    } else {
                        xRotation = rotationX;
                    }
                }

                touch.x = motionEvent.getX();
                touch.y = motionEvent.getY();
                touch.z = 0f;

                break;
            }
        }

        for (Layer layer : layers) {
            layer.updateTouch(motionEvent, xRotation, yRotation);
        }
    }

    @Override
    public void updateDeviceRotation(float azimuth) {
        // FIXME: value sequence 359° --> 0° --> 359° produces a calculation error while smoothing. (modulo doesn't help)
        float smoothedAzimuth = LowPassFilter.filter(azimuth, yRotation, .005f);
        float roundScaledAzimuth = (float) roundScale(smoothedAzimuth);
//        Log.v(TAG, "roundScaledAzimuth = " + roundScaledAzimuth + "°");
        if (Math.abs(yRotation - roundScaledAzimuth) > .04f) {
            yRotation = roundScaledAzimuth;
            Log.v(TAG, "yRotation = " + yRotation + "°");
        }
    }

    @Override
    public void updateDeviceLocation(@Nullable Location location) {
        PositionLayer positionLayer = getDevicePositionLayer();

        if (positionLayer == null) {
            positionLayer = new PositionLayer(location, Layer.LayerType.CDP);
            layers.add(positionLayer);
        }

        positionLayer.updateLocation(location, terrainGeoArea);

        if (location != null && trackDistanceInMeters > 0) {

            if (lastKnownLocation == null) {
                lastKnownLocation = location;
            }

            // FIXME: make distance-value modifiable
            if (GeoUtil.getDistanceBetweenTwoPointsInMeter(lastKnownLocation, location) > (float) trackDistanceInMeters) {
                updateTrackedLocation(lastKnownLocation);
                lastKnownLocation = location;
            }
        }
    }

    @Override
    public void updateTrackedLocation(Location location) {
        PositionLayer trackedDevicePositionLayer = new PositionLayer(location, Layer.LayerType.TDP);
        trackedDevicePositionLayer.updateLocation(location, terrainGeoArea);

        synchronized (this) {
            layers.add(trackedDevicePositionLayer);
            Log.d(TAG, "tracked location added");
        }

        // TODO: persists tracked location (event?)
    }

    @Override
    public void updateTrackDistance(int trackDistanceInMeters) {
        this.trackDistanceInMeters = trackDistanceInMeters;
        if (trackDistanceInMeters == 0) {
            trackEnabled = false;
        }
        Log.d(TAG, "track distance changed --> " + trackDistanceInMeters + "m");
    }

    @Override
    public void updateTrackEnabled(boolean trackEnabled) {
        this.trackEnabled = trackEnabled;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        // not implemented
    }

    @Nullable
    public Location getLastKnownLocation() {
        return lastKnownLocation;
    }

    private void sendProgressUpdate(int currentCount, int maximalCount) {
        float progressValue = (float) currentCount * 100f / (float) maximalCount;
        if ((int) progressValue > lastKnownProgressValue) {
            lastKnownProgressValue = (int) progressValue;
            EventBus.getDefault().post(new Events.ProgressUpdate(progressValue));
        }
    }

    private List<Util.PointF3D> initPoints(Bitmap bitmap) {
        List<Util.PointF3D> points = new ArrayList<>();

        int currentCount = 0;
        int maximalCount = BITMAP_DIMENSION * BITMAP_DIMENSION;

        // Just create flat with 1081 x 1081 points. Values are based on generated elevation map.
        for (double xCoordinate = -XZ_DIMENSION; xCoordinate <= XZ_DIMENSION; xCoordinate = roundScale(xCoordinate + XZ_STRIDE)) {
            for (double zCoordinate = -XZ_DIMENSION; zCoordinate <= XZ_DIMENSION; zCoordinate = roundScale(zCoordinate + XZ_STRIDE)) {
                float elevationValue = getElevationValueFromLocation(bitmap, xCoordinate, zCoordinate);
                points.add(new Util.PointF3D((float) xCoordinate, elevationValue, (float) zCoordinate));
                sendProgressUpdate(++currentCount, maximalCount);
            }
        }

        return points;
    }

    private FloatBuffer initVertices(List<Util.PointF3D> points) {
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4 * 3 * points.size());
        byteBuffer.order(ByteOrder.nativeOrder());
        FloatBuffer vertices = byteBuffer.asFloatBuffer();

        for (Util.PointF3D point : points) {
            vertices.put(point.x);
            vertices.put(point.y);
            vertices.put(point.z);
        }

        vertices.position(0);
        return vertices;
    }

    private Pair<Integer, FloatBuffer> initLayer(Bitmap bitmap) {
        List<Util.PointF3D> points = initPoints(bitmap);
        return new Pair<>(points.size(), initVertices(points));
    }

    // NOTE: vertical scale is 0 (black) to 1,024 (white) meters
    // TODO: See comment in README of map-archive. Elevation calculation was adapted. May be, calculation must be adapted here.
    public static float getElevationValueFromLocation(Bitmap bitmap, double xCoordinate, double zCoordinate) {
        int xPosition = (int) roundScale((xCoordinate + XZ_DIMENSION) * 100);
        int zPosition = (int) roundScale((zCoordinate + XZ_DIMENSION) * 100);

        int color = bitmap.getPixel(xPosition, zPosition);      // 0 .. 255
        int rgbValue = Color.red(color);

        double yCoordinate = rgbValue * 4 * XZ_DIMENSION / 1024;
        return (float) yCoordinate / 4f;
    }

    private PositionLayer getDevicePositionLayer() {
        for (Layer layer : layers) {
            if (layer instanceof PositionLayer && layer.getLayerType() == Layer.LayerType.CDP) {
                return (PositionLayer) layer;
            }
        }
        return null;
    }

    private void initLayers() {
        lastKnownLocation = null;
        if (layers == null || layers.size() > 1) {
            layers = new ArrayList<>();
            layers.add(new TerrainLayer());
        }
    }

    private class InitiationThread implements Runnable {

        InitiationThread(WidgetConfiguration widgetConfiguration) {
            terrainGeoArea = widgetConfiguration.getGeoArea();

            if (widgetConfiguration.hasLocation()) {
                updateDeviceLocation(widgetConfiguration.getLocation());
            }

            if (widgetConfiguration.hasRoute()) {
                Route route = widgetConfiguration.getRoute();

                for (RoutePoint routePoint : route.getRoutePointList()) {
                    Location location = GeoUtil.getLocation(routePoint.getLatitude(), routePoint.getLongitude());
                    RouteLayer routeLayer = new RouteLayer(location, terrainGeoArea);

                    if (routeLayer.isVisible()) {
                        layers.add(routeLayer);
                    }
                }
            }
        }

        @Override
        public void run() {
            Pair<Integer, FloatBuffer> layerInitResults = initLayer(terrainGeoArea.getHeightMapBitmap());
            //noinspection ConstantConditions
            numberOfPoints = layerInitResults.first;
            vertices = layerInitResults.second;

            yRotation = RendererOpenGL.ROTATION_INITIAL;
            xRotation = RendererOpenGL.ROTATION_INITIAL / 2f;

            EventBus.getDefault().postSticky(new Events.WidgetReady());
        }
    }
}
