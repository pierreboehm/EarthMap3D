package org.pb.android.geomap3d.widget;

import android.view.MotionEvent;
import java.nio.FloatBuffer;
import javax.microedition.khronos.opengles.GL10;

public class TerrainLayer extends Layer {

    @Override
    public void draw(GL10 gl, FloatBuffer vertices, int numberOfPoints) {
        gl.glPointSize(1f);
        gl.glVertexPointer(3, GL10.GL_FLOAT, 0, vertices);
        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
//        gl.glEnable(GL10.GL_BLEND);
//        gl.glBlendFunc(GLES10.GL_SRC_ALPHA, GLES10.GL_ONE_MINUS_SRC_ALPHA);
        gl.glColor4f(0f, .6f, .8f, .5f);
        gl.glDrawArrays(GL10.GL_POINTS, 0, numberOfPoints);
//        gl.glDisable(GL10.GL_BLEND);
        gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
    }

    @Override
    public void updateTouch(MotionEvent motionEvent, float xRotation, float yRotation) {
        // not implemented
    }

}
