package org.pb.android.geomap3d.util;

import android.location.Location;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

import org.pb.android.geomap3d.data.persist.geoarea.GeoArea;
import org.pb.android.geomap3d.fragment.ui.MapView;
import org.pb.android.geomap3d.widget.TerrainWidget;

import androidx.annotation.Nullable;

import static java.lang.Math.asin;
import static java.lang.Math.atan2;
import static java.lang.Math.cos;
import static java.lang.Math.sin;
import static java.lang.Math.toDegrees;
import static java.lang.Math.toRadians;

public class GeoUtil {

    private static final String TAG = GeoUtil.class.getSimpleName();

    public static final double DELTA_LATITUDE = 0.071865;
    public static final double DELTA_LONGITUDE = 0.114997;

    public static final int RADIUS_OF_EARTH_IN_KILOMETER = 6371;
    private static final double DEFAULT_SIDE_LENGTH = MapView.MINIMUM_GEO_FENCE_SIZE_IN_METER / 1000;

    public static boolean isLocationOnMap(Location location, GeoArea geoArea) {
        return location.getLatitude() <= geoArea.getBoxStartPoint().getLatitude()
                && location.getLatitude() >= geoArea.getBoxEndPoint().getLatitude()
                && location.getLongitude() >= geoArea.getBoxStartPoint().getLongitude()
                && location.getLongitude() <= geoArea.getBoxEndPoint().getLongitude();
    }

    public static boolean isLocationInsideBounds(Location location, Location boundStartLocation, Location boundEndLocation) {
        return location.getLatitude() <= boundStartLocation.getLatitude()
                && location.getLatitude() >= boundEndLocation.getLatitude()
                && location.getLongitude() >= boundStartLocation.getLongitude()
                && location.getLongitude() <= boundEndLocation.getLongitude();
    }

    public static PositionOffsets getPositionOffsets(Location location, GeoArea geoArea) {
        return new PositionOffsets(location, geoArea);
    }

    // TODO: compare correctness of results of both methods
    public static float getDistanceBetweenTwoPointsInMeter(Location startPoint, Location endPoint) {
        float[] results = new float[1];
        double lat1 = startPoint.getLatitude();
        double lng1 = startPoint.getLongitude();
        double lat2 = endPoint.getLatitude();
        double lng2 = endPoint.getLongitude();
        Location.distanceBetween(lat1, lng1, lat2, lng2, results);
        return results[0];
    }

    public static Location getLocation(double latitude, double longitude) {
        Location location = new Location("");
        location.setLatitude(latitude);
        location.setLongitude(longitude);
        return location;
    }

    public static Location getLocationFromLatLng(LatLng pointLatLng) {
        if (pointLatLng == null) {
            Log.e(TAG, "Try to parse LatLng which is null");
            return null;
        }

        Location location = new Location("");
        location.setLatitude(pointLatLng.latitude);
        location.setLongitude(pointLatLng.longitude);
        return location;
    }

    @Nullable
    public static LatLng getLatLngFromLocation(@Nullable Location location) {
        return location == null ? null : new LatLng(location.getLatitude(), location.getLongitude());
    }

    public static LatLngBounds getDefaultRectangleLatLngBounds(LatLng centerOfMap) {
        return getRectangleLatLngBounds(centerOfMap, DEFAULT_SIDE_LENGTH);
    }

    public static double getDistanceBetweenTwoPointsInMeter(LatLng startPoint, LatLng endPoint) {
        return 1000 * getDistanceBetweenTwoPointsInKilometer(startPoint, endPoint);
    }

    public static LatLng computeOffset(LatLng from, double distance, double heading) {
        distance /= RADIUS_OF_EARTH_IN_KILOMETER;
        heading = toRadians(heading);

        double fromLat = toRadians(from.latitude);
        double fromLng = toRadians(from.longitude);
        double cosDistance = cos(distance);
        double sinDistance = sin(distance);
        double sinFromLat = sin(fromLat);
        double cosFromLat = cos(fromLat);
        double sinLat = cosDistance * sinFromLat + sinDistance * cosFromLat * cos(heading);
        double dLng = atan2(sinDistance * cosFromLat * sin(heading), cosDistance - sinFromLat * sinLat);
        return new LatLng(toDegrees(asin(sinLat)), toDegrees(fromLng + dLng));
    }

