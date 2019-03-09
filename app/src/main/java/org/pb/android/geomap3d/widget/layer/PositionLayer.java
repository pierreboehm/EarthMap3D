package org.pb.android.geomap3d.widget.layer;

import android.location.Location;
import android.util.Log;
import android.view.MotionEvent;

import org.greenrobot.eventbus.EventBus;
import org.pb.android.geomap3d.data.GeoModel;
import org.pb.android.geomap3d.event.Events;
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
    private LayerType layerType;

    private float positionYOffset = 0f;
    private float positionXOffset = 0f;
    private float positionZOffset = 0f;

    private List<Util.PointF3D> points = new ArrayList<>();
    private FloatBuffer vertices;
    private float scale = 0.5f;
    private boolean showPointer = false;

    public PositionLayer(Location location, LayerType layerType) {
        this.location = location;
        this.layerType = layerType;

        initLayer();
    }

    @Override
    public LayerType getLayerType() {
        return layerType;
    }

    @Override
    public void draw(GL10 gl, FloatBuffer p1, int p2) {

        if (!showPointer) {
            return;
        }

        gl.glPushMatrix();

        gl.glTranslatef(positionXOffset, positionYOffset, positionZOffset);

        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
        gl.glVertexPointer(3, GL10.GL_FLOAT, 0, vertices);
        gl.glColor4f(layerType.getGlColor().red, layerType.getGlColor().green, layerType.getGlColor().blue, 1f);

        // draw center point w/o scale
        gl.glPointSize(12f);
        gl.glDrawArrays(GL10.GL_POINTS, 0, 1);

        // draw line
        gl.glLineWidth(1f); // or 1f
        gl.glDrawArrays(GL10.GL_LINES, 1, 2);

        // draw animated ring just for CDP
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

        gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
        gl.glPopMatrix();
    }

    @Override
    public void updateTouch(MotionEvent motionEvent, float xRotation, float yRotation) {
        // not implemented
    }

    public void updateLocation(Location location, GeoModel geoModel) {
        this.location = location;
        Log.v(TAG, "new location: lat=" + location.getLatitude() + ", longitude=" + location.getLongitude());

        if (GeoUtil.isLocationOnMap(location, geoModel)) {

            if (!showPointer) {
                EventBus.getDefault().post(new Events.VibrationEvent());
            }

            showPointer = true;

            GeoUtil.PositionOffsets positionOffsets = GeoUtil.getPositionOffsets(location, geoModel);
            positionXOffset = positionOffsets.xOffset;
            positionYOffset = positionOffsets.yOffset;
            positionZOffset = positionOffsets.zOffset;
        } else {
            if (showPointer) {
                EventBus.getDefault().post(new Events.ShowToast("Position outside of map"));
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

        // point
        points.add(new Util.PointF3D(0f, 0.1f, 0f));

        // line
        points.add(new Util.PointF3D(0f, 0.1f, 0f));
        points.add(new Util.PointF3D(0f, 0f, 0f));

        // ring
        if (layerType == LayerType.CDP) {
            for (int i = 9; i < 368; i++) {
                points.add(new Util.PointF3D(
                        (float) (Math.cos((double) (i - 9) * Math.PI / 180.0) * 0.01f),
                        0f,
                        (float) (Math.sin((double) (i - 9) * Math.PI / 180.0) * 0.01f)
                ));
            }
        }

        vertices = initVertices(points);
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
