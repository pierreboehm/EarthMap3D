package org.pb.android.geomap3d.widget;

import android.location.Location;

public class WidgetConfiguration {

    private Location location;

    private WidgetConfiguration(Builder builder) {
    }

    public static Builder create() {
        return new Builder();
    }

    public static class Builder {
        Location location;

        public Builder setLocation(Location location) {
            this.location = location;
            return this;
        }

        public WidgetConfiguration getConfiguration() {
            WidgetConfiguration widgetConfiguration = new WidgetConfiguration(this);
            return widgetConfiguration;
        }
    }

}