    public static double getBearing(Location startPoint, Location endPoint) {
        double longitude1 = startPoint.getLongitude();
        double latitude1 = Math.toRadians(startPoint.getLatitude());

        double longitude2 = endPoint.getLongitude();
        double latitude2 = Math.toRadians(endPoint.getLatitude());

        double longDiff = Math.toRadians(longitude2 - longitude1);

        double y = Math.sin(longDiff) * Math.cos(latitude2);
        double x = Math.cos(latitude1) * Math.sin(latitude2) - Math.sin(latitude1) * Math.cos(latitude2) * Math.cos(longDiff);

        return Math.toDegrees(Math.atan2(y, x));
    }

    private static double getDistanceBetweenTwoPointsInKilometer(LatLng startPoint, LatLng endPoint) {
        if (startPoint == null || endPoint == null) {
            Log.e(TAG, "get Distance to null location");
            return 0;
        }

        double lat1 = startPoint.latitude;
        double lat2 = endPoint.latitude;
        double lon1 = startPoint.longitude;
        double lon2 = endPoint.longitude;
        double dLat = toRadians(lat2 - lat1);
        double dLon = toRadians(lon2 - lon1);
        double a = sin(dLat / 2) * sin(dLat / 2)
                + cos(toRadians(lat1))
                * cos(toRadians(lat2)) * sin(dLon / 2)
                * sin(dLon / 2);
        double c = 2 * asin(Math.sqrt(a));
        return RADIUS_OF_EARTH_IN_KILOMETER * c;

    }

    private static LatLngBounds getRectangleLatLngBounds(LatLng centerOfMap, double sideLengthInKilometers) {
        return new LatLngBounds(getSouthWestLatLng(centerOfMap, sideLengthInKilometers), getNorthEastLatLng(centerOfMap, sideLengthInKilometers));
    }

    private static LatLng getNorthEastLatLng(LatLng centerOfMap, double sideLengthInKilometers) {
        LatLng southWest = computeOffset(centerOfMap, sideLengthInKilometers / 2, 90);
        southWest = computeOffset(southWest, sideLengthInKilometers / 2, 0);
        return southWest;
    }

    private static LatLng getSouthWestLatLng(LatLng centerOfMap, double sideLengthInKilometers) {
        LatLng northEast = computeOffset(centerOfMap, sideLengthInKilometers / 2, 270);
        northEast = computeOffset(northEast, sideLengthInKilometers / 2, 180);
        return northEast;
    }

//    public static double getDistanceBetweenTwoPointsInMeter(Location startPoint, Location endPoint) {
//        return 1000 * getDistanceBetweenTwoPointsInKilometer(startPoint, endPoint);
//    }

//    private static double getDistanceBetweenTwoPointsInKilometer(Location startPoint, Location endPoint) {
//        if (startPoint == null || endPoint == null) {
//            return 0;
//        }
//
//        double startPointLatitude = startPoint.getLatitude();
//        double endPointLatitude = endPoint.getLatitude();
//        double startPointLongitude = startPoint.getLongitude();
//        double endPointLongitude = endPoint.getLongitude();
//
//        double deltaLatitudes = toRadians(endPointLatitude - startPointLatitude);
//        double deltaLongitudes = toRadians(endPointLongitude - startPointLongitude);
//
//        double a = sin(deltaLatitudes / 2) * sin(deltaLatitudes / 2) + cos(toRadians(startPointLatitude))
//                * cos(toRadians(endPointLatitude)) * sin(deltaLongitudes / 2) * sin(deltaLongitudes / 2);
//        double c = 2 * asin(Math.sqrt(a));
//
//        return RADIUS_OF_EARTH_IN_KILOMETER * c;
//    }

    public static class PositionOffsets {

        public float xOffset, yOffset, zOffset;

        PositionOffsets(Location location, GeoArea geoArea) {
            xOffset = (float) ((TerrainWidget.XZ_DIMENSION * (location.getLongitude() - geoArea.getCenterPoint().getLongitude())) / (geoArea.getBoxEndPoint().getLongitude() - geoArea.getCenterPoint().getLongitude()));
            zOffset = -(float) ((TerrainWidget.XZ_DIMENSION * (location.getLatitude() - geoArea.getCenterPoint().getLatitude())) / (geoArea.getBoxStartPoint().getLatitude() - geoArea.getCenterPoint().getLatitude()));
            yOffset = TerrainWidget.getElevationValueFromLocation(geoArea.getHeightMapBitmap(), xOffset, zOffset);
        }

    }
}
