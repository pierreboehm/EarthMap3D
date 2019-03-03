package org.pb.android.geomap3d.widget;

import android.view.MotionEvent;

import java.io.Serializable;

import javax.microedition.khronos.opengles.GL10;

public abstract class Widget implements Serializable {

    public abstract void draw(GL10 gl);

    public abstract void updateTouch(MotionEvent motioEvent);

    public abstract void updateDeviceRotation(float azimuth);

    public abstract void initWidget(WidgetConfiguration widgetConfiguration);

    public boolean isInitialized() {
        return false;
    }

}
