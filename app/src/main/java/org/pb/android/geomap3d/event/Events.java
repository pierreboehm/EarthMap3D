package org.pb.android.geomap3d.event;

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

    public static class ProgressFinished {
    }

    public static class LocationManagerReady {
    }

}
