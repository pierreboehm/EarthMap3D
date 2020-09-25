package org.pb.android.geomap3d.widget.layer;

import android.view.MotionEvent;

import java.nio.FloatBuffer;

import javax.microedition.khronos.opengles.GL10;

public abstract class Layer {

    public abstract LayerType getLayerType();

    public abstract void draw(GL10 gl, FloatBuffer vertices, int numberOfPoints);

    public abstract void updateTouch(MotionEvent motionEvent, float xRotation, float yRotation);

    public enum LayerType {
        CDP(GLColor.WHITE),    // C(urrent) D(evice) P(osition)
        TDP(GLColor.GRAY),    // T(racked) D(evice) P(osition)
        CMP(GLColor.YELLOW),    // camp
//        POO(GLColor.GREEN),    // P(oint) O(f) O(bservation)

        POL(GLColor.BLUE),    // P(oint) O(f) L(air)  (also den, burrow, etc. location where animals sleep)
        ROB(GLColor.GREEN);     // Route point

        private final GLColor glColor;

        LayerType(GLColor glColor) {
            this.glColor = glColor;
        }
        public GLColor getGlColor() {
            return glColor;
        }

    }

    public enum GLColor {
        WHITE(1f, 1f, 1f),
        YELLOW(1f, .7f, 0f),
        GREEN(0f, 1f, 0f),
        BLUE(0f, 0f, 1f),
        GRAY(.7f, .7f, .7f);

        public final float red, green, blue;

        GLColor(float red, float green, float blue) {
            this.red = red;
            this.green = green;
            this.blue = blue;
        }
    }
}
