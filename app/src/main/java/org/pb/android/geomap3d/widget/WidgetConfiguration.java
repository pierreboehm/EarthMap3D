package org.pb.android.geomap3d.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;

import org.pb.android.geomap3d.data.persist.geolocation.GeoLocation;

import androidx.annotation.Nullable;

import static org.pb.android.geomap3d.widget.TerrainWidget.BITMAP_DIMENSION;

public class WidgetConfiguration {

    private Location location;
    private GeoLocation geoLocation;

    private WidgetConfiguration(Builder builder) {
        location = builder.location;
        geoLocation = builder.geoLocation == null ? new GeoLocation("", location, builder.heightMapBitmap) : builder.geoLocation;

        if (!geoLocation.hasHeightMapBitmap()) {
            geoLocation.setHeightMap(Bitmap.createBitmap(BITMAP_DIMENSION, BITMAP_DIMENSION, Bitmap.Config.RGB_565));
        }
    }

    public boolean hasLocation() {
        return location != null;
    }

    @Nullable
    public Location getLocation() {
        return location;
    }

    public boolean hasHeightMapBitmap() {
        return geoLocation.getHeightMapBitmap() != null;
    }

    public Bitmap getHeightMapBitmap() {
        return geoLocation.getHeightMapBitmap();
    }

    public GeoLocation getGeoLocation() {
        return geoLocation;
    }

    public static Builder create() {
        return new Builder();
    }

    public static class Builder {
        Location location;
        Bitmap heightMapBitmap = null;
        GeoLocation geoLocation = null;

        public Builder setLocation(Location location) {
            this.location = location;
            return this;
        }

        public Builder setHeightMapBitmapFromResource(Context context, int heightMapResourceId) {
            Bitmap rawmap = BitmapFactory.decodeResource(context.getResources(), heightMapResourceId);
            heightMapBitmap = Bitmap.createScaledBitmap(rawmap, BITMAP_DIMENSION, BITMAP_DIMENSION, true);
            return this;
        }

        public Builder setHeightMapBitmap(Bitmap heightMapBitmap) {
            this.heightMapBitmap = Bitmap.createScaledBitmap(heightMapBitmap, BITMAP_DIMENSION, BITMAP_DIMENSION, true);
            return this;
        }

        public Builder setGeoLocation(GeoLocation geoLocation) {
            this.geoLocation = geoLocation;
            return this;
        }

        public WidgetConfiguration getConfiguration() {
            WidgetConfiguration widgetConfiguration = new WidgetConfiguration(this);
            return widgetConfiguration;
        }
    }

}
