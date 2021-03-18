package org.pb.android.geomap3d.widget.layer;

import android.location.Location;
import android.util.Log;
import android.view.MotionEvent;

import androidx.annotation.Nullable;

import org.pb.android.geomap3d.data.persist.geoarea.GeoArea;
import org.pb.android.geomap3d.data.persist.geoplace.GeoPlace;
import org.pb.android.geomap3d.util.GeoUtil;
import org.pb.android.geomap3d.util.Util;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import javax.microedition.khronos.opengles.GL10;

public class DirectionLayer extends Layer {

    private static final String TAG = DirectionLayer.class.getSimpleName();

    private List<Util.PointF3D> points = new ArrayList<>();
    private FloatBuffer vertices;

    private float positionYOffset = 0f;
    private float positionXOffset = 0f;
    private float positionZOffset = 0f;

    private float yRotation;
    private float bearingTo;
    private float[] alpha = {.9f, .6f, .3f};
    private Location location;
    private GeoPlace targetGeoPlace;

    public DirectionLayer(Location location, GeoPlace targetGeoPlace, GeoArea geoArea) {
        setupPositionOffsets(location, geoArea);
        setTargetGeoPlace(targetGeoPlace);
        initLayer();
    }

    @Override
    public LayerType getLayerType() {
        return LayerType.PTR;
    }

    @Override
    public void draw(GL10 gl, FloatBuffer p1, int p2) {
        if (positionXOffset == 0f && positionZOffset == 0f) {
            return;
        }

        gl.glPushMatrix();

        gl.glTranslatef(positionXOffset, positionYOffset, positionZOffset);
        gl.glRotatef(bearingTo, 0f, 1f, 0f);

        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
        gl.glVertexPointer(3, GL10.GL_FLOAT, 0, vertices);

        gl.glEnable(GL10.GL_BLEND);
        gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);

        for (int index = 0; index < 3; index++) {
            alpha[index] = computeNextAlpha(alpha[index]);
            gl.glColor4f(getLayerType().getGlColor().red, getLayerType().getGlColor().green, getLayerType().getGlColor().blue, alpha[index]);
            gl.glDrawArrays(GL10.GL_TRIANGLES, index * 3, 3);
        }

        gl.glDisable(GL10.GL_BLEND);
        gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
        gl.glPopMatrix();
    }

    @Override
    public void updateTouch(MotionEvent motionEvent, float xRotation, float yRotation) {
        this.yRotation = yRotation;
    }

    public void updateLocation(@Nullable Location location, GeoArea geoArea) {
        if (location == null) {
            return;
        }

        setupPositionOffsets(location, geoArea);
    }

    public void setTargetGeoPlace(GeoPlace targetGeoPlace) {
        this.targetGeoPlace = targetGeoPlace;

        if (location == null) {
            return;
        }

        float bearing = (float) GeoUtil.getBearing(location, targetGeoPlace.getLocation());
        bearingTo = bearing < 0f ? (bearing + 360f) % 360f : bearing;
        //bearingTo = (bearing + 180f) % 360f;
        Log.d(TAG, "bearing: " + bearingTo);
    }

    private void setupPositionOffsets(Location location, GeoArea geoArea) {
        this.location = location;

        if (GeoUtil.isLocationOnMap(location, geoArea)) {
            GeoUtil.PositionOffsets positionOffsets = GeoUtil.getPositionOffsets(location, geoArea);
            positionXOffset = positionOffsets.xOffset;
            positionYOffset = positionOffsets.yOffset;
            positionZOffset = positionOffsets.zOffset;
        } else {
            positionXOffset = 0f;
            positionYOffset = 0f;
            positionZOffset = 0f;
        }
    }

    private void initLayer() {
        points = new ArrayList<>();

        points.add(new Util.PointF3D(0f, 0f, .2f)); // possibly negative z
        points.add(new Util.PointF3D(.05f, 0f, .1f));
        points.add(new Util.PointF3D(-.05f, 0f, .1f));

        points.add(new Util.PointF3D(0f, 0f, .3f));
        points.add(new Util.PointF3D(.05f, 0f, .2f));
        points.add(new Util.PointF3D(-.05f, 0f, .2f));

        points.add(new Util.PointF3D(0f, 0f, .4f));
        points.add(new Util.PointF3D(.05f, 0f, .3f));
        points.add(new Util.PointF3D(-.05f, 0f, .3f));

        vertices = initVertices(points);
    }

    private float computeNextAlpha(float alpha) {
        float newAlpha = (alpha + .01f) % 1f;
        return Math.max(newAlpha, .3f);
    }
}
