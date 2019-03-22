package org.pb.android.geomap3d.util;

import android.location.Location;

import org.pb.android.geomap3d.data.GeoModel;
import org.pb.android.geomap3d.widget.TerrainWidget;

public class GeoUtil {

    public static final double DELTA_LATITUDE = 0.071865;
    public static final double DELTA_LONGITUDE = 0.114997;

    private static final int RADIUS_OF_EARTH_IN_KILOMETER = 6371;

    private static final String TAG = GeoUtil.class.getSimpleName();

    public static boolean isLocationOnMap(Location location, GeoModel geoModel) {
        return location.getLatitude() <= geoModel.getBoxStartPoint().getLatitude()
                && location.getLatitude() >= geoModel.getBoxEndPoint().getLatitude()
                && location.getLongitude() >= geoModel.getBoxStartPoint().getLongitude()
                && location.getLongitude() <= geoModel.getBoxEndPoint().getLongitude();
    }

    public static boolean isLocationInsideBounds(Location location, Location boundStartLocation, Location boundEndLocation) {
        return location.getLatitude() <= boundStartLocation.getLatitude()
                && location.getLatitude() >= boundEndLocation.getLatitude()
                && location.getLongitude() >= boundStartLocation.getLongitude()
                && location.getLongitude() <= boundEndLocation.getLongitude();
    }

    public static PositionOffsets getPositionOffsets(Location location, GeoModel geoModel) {
        return new PositionOffsets(location, geoModel);
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

//    public static double getDistanceBetweenTwoPointsInMeter(Location startPoint, Location endPoint) {
//        return 1000 * getDistanceBetweenTwoPointsInKilometer(startPoint, endPoint);
//    }
//
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

        PositionOffsets(Location location, GeoModel geoModel) {
            xOffset = (float) ((TerrainWidget.XZ_DIMENSION * (location.getLongitude() - geoModel.getCenterPoint().getLongitude())) / (geoModel.getBoxEndPoint().getLongitude() - geoModel.getCenterPoint().getLongitude()));
            zOffset = -(float) ((TerrainWidget.XZ_DIMENSION * (location.getLatitude() - geoModel.getCenterPoint().getLatitude())) / (geoModel.getBoxStartPoint().getLatitude() - geoModel.getCenterPoint().getLatitude()));
            yOffset = TerrainWidget.getElevationValueFromLocation(geoModel.getHeightMapBitmap(), (double) xOffset, (double) zOffset);
        }

    }
}
