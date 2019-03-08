package org.pb.android.geomap3d.widget.layer;

import android.view.MotionEvent;

import java.nio.FloatBuffer;

import javax.microedition.khronos.opengles.GL10;

public abstract class Layer {

    // TODO: create complex enum that also contains pin-color

    public enum LayerType {
        CDP,    // C(urrent) D(evice) P(osition)        --> white pin with animated ring at bottom
        TDP,    // T(racked) D(evice) P(osition)        --> white pin
        CMP,    // camp                                 --> yellow pin
        POO,    // P(oint) O(f) O(bservation)           --> green pin
        POL,    // P(oint) O(f) L(air)  (also den, burrow, etc. location where animals sleep)   --> blue pin
    }

    public abstract void draw(GL10 gl, FloatBuffer vertices, int numberOfPoints);

    public abstract void updateTouch(MotionEvent motionEvent, float xRotation, float yRotation);

}
