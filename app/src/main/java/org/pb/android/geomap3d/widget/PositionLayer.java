package org.pb.android.geomap3d.widget;

import android.view.MotionEvent;

import java.nio.FloatBuffer;

import javax.microedition.khronos.opengles.GL10;

public class PositionLayer extends Layer {

    @Override
    public void draw(GL10 gl, FloatBuffer vertices, int numberOfPoints) {
        // use GL_TRIANGLES
    }

    @Override
    public void updateTouch(MotionEvent motionEvent, float xRotation, float yRotation) {

    }
}
