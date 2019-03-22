package org.pb.android.geomap3d.event;

import android.location.Location;

public class Events {

    public static class ProgressUpdate {
        private final float progressValue;

        public ProgressUpdate(float progressValue) {
            this.progressValue = progressValue;
        }

        public float getProgressValue() {
            return progressValue;
        }
    }

    public static class WidgetReady {
    }

    public static class FragmentLoaded {
        private final String tag;

        public FragmentLoaded(String tag) {
            this.tag = tag;
        }

        public String getTag() {
            return tag;
        }
    }

    public static class VibrationEvent {
    }

    public static class ShowToast {
        private final String message;

        public ShowToast(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }

    public static class OutsideOfMap {
        private final Location location;

        public OutsideOfMap(Location location) {
            this.location = location;
        }

        public Location getLocation() {
            return location;
        }
    }
}
