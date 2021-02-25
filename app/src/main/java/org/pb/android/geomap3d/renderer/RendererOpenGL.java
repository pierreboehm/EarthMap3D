package org.pb.android.geomap3d.renderer;

import android.content.Context;
import android.content.res.Configuration;
import android.location.Location;
import android.opengl.GLSurfaceView;
import android.opengl.GLU;
import android.view.MotionEvent;

import org.pb.android.geomap3d.data.persist.geoplace.GeoPlaces;
import org.pb.android.geomap3d.widget.Widget;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class RendererOpenGL implements GLSurfaceView.Renderer {

    private static final String TAG = RendererOpenGL.class.getSimpleName();

    public static final float ROTATION_INITIAL = 20f;
    public static final float SCALE_INITIAL = .6f;
    private static final float DEPTH_INITIAL = -10f;

    private Widget widget;
    private float scale;
    private int orientation;

    public RendererOpenGL(Context context) {
        scale = SCALE_INITIAL;
    }

    @Override
    public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
        gl10.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        gl10.glShadeModel(GL10.GL_SMOOTH);
        gl10.glClearDepthf(1.0f);
        gl10.glEnable(GL10.GL_DEPTH_TEST);
        gl10.glDepthFunc(GL10.GL_LEQUAL);
        gl10.glDisable(GL10.GL_DITHER);
        gl10.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT, GL10.GL_NICEST);
    }

    @Override
    public void onSurfaceChanged(GL10 gl10, int width, int height) {
        orientation = (width > height) ? Configuration.ORIENTATION_LANDSCAPE : Configuration.ORIENTATION_PORTRAIT;

        gl10.glViewport(0, 0, width, height);

        gl10.glMatrixMode(GL10.GL_PROJECTION);
        gl10.glLoadIdentity();

//        float aspect = width < height ? width / height : height / width;
        GLU.gluPerspective(gl10, 45.0f, (float) width / height, 0.1f, 100.0f);

        gl10.glMatrixMode(GL10.GL_MODELVIEW);
        gl10.glLoadIdentity();

        if (widget != null) {
            onDrawFrame(gl10);
        }
    }

    @Override
    public void onDrawFrame(GL10 gl10) {
        gl10.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);

        gl10.glLoadIdentity();

        gl10.glTranslatef(0f, -1f, DEPTH_INITIAL);

        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            gl10.glScalef(scale * 2f, scale * 2f, scale * 2f);
        } else {
            gl10.glScalef(scale, scale, scale);
        }

//        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
//            gl10.glRotatef(90f, 0f, 1f, 0f);
//        }

        if (widget != null) {
            widget.draw(gl10);
        }
    }

    public float getScale() {
        return scale;
    }

    public void setWidget(Widget widget) {
        this.widget = widget;
    }

    public void updateTouchEvent(MotionEvent motionEvent) {
        if (widget != null) {
            widget.updateTouch(motionEvent);
        }
    }

    public void updateDeviceRotation(float azimuth) {
        if (widget != null) {
            widget.updateDeviceRotation(azimuth);
        }
    }

    public void updateDeviceLocation(Location location) {
        if (widget != null) {
            widget.updateDeviceLocation(location);
        }
    }

    public void updateTrackedLocation(Location location) {
        if (widget != null) {
            widget.updateTrackedLocation(location);
        }
    }

    public void setTrackDistance(int trackDistance) {
        if (widget != null) {
            widget.updateTrackDistance(trackDistance);
        }
    }

    public void setTackEnabled(boolean trackEnabled) {
        if (widget != null) {
            widget.updateTrackEnabled(trackEnabled);
        }
    }

    public void setCampLocation(Location location) {
        if (widget != null) {
            widget.setCampLocation(location);
        }
    }

    public void setGeoPlaces(GeoPlaces geoPlaces) {
        if (widget != null) {
            widget.setGeoPlaces(geoPlaces);
        }
    }

    public synchronized void updateScale(float scale) {
        this.scale = scale;
    }
}

