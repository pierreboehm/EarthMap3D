package org.pb.android.geomap3d.renderer;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.opengl.GLU;
import android.view.MotionEvent;

import org.pb.android.geomap3d.widget.TerrainWidget;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class RendererOpenGL implements GLSurfaceView.Renderer {

    public static final float ROTATION_INITIAL = 20f;
    public static final float SCALE_INITIAL = .6f;
    private static final float DEPTH_INITIAL = -10f;

    private Context context;
    private TerrainWidget terrainWidget;
    private float scale;

    public RendererOpenGL(Context context) {
        scale = SCALE_INITIAL;
        this.context = context;
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
        gl10.glViewport(0, 0, width, height);

        gl10.glMatrixMode(GL10.GL_PROJECTION);
        gl10.glLoadIdentity();

//        float aspect = width < height ? width / height : height / width;
        GLU.gluPerspective(gl10, 45.0f, (float) width / height, 0.1f, 100.0f);

        gl10.glMatrixMode(GL10.GL_MODELVIEW);
        gl10.glLoadIdentity();

        if (terrainWidget != null) {
            onDrawFrame(gl10);
        }
    }

    @Override
    public void onDrawFrame(GL10 gl10) {
        gl10.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);

        gl10.glLoadIdentity();
        gl10.glTranslatef(0f, -1f, DEPTH_INITIAL);
        gl10.glScalef(scale, scale, scale);

        if (terrainWidget != null) {
            terrainWidget.draw(gl10);
        }
    }

    public void initWidget() {
        terrainWidget = new TerrainWidget(context);
    }

    public void startWidget() {
        terrainWidget.initWidget();
    }

    public void updateTouchEvent(MotionEvent motionEvent) {
        if (terrainWidget != null) {
            terrainWidget.updateTouch(motionEvent);
        }
    }

    public void updateDeviceRotation(float azimuth) {
        if (terrainWidget != null) {
            terrainWidget.updateDeviceRotation(azimuth);
        }
    }

    public synchronized void updateScale(float scale) {
        this.scale = scale;
    }
}

