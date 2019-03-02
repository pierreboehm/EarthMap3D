package org.pb.android.geomap3d.renderer;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.view.MotionEvent;

public abstract class OpenGLRenderer implements GLSurfaceView.Renderer {

    public abstract void updateTouch(MotionEvent motionEvent);

    public static GLSurfaceView.Renderer loadRenderer(Context context) {
        return new RendererOpenGL(context);
    }
}
