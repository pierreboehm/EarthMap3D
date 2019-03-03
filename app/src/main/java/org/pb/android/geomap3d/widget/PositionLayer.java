package org.pb.android.geomap3d.widget;

import android.location.Location;
import android.util.Log;
import android.view.MotionEvent;

import org.pb.android.geomap3d.util.GeoUtil;
import org.pb.android.geomap3d.util.Util;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import javax.microedition.khronos.opengles.GL10;

public class PositionLayer extends Layer {

    private static final String TAG = PositionLayer.class.getSimpleName();

    private Location location;

    private float positionYOffset = 0f;
    private float positionXOffset = 0.3f;
    private float positionZOffset = 0.2f;

    private List<Util.PointF3D> points = new ArrayList<>();
    private FloatBuffer vertices;
    private int numberOfPoints;

    public PositionLayer(Location location) {
        this.location = location;
        initLayer();
    }

    @Override
    public void draw(GL10 gl, FloatBuffer p1, int p2) {
        // use GL_TRIANGLES

        gl.glPushMatrix();

            gl.glTranslatef(positionXOffset, positionYOffset, positionZOffset);

            gl.glPointSize(9f);
            gl.glVertexPointer(3, GL10.GL_FLOAT, 0, vertices);
            gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
            gl.glColor4f(1f, 1f, 1f, 1f);
    //        gl.glEnable(GL10.GL_BLEND);
    //        gl.glBlendFunc(GLES10.GL_SRC_ALPHA, GLES10.GL_ONE_MINUS_SRC_ALPHA);
            gl.glDrawArrays(GL10.GL_TRIANGLES, 0, numberOfPoints);
    //        gl.glDisable(GL10.GL_BLEND);
            gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);

        gl.glPopMatrix();
    }

    @Override
    public void updateTouch(MotionEvent motionEvent, float xRotation, float yRotation) {
        // not implemented
    }

    public void updateLocation(Location location) {
        this.location = location;

        positionYOffset = GeoUtil.getHeightAtPosition(location);

        Log.v(TAG, "new location: lat=" + location.getLatitude() + ", longitude=" + location.getLongitude());
    }

    private void initLayer() {
        points = new ArrayList<>();

        points.add(new Util.PointF3D(-0.025f, 0.05f, 0f));
        points.add(new Util.PointF3D( 0.025f, 0.05f, 0f));
        points.add(new Util.PointF3D(     0f,    0f, 0f));

        points.add(new Util.PointF3D(0f, 0.05f, -0.025f));
        points.add(new Util.PointF3D(0f, 0.05f,  0.025f));
        points.add(new Util.PointF3D(0f,    0f,      0f));

        vertices = initVertices(points);
        numberOfPoints = points.size();
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
