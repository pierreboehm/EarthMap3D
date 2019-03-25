package org.pb.android.geomap3d.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.location.Location;
import android.os.Parcel;
import android.support.v4.util.Pair;
import android.util.Log;
import android.view.MotionEvent;

import org.greenrobot.eventbus.EventBus;
import org.pb.android.geomap3d.compass.LowPassFilter;
import org.pb.android.geomap3d.data.GeoModel;
import org.pb.android.geomap3d.event.Events;
import org.pb.android.geomap3d.renderer.RendererOpenGL;
import org.pb.android.geomap3d.util.GeoUtil;
import org.pb.android.geomap3d.util.Util;
import org.pb.android.geomap3d.widget.layer.Layer;
import org.pb.android.geomap3d.widget.layer.PositionLayer;
import org.pb.android.geomap3d.widget.layer.TerrainLayer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import javax.microedition.khronos.opengles.GL10;

import static org.pb.android.geomap3d.util.Util.roundScale;

@SuppressLint("ParcelCreator")
public class TerrainWidget extends Widget {

    public static final int BITMAP_DIMENSION = 1081;
    public static final double XZ_DIMENSION = 5.4;
    public static final double XZ_STRIDE = 0.01;

    private static final String TAG = TerrainWidget.class.getSimpleName();

    private Context context;
    private List<Layer> layers;

    private FloatBuffer vertices;
    private int numberOfPoints;

    private float xRotation;
    private float yRotation;

    private Util.PointF3D touch;
    private int lastKnownProgressValue = 0;

    private GeoModel terrainGeoModel;
    private Location lastKnownLocation;

    private int trackDistanceInMeters = 250;

    public TerrainWidget(Context context) {
        this.context = context;

        touch = new Util.PointF3D(0f, 0f, 0f);

        layers = new ArrayList<>();
        layers.add(new TerrainLayer());
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
                    if (GeoUtil.isLocationOnMap(((PositionLayer) layer).getLocation(), terrainGeoModel)) {
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
                    xRotation = (xRotation + diffY / 10f) % 360f;
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
    public void updateDeviceLocation(Location location) {
        PositionLayer positionLayer = getDevicePositionLayer();

        if (positionLayer == null) {
            positionLayer = new PositionLayer(location, Layer.LayerType.CDP);
            layers.add(positionLayer);
        }

        positionLayer.updateLocation(location, terrainGeoModel);

        if (location != null && trackDistanceInMeters > 0) {

            if (lastKnownLocation == null) {
                lastKnownLocation = location;
            }

            // FIXME: make distance-value modifiable
            if (GeoUtil.getDistanceBetweenTwoPointsInMeter(lastKnownLocation, location) > (float) trackDistanceInMeters) {
                // add new positionLayer from type TDP
                PositionLayer trackedDevicePositionLayer = new PositionLayer(lastKnownLocation, Layer.LayerType.TDP);
                trackedDevicePositionLayer.updateLocation(lastKnownLocation, terrainGeoModel);

                synchronized (this) {
                    layers.add(trackedDevicePositionLayer);
                }

                lastKnownLocation = location;
            }
        }
    }

    @Override
    public void updateTrackDistance(int trackDistanceInMeters) {
        this.trackDistanceInMeters = trackDistanceInMeters;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        // not implemented
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

    // FIXME: move to GeoUtil
    // vertical scale is 0 (black) to 1,024 (white) meters
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
            if (layer instanceof PositionLayer
                    && layer.getLayerType() == Layer.LayerType.CDP) {
                return (PositionLayer) layer;
            }
        }
        return null;
    }

    private class InitiationThread implements Runnable {

        InitiationThread(WidgetConfiguration widgetConfiguration) {

            if (widgetConfiguration.hasLocation()) {
                PositionLayer positionLayer = getDevicePositionLayer();

                if (positionLayer == null) {
                    layers.add(new PositionLayer(widgetConfiguration.getLocation(), Layer.LayerType.CDP));
                } else {
                    positionLayer.updateLocation(widgetConfiguration.getLocation(), widgetConfiguration.getGeoModel());
                }
            }

            terrainGeoModel = widgetConfiguration.getGeoModel();

            /*
            Location boxStartPoint = terrainGeoModel.getBoxStartPoint();
            Location boxEndPoint = terrainGeoModel.getBoxEndPoint();

            Location newBoxStartPoint = new Location("");
            newBoxStartPoint.setLatitude(boxStartPoint.getLatitude());
            newBoxStartPoint.setLongitude(boxEndPoint.getLongitude());

            Location newBoxEndPoint = new Location("");
            newBoxEndPoint.setLatitude(boxEndPoint.getLatitude());
            newBoxEndPoint.setLongitude(boxEndPoint.getLongitude() - GeoUtil.DELTA_LONGITUDE);

            String centerPoint = String.format(Locale.getDefault().US, "%.06f,%.06f", newBoxEndPoint.getLatitude() + GeoUtil.DELTA_LATITUDE / 2f, newBoxEndPoint.getLongitude() + GeoUtil.DELTA_LONGITUDE / 2f);
            Log.v(TAG, ">> " + centerPoint);
            */

            /*
            String link = String.format(Locale.getDefault().US,"http://terrain.party/api/export?name=kauffunger_wald_2&box=%.06f,%.06f,%.06f,%.06f",
                    newBoxStartPoint.getLongitude(), newBoxStartPoint.getLatitude(),
                    newBoxEndPoint.getLongitude(), newBoxEndPoint.getLatitude());

            Log.v(TAG, ">> " + link);
            */
        }

        @Override
        public void run() {
            Pair<Integer, FloatBuffer> layerInitResults = initLayer(terrainGeoModel.getHeightMapBitmap());
            numberOfPoints = layerInitResults.first;
            vertices = layerInitResults.second;

            yRotation = RendererOpenGL.ROTATION_INITIAL;
            xRotation = RendererOpenGL.ROTATION_INITIAL / 2f;
        }
    }
}
