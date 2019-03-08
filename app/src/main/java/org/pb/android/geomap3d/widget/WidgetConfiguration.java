package org.pb.android.geomap3d.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.support.annotation.Nullable;

import org.pb.android.geomap3d.data.GeoModel;

import static org.pb.android.geomap3d.widget.TerrainWidget.BITMAP_DIMENSION;

public class WidgetConfiguration {

    private Location location;
    private GeoModel geoModel;

    private WidgetConfiguration(Builder builder) {
        location = builder.location;
        geoModel = builder.geoModel == null ? new GeoModel("", location, builder.heightMapBitmap) : builder.geoModel;

        if (!geoModel.hasHeightMapBitmap()) {
            geoModel.setHeightMap(Bitmap.createBitmap(BITMAP_DIMENSION, BITMAP_DIMENSION, Bitmap.Config.RGB_565));
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
        return geoModel.getHeightMapBitmap() != null;
    }

    public Bitmap getHeightMapBitmap() {
        return geoModel.getHeightMapBitmap();
    }

    public GeoModel getGeoModel() {
        return geoModel;
    }

    public static Builder create() {
        return new Builder();
    }

    public static class Builder {
        Location location;
        Bitmap heightMapBitmap = null;
        GeoModel geoModel = null;

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

        public Builder setGeoModel(GeoModel geoModel) {
            this.geoModel = geoModel;
            return this;
        }

        public WidgetConfiguration getConfiguration() {
            WidgetConfiguration widgetConfiguration = new WidgetConfiguration(this);
            return widgetConfiguration;
        }
    }

}
