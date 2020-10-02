package org.pb.android.geomap3d.widget.layer;

import android.graphics.Bitmap;
import android.util.Log;
import android.view.MotionEvent;

import org.pb.android.geomap3d.util.Util;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.microedition.khronos.opengles.GL10;

import static org.pb.android.geomap3d.util.Util.roundScale;
import static org.pb.android.geomap3d.widget.TerrainWidget.XZ_DIMENSION;
import static org.pb.android.geomap3d.widget.TerrainWidget.XZ_STRIDE;
import static org.pb.android.geomap3d.widget.TerrainWidget.getElevationValueFromLocation;

public class TerrainLayer extends Layer {

    private static final String TAG = TerrainLayer.class.getSimpleName();

    private boolean hasColoredMap = false;

    private FloatBuffer vertices;
    private int numberOfPoints;

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
        // TODO: loop through color layer map and draw each of them
    }

    @Override
    public void updateTouch(MotionEvent motionEvent, float xRotation, float yRotation) {
        // not implemented
    }

    public boolean isInitialized() {
        return vertices != null;
    }

    public void initLayer(Bitmap heightBitmap) {
        List<Util.PointF3D> points = initPoints(heightBitmap);
        vertices = initVertices(points);
        numberOfPoints = points.size();

        //new ColoringPointsThread(points).run();
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

    private FloatBuffer initVertices(List<Util.PointF3D> points) {
        // create uni-colored topology map until colored map is ready ...
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

    private class ColoringPointsThread implements Runnable {

        final List<Util.PointF3D> points3D;

        ColoringPointsThread(List<Util.PointF3D> points3D) {
            this.points3D = points3D;
        }

        @Override
        public void run() {
            SortedMap<Float, List<Util.PointF3D>> pointsLayerMap = new TreeMap<>();

            for (Util.PointF3D point : points3D) {
                float layerKey = point.y;

                if (pointsLayerMap.containsKey(layerKey)) {
                    List<Util.PointF3D> layerValue = pointsLayerMap.get(layerKey);
                    layerValue.add(point);
                    pointsLayerMap.put(layerKey, layerValue);
                } else {
                    pointsLayerMap.put(layerKey, new ArrayList<>(Arrays.asList(point)));
                }
            }

            Log.i(TAG, "Colored points map is pre-filtered now.");

            hasColoredMap = /*true*/false;
        }
    }
}
