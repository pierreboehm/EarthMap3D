package org.pb.android.geomap3d.widget;

import android.location.Location;
import android.os.Parcelable;
import android.view.MotionEvent;

import org.pb.android.geomap3d.data.persist.geoplace.GeoPlaces;

import javax.microedition.khronos.opengles.GL10;

public abstract class Widget implements Parcelable {

    public abstract void draw(GL10 gl);

    public abstract void updateTouch(MotionEvent motionEvent);

    public abstract void updateDeviceRotation(float azimuth);

    public abstract void updateDeviceLocation(Location location);

    public abstract void updateTrackDistance(int trackDistance);

    public abstract void updateTrackedLocation(Location location);

    public abstract void updateTrackEnabled(boolean trackEnabled);

    public abstract void setCampLocation(Location location);

    public abstract void setGeoPlaces(GeoPlaces geoPlaces);

    public abstract void setShowGeoPlaces(boolean show);

    public abstract void initWidget(WidgetConfiguration widgetConfiguration);

    public abstract void updateWidget(WidgetConfiguration widgetConfiguration);

    public boolean isInitialized() {
        return false;
    }

}
