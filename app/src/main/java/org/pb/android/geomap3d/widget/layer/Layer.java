package org.pb.android.geomap3d.widget.layer;

import android.view.MotionEvent;

import java.nio.FloatBuffer;

import javax.microedition.khronos.opengles.GL10;

public abstract class Layer {

    public abstract void draw(GL10 gl, FloatBuffer vertices, int numberOfPoints);

    public abstract void updateTouch(MotionEvent motionEvent, float xRotation, float yRotation);

}
