package org.pb.android.geomap3d.widget.layer;

import android.view.MotionEvent;

import org.pb.android.geomap3d.util.Util;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import javax.microedition.khronos.opengles.GL10;

public class RouteLayer extends Layer {

    private List<Util.PointF3D> points = new ArrayList<>();
    private FloatBuffer vertices;

    public RouteLayer() {
        initLayer();
    }

    @Override
    public LayerType getLayerType() {
        return LayerType.ROB;
    }

    @Override
    public void draw(GL10 gl, FloatBuffer vertices, int numberOfPoints) {
        // TODO
    }

    @Override
    public void updateTouch(MotionEvent motionEvent, float xRotation, float yRotation) {
        // not implemented
    }

    private void initLayer() {
        // TODO
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
}
