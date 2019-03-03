package org.pb.android.geomap3d.widget;

import android.location.Location;
import android.util.Log;
import android.view.MotionEvent;

import java.nio.FloatBuffer;

import javax.microedition.khronos.opengles.GL10;

public class PositionLayer extends Layer {

    private static final String TAG = PositionLayer.class.getSimpleName();

    private Location location;

    public PositionLayer(Location location) {
        this.location = location;
    }

    @Override
    public void draw(GL10 gl, FloatBuffer vertices, int numberOfPoints) {
        // use GL_TRIANGLES
    }

    @Override
    public void updateTouch(MotionEvent motionEvent, float xRotation, float yRotation) {

    }

    public void updateLocation(Location location) {
        this.location = location;
        Log.v(TAG, "new location: lat=" + location.getLatitude() + ", longitude=" + location.getLongitude());
    }
}
