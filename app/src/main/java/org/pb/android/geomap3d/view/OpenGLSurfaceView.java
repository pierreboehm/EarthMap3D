package org.pb.android.geomap3d.view;

import android.content.Context;
import android.location.Location;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import org.pb.android.geomap3d.renderer.OpenGLRenderer;
import org.pb.android.geomap3d.renderer.RendererOpenGL;
import org.pb.android.geomap3d.widget.Widget;

public class OpenGLSurfaceView extends GLSurfaceView implements View.OnTouchListener {

    private Renderer openGLRenderer;
    private ScaleGestureDetector scaleGestureDetector;
    private float scaleFactor = RendererOpenGL.SCALE_INITIAL;

    public OpenGLSurfaceView(Context context) {
        this(context, null);
    }

    public OpenGLSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);

        setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        setKeepScreenOn(true);

        scaleGestureDetector = new ScaleGestureDetector(context, new ScaleListener());
        setOnTouchListener(this);
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        // FIXME: prevent hopping. (e.g. by just delegating event if count of pointers is more than 1)
        scaleGestureDetector.onTouchEvent(motionEvent);

        ((RendererOpenGL) openGLRenderer).updateTouchEvent(motionEvent);
        return true;
    }

    public void initRenderer(Context context) {
        openGLRenderer = OpenGLRenderer.loadRenderer(context);
        setRenderer(openGLRenderer);
        scaleFactor = ((RendererOpenGL) openGLRenderer).getScale();
    }

    public void updateDeviceRotation(float azimuth) {
        ((RendererOpenGL) openGLRenderer).updateDeviceRotation(azimuth);
    }

    public void updateDeviceLocation(Location location) {
        ((RendererOpenGL) openGLRenderer).updateDeviceLocation(location);
    }

    public void setTrackDistance(int trackDistance) {
        ((RendererOpenGL) openGLRenderer).setTrackDistance(trackDistance);
    }

    public void setWidget(Widget widget) {
        ((RendererOpenGL) openGLRenderer).setWidget(widget);
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            scaleFactor *= detector.getScaleFactor();
            scaleFactor = Math.max(0.6f, Math.min(scaleFactor, 5.0f));

            ((RendererOpenGL) openGLRenderer).updateScale(scaleFactor);
            return true;
        }
    }
}
