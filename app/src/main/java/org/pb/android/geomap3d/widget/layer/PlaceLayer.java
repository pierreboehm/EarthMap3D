package org.pb.android.geomap3d.widget.layer;

import android.location.Location;
import android.view.MotionEvent;

import org.pb.android.geomap3d.data.persist.geoarea.GeoArea;
import org.pb.android.geomap3d.util.GeoUtil;
import org.pb.android.geomap3d.util.Util;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import javax.microedition.khronos.opengles.GL10;

public class PlaceLayer extends Layer {

    public static final String TAG = PlaceLayer.class.getSimpleName();

    private List<Util.PointF3D> points = new ArrayList<>();
    private FloatBuffer vertices;

    private float positionYOffset = 0f;
    private float positionXOffset = 0f;
    private float positionZOffset = 0f;

    private float yRotation = 0f;
    private boolean isVisible = true;

    public PlaceLayer(Location location, GeoArea geoArea) {
        setupPositionOffsets(location, geoArea);
        initLayer();
    }

    @Override
    public LayerType getLayerType() {
        return LayerType.PLC;
    }

    @Override
    public void draw(GL10 gl, FloatBuffer p1, int p2) {
        if (!isVisible || (positionXOffset == 0f && positionZOffset == 0f)) {
            return;
        }

        gl.glPushMatrix();

        gl.glTranslatef(positionXOffset, positionYOffset, positionZOffset);
        gl.glRotatef(-yRotation, 0f, 1f, 0f);

        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
        gl.glVertexPointer(3, GL10.GL_FLOAT, 0, vertices);
        gl.glColor4f(getLayerType().getGlColor().red, getLayerType().getGlColor().green, getLayerType().getGlColor().blue, 1f);

        gl.glDrawArrays(GL10.GL_TRIANGLES, 0, 3);

        gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
        gl.glPopMatrix();
    }

    @Override
    public void updateTouch(MotionEvent motionEvent, float xRotation, float yRotation) {
        this.yRotation = yRotation;
    }

    public boolean isVisible() {
        return isVisible;
    }

    public void setVisible(boolean isVisible) {
        this.isVisible = isVisible;
    }

    private void setupPositionOffsets(Location location, GeoArea geoArea) {
        if (GeoUtil.isLocationOnMap(location, geoArea)) {
            GeoUtil.PositionOffsets positionOffsets = GeoUtil.getPositionOffsets(location, geoArea);
            positionXOffset = positionOffsets.xOffset;
            positionYOffset = positionOffsets.yOffset;
            positionZOffset = positionOffsets.zOffset;

            isVisible = true;
        }
    }

    private void initLayer() {
        points = new ArrayList<>();

        // top left
        points.add(new Util.PointF3D(-0.05f, 0.1f, 0f));
        // top right
        points.add(new Util.PointF3D(0.05f, 0.1f, 0f));
        // bottom center
        points.add(new Util.PointF3D(0f, 0f, 0f));

        vertices = initVertices(points);
    }
}
