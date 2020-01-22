package org.pb.android.geomap3d.widget.layer;

import android.view.MotionEvent;

import java.nio.FloatBuffer;

import javax.microedition.khronos.opengles.GL10;

public class RouteLayer extends Layer {

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
}
