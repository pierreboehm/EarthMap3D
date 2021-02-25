package org.pb.android.geomap3d.widget.layer;

import android.location.Location;
import android.util.Log;
import android.view.MotionEvent;

import org.greenrobot.eventbus.EventBus;
import org.pb.android.geomap3d.data.persist.geoarea.GeoArea;
import org.pb.android.geomap3d.event.Events;
import org.pb.android.geomap3d.util.GeoUtil;
import org.pb.android.geomap3d.util.Util;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.microedition.khronos.opengles.GL10;

import androidx.annotation.Nullable;

public class PositionLayer extends Layer {

    private static final String TAG = PositionLayer.class.getSimpleName();

    private Location location;
    private LayerType layerType;

    private float positionYOffset = 0f;
    private float positionXOffset = 0f;
    private float positionZOffset = 0f;

    private List<Util.PointF3D> points = new ArrayList<>();
    private FloatBuffer vertices;
    private float scale = 0.5f;
    private float pointSize;
    private boolean showPointer = true;     // FIXME: should be false, until first location update

    public PositionLayer(Location location, LayerType layerType) {
        this.location = location;
        this.layerType = layerType;

        pointSize = (layerType == LayerType.CDP || layerType == LayerType.CMP) ? 12f : 6f;

        initLayer();
    }

    @Override
    public LayerType getLayerType() {
        return layerType;
    }

    @Override
    public void draw(GL10 gl, FloatBuffer p1, int p2) {

        if (!showPointer || (positionXOffset == 0f && positionZOffset == 0f)) {
            return;
        }

        gl.glPushMatrix();

        gl.glTranslatef(positionXOffset, positionYOffset, positionZOffset);

        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
        gl.glVertexPointer(3, GL10.GL_FLOAT, 0, vertices);
        gl.glColor4f(layerType.getGlColor().red, layerType.getGlColor().green, layerType.getGlColor().blue, 1f);

        // draw center point w/o scale
        gl.glPointSize(pointSize);
        gl.glDrawArrays(GL10.GL_POINTS, 0, 1);

        // draw line and animated ring just for CDP
        if (layerType == LayerType.CDP || layerType == LayerType.CMP) {
            // draw line
            gl.glLineWidth(1f); // or 1f
            gl.glDrawArrays(GL10.GL_LINES, 1, 2);

            if (layerType == LayerType.CDP) {
                gl.glScalef(scale, 1f, scale);

                // draw ring
                gl.glLineWidth(4f);
                gl.glDrawArrays(GL10.GL_LINE_LOOP, 3, 359);

                scale = scale + 0.04f;
                if (scale > 6f) {
                    scale = 0.5f;
                }
            }
        }

        gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
        gl.glPopMatrix();
    }

    @Override
    public void updateTouch(MotionEvent motionEvent, float xRotation, float yRotation) {
        // not implemented
    }

    public Location getLocation() {
        return location;
    }

    public void updateLocation(@Nullable Location location, GeoArea geoArea) {
        if (location == null) {
            Log.i(TAG, "received location is null");
            return;
        }

        float distance = GeoUtil.getDistanceBetweenTwoPointsInMeter(this.location, location);
        // ignore location updates that equal current position
        if (layerType == LayerType.CDP && distance == 0f) {
            return;
        }

        this.location = location;
        Log.v(TAG, String.format(Locale.US, "new location: lat=%.06f, lng=%.06f, layerType=%s, dist=%.2fm", location.getLatitude(), location.getLongitude(), layerType.name(), distance));

        if (GeoUtil.isLocationOnMap(location, geoArea)) {

            if (!showPointer && (layerType == LayerType.CDP)) {
                EventBus.getDefault().post(new Events.VibrationEvent());
            }

            showPointer = true;

            GeoUtil.PositionOffsets positionOffsets = GeoUtil.getPositionOffsets(location, geoArea);
            positionXOffset = positionOffsets.xOffset;
            positionYOffset = positionOffsets.yOffset;
            positionZOffset = positionOffsets.zOffset;
        } else {
            if (showPointer && layerType == LayerType.CDP) {
                EventBus.getDefault().postSticky(new Events.OutsideOfMap(location));
            }

            showPointer = false;

            positionXOffset = 0f;
            positionYOffset = 0f;
            positionZOffset = 0f;

            Log.i(TAG, "Position outside of map");
        }

    }

    private void initLayer() {
        points = new ArrayList<>();

        if (layerType == LayerType.CDP || layerType == LayerType.CMP) {

            // point
            points.add(new Util.PointF3D(0f, 0.1f, 0f));

            // line
            points.add(new Util.PointF3D(0f, 0.1f, 0f));
            points.add(new Util.PointF3D(0f, 0f, 0f));

            if (layerType == LayerType.CDP) {
                // ring
                for (int i = 9; i < 368; i++) {
                    points.add(new Util.PointF3D(
                            (float) (Math.cos((double) (i - 9) * Math.PI / 180.0) * 0.01f),
                            0f,
                            (float) (Math.sin((double) (i - 9) * Math.PI / 180.0) * 0.01f)
                    ));
                }
            }
        } else if (layerType == LayerType.TDP) {
            points.add(new Util.PointF3D(0f, 0f, 0f));
        }

        vertices = initVertices(points);
    }
}
