package org.pb.android.geomap3d.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.support.v4.util.Pair;
import android.view.MotionEvent;

import org.greenrobot.eventbus.EventBus;
import org.pb.android.geomap3d.R;
import org.pb.android.geomap3d.compass.LowPassFilter;
import org.pb.android.geomap3d.event.Events;
import org.pb.android.geomap3d.renderer.RendererOpenGL;
import org.pb.android.geomap3d.util.Util;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import javax.microedition.khronos.opengles.GL10;

import static org.pb.android.geomap3d.util.Util.roundScale;

public class TerrainWidget extends Widget {

    private static final String TAG = TerrainWidget.class.getSimpleName();

    private Bitmap bitmap;
    private List<Layer> layers;

    private FloatBuffer vertices;
    private int numberOfPoints;

    private float xRotation;
    private float yRotation;

    private Util.PointF3D touch;
    private int lastKnownProgressValue = 0;

    public TerrainWidget(Context context) {
        touch = new Util.PointF3D(0f, 0f, 0f);

        Bitmap rawmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.kaufunger_wald_height_map);
        bitmap = Bitmap.createScaledBitmap(rawmap, 1081, 1081, true);

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
                layer.draw(gl, vertices, numberOfPoints);
            }
        }

        gl.glPopMatrix();
    }

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

            // DON'T FIXME: keep that position as kind of a priority handling
            case MotionEvent.ACTION_UP: {
                // deactivate layer (should translate layer from its end position back to center)
                break;
            }
        }

        for (Layer layer : layers) {
            layer.updateTouch(motionEvent, xRotation, yRotation);
        }
    }

    public void updateDeviceRotation(float azimuth) {
        float smoothedAzimuth = LowPassFilter.filter(azimuth, yRotation, .005f);
        float roundScaledAzimuth = (float) roundScale(smoothedAzimuth);
        if (Math.abs(yRotation - roundScaledAzimuth) > .04f) {
            yRotation = roundScaledAzimuth;
        }
    }

    @Override
    public void initWidget(WidgetConfiguration widgetConfiguration) {
        new InitiationThread().run();

//        Pair<Integer, FloatBuffer> layerInitResults = initLayer();
//        numberOfPoints = layerInitResults.first;
//        vertices = layerInitResults.second;
//
//        yRotation = RendererOpenGL.ROTATION_INITIAL;
//        xRotation = RendererOpenGL.ROTATION_INITIAL;
//
//        EventBus.getDefault().post(new Events.WidgetReady());
    }

    @Override
    public boolean isInitialized() {
        return vertices != null;
    }

    private void sendProgressUpdate(int currentCount, int maximalCount) {
        float progressValue = (float) currentCount * 100f / (float) maximalCount;
        if ((int) progressValue > lastKnownProgressValue) {
            lastKnownProgressValue = (int) progressValue;
            EventBus.getDefault().post(new Events.ProgressUpdate(progressValue));
        }
    }

    private class InitiationThread implements Runnable {
        @Override
        public void run() {
            Pair<Integer, FloatBuffer> layerInitResults = initLayer();
            numberOfPoints = layerInitResults.first;
            vertices = layerInitResults.second;

            yRotation = RendererOpenGL.ROTATION_INITIAL;
            xRotation = RendererOpenGL.ROTATION_INITIAL;

            EventBus.getDefault().post(new Events.WidgetReady());
        }
    }

    private List<Util.PointF3D> initPoints() {
        List<Util.PointF3D> points = new ArrayList<>();

        int currentCount = 0;
        int maximalCount = 1081 * 1081;

        // Just create flat with 1081 x 1081 points. Values are based on generated elevation map.
        for (double xCoordinate = -5.4; xCoordinate <= 5.4; xCoordinate = roundScale(xCoordinate + 0.01)) {
            for (double zCoordinate = -5.4; zCoordinate <= 5.4; zCoordinate = roundScale(zCoordinate + 0.01)) {
                float elevationValue = getElevationValueFromLocation(xCoordinate, zCoordinate);
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

    private Pair<Integer, FloatBuffer> initLayer() {
        List<Util.PointF3D> points = initPoints();
        return new Pair<>(points.size(), initVertices(points));
    }

    // vertical scale is 0 (black) to 1,024 (white) meters
    private float getElevationValueFromLocation(double xCoordinate, double zCoordinate) {
        int xPosition = (int) roundScale((xCoordinate + 5.4) * 100);
        int zPosition = (int) roundScale((zCoordinate + 5.4) * 100);

        int color = bitmap.getPixel(xPosition, zPosition);      // 0 .. 255
        int rgbValue = Color.red(color);

        double yCoordinate = rgbValue * 4 * 5.4 / 1024;
        return (float) yCoordinate / 4f;
    }
}
