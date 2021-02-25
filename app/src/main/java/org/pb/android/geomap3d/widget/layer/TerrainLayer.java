package org.pb.android.geomap3d.widget.layer;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Pair;
import android.view.MotionEvent;

import org.pb.android.geomap3d.util.Util;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.microedition.khronos.opengles.GL10;

import static org.pb.android.geomap3d.util.Util.roundScale;
import static org.pb.android.geomap3d.widget.TerrainWidget.XZ_DIMENSION;
import static org.pb.android.geomap3d.widget.TerrainWidget.XZ_STRIDE;
import static org.pb.android.geomap3d.widget.TerrainWidget.getElevationValueFromLocation;

public class TerrainLayer extends Layer {

    private static final String TAG = TerrainLayer.class.getSimpleName();

    private HandlerThread backgroundHandlerThread;
    private Handler backgroundHandler;

    private Map<Integer, Pair<Integer, FloatBuffer>> coloredVerticesMap;

    private boolean hasColoredMap = false;

    private FloatBuffer vertices;
    private int numberOfPoints;

    private int elevationValue = -1;

    @Override
    public LayerType getLayerType() {
        return null;
    }

    @Override
    public void draw(GL10 gl, FloatBuffer p1, int p2) {

        gl.glPointSize(1f);
        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);

        if (hasColoredMap) {
            drawColoredMap(gl);
        } else {
            gl.glColor4f(0f, .6f, .8f, .5f);
            gl.glVertexPointer(3, GL10.GL_FLOAT, 0, vertices);
            gl.glDrawArrays(GL10.GL_POINTS, 0, numberOfPoints);
        }

        gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
    }

    private void drawColoredMap(GL10 gl) {
        for (Map.Entry<Integer, Pair<Integer, FloatBuffer>> entry : coloredVerticesMap.entrySet()) {
            int layerKey = entry.getKey();

            if (elevationValue > -1 && (layerKey >= elevationValue - 1 && layerKey <= elevationValue + 1)) {
                gl.glColor4f(.56f, .93f, .56f, 0f);
            } else {
                float color4fBlue = (float) entry.getKey() / 255f;
                gl.glColor4f(0f, .5f, color4fBlue, 0f);
            }

            gl.glVertexPointer(3, GL10.GL_FLOAT, 0, entry.getValue().second);
            gl.glDrawArrays(GL10.GL_POINTS, 0, entry.getValue().first);
        }
    }

    @Override
    public void updateTouch(MotionEvent motionEvent, float xRotation, float yRotation) {
        // not implemented
    }

    public void setCurrentElevationValue(int elevationValue) {
        this.elevationValue = elevationValue;
    }

    public boolean isInitialized() {
        return vertices != null;
    }

    public void initLayer(Bitmap heightBitmap) {
        List<Util.PointF3D> points = initPoints(heightBitmap);
        vertices = initVertices(points);
        numberOfPoints = points.size();

        initBackgroundThread();
        backgroundHandler.post(new ColoringPointsThread(points));
    }

    private List<Util.PointF3D> initPoints(Bitmap bitmap) {
        List<Util.PointF3D> points = new ArrayList<>();

        // Just create flat with 1081 x 1081 points. Values are based on generated elevation map.
        for (double xCoordinate = -XZ_DIMENSION; xCoordinate <= XZ_DIMENSION; xCoordinate = roundScale(xCoordinate + XZ_STRIDE)) {
            for (double zCoordinate = -XZ_DIMENSION; zCoordinate <= XZ_DIMENSION; zCoordinate = roundScale(zCoordinate + XZ_STRIDE)) {
                float elevationValue = getElevationValueFromLocation(bitmap, xCoordinate, zCoordinate);
                points.add(new Util.PointF3D((float) xCoordinate, elevationValue, (float) zCoordinate));
            }
        }

        return points;
    }

    private void initBackgroundThread() {
        if (backgroundHandlerThread == null) {
            backgroundHandlerThread = new HandlerThread("background");
            backgroundHandlerThread.start();
        }

        if (backgroundHandler == null) {
            backgroundHandler = new Handler(backgroundHandlerThread.getLooper());
        }
    }

    private void quitBackgroundThread() {
        // Finish processing posted messages, then join on the handling thread
        backgroundHandlerThread.quitSafely();
        try {
            backgroundHandlerThread.join();
            Log.d(TAG, "background handler thread closed");
        } catch (InterruptedException ex) {
            // implement
        } finally {
            backgroundHandlerThread = null;
            backgroundHandler = null;
        }
    }

    private class ColoringPointsThread implements Runnable {

        final List<Util.PointF3D> points3D;

        ColoringPointsThread(List<Util.PointF3D> points3D) {
            this.points3D = points3D;
        }

        @Override
        public void run() {
            SortedMap<Integer, List<Util.PointF3D>> pointsLayerMap = new TreeMap<>();

            for (Util.PointF3D point : points3D) {
                int layerKey = (int) roundScale (point.y * 4 * 1024 / XZ_DIMENSION / 4);

                if (pointsLayerMap.containsKey(layerKey)) {
                    List<Util.PointF3D> layerValue = pointsLayerMap.get(layerKey);
                    layerValue.add(point);
                    pointsLayerMap.put(layerKey, layerValue);
                } else {
                    pointsLayerMap.put(layerKey, new ArrayList<>(Arrays.asList(point)));
                }
            }

            Log.i(TAG, "Colored points map is pre-filtered now.");

            coloredVerticesMap = new HashMap<>();

            for (Map.Entry<Integer, List<Util.PointF3D>> entry : pointsLayerMap.entrySet()) {
                FloatBuffer vertices = initVertices(entry.getValue());
                int numberOfPoints = entry.getValue().size();
                coloredVerticesMap.put(entry.getKey(), new Pair<>(numberOfPoints, vertices));
            }

            Log.i(TAG, "Colored vertices map is ready for use.");

            hasColoredMap = true;
            quitBackgroundThread();
        }
    }
}
