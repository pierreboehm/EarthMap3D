package org.pb.android.geomap3d.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;

import org.pb.android.geomap3d.data.persist.geoarea.GeoArea;
import org.pb.android.geomap3d.util.GeoUtil;

import androidx.annotation.Nullable;

import static org.pb.android.geomap3d.widget.TerrainWidget.BITMAP_DIMENSION;

public class WidgetConfiguration {

    private Location location;
    private GeoArea geoArea;

    private WidgetConfiguration(Builder builder) {
        location = builder.location;
        geoArea = builder.geoArea;

        if (geoArea == null) {
            geoArea = new GeoArea.Builder()
                .setCenterOfMap(GeoUtil.getLatLngFromLocation(location))
                .setHeightMap(builder.heightMapBitmap)
                .build();
        }

        if (!geoArea.hasHeightMapBitmap()) {
            geoArea.setHeightMap(Bitmap.createBitmap(BITMAP_DIMENSION, BITMAP_DIMENSION, Bitmap.Config.RGB_565));
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
        return geoArea.getHeightMapBitmap() != null;
    }

    public Bitmap getHeightMapBitmap() {
        return geoArea.getHeightMapBitmap();
    }

    public GeoArea getGeoArea() {
        return geoArea;
    }

    public static Builder create() {
        return new Builder();
    }

    public static class Builder {
        Location location;
        Bitmap heightMapBitmap = null;
        GeoArea geoArea = null;

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

        public Builder setGeoArea(GeoArea geoArea) {
            this.geoArea = geoArea;
            return this;
        }

        public WidgetConfiguration getConfiguration() {
            WidgetConfiguration widgetConfiguration = new WidgetConfiguration(this);
            return widgetConfiguration;
        }
    }

}
