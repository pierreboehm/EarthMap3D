package org.pb.android.geomap3d.widget;

import android.location.Location;
import android.support.annotation.Nullable;

public class WidgetConfiguration {

    private Location location;
    private int heightMapResourceId;

    private WidgetConfiguration(Builder builder) {
        location = builder.location;
        heightMapResourceId = builder.heightMapResourceId;
    }

    @Nullable
    public Location getLocation() {
        return location;
    }

    public boolean hasHeightMapResourceId() {
        return heightMapResourceId != -1;
    }

    public int getHeightMapResourceId() {
        return heightMapResourceId;
    }

    public static Builder create() {
        return new Builder();
    }

    public static class Builder {
        Location location;
        int heightMapResourceId = -1;

        public Builder setLocation(Location location) {
            this.location = location;
            return this;
        }

        public Builder setHeightMapResourceId(int heightMapResourceId) {
            this.heightMapResourceId = heightMapResourceId;
            return this;
        }

        public WidgetConfiguration getConfiguration() {
            WidgetConfiguration widgetConfiguration = new WidgetConfiguration(this);
            return widgetConfiguration;
        }
    }

}
