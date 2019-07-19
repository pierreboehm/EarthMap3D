package org.pb.android.geomap3d.util;

public class NetworkAvailabilityUtil {

    private static NetworkAvailabilityCheck networkAvailabilityCheckCallback;

    public static void setNetworkAvailabilityCheck(NetworkAvailabilityCheck networkAvailabilityCheck) {
        networkAvailabilityCheckCallback = networkAvailabilityCheck;
    }

    public static boolean isNetworkAvailable() {
        if (networkAvailabilityCheckCallback != null) {
            return networkAvailabilityCheckCallback.isNetworkAvailable();
        }
        return false;
    }

    public interface NetworkAvailabilityCheck {
        boolean isNetworkAvailable();
    }
}
