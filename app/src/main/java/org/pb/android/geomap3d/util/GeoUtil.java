package org.pb.android.geomap3d.util;

import android.location.Location;

import org.pb.android.geomap3d.data.GeoModel;
import org.pb.android.geomap3d.widget.TerrainWidget;

import static java.lang.Math.asin;
import static java.lang.Math.cos;
import static java.lang.Math.sin;
import static java.lang.Math.toRadians;

public class GeoUtil {

    public static final double DELTA_LATITUDE = 0.071865;
    public static final double DELTA_LONGITUDE = 0.114997;

    private static final int RADIUS_OF_EARTH_IN_KILOMETER = 6371;

    private static final String TAG = GeoUtil.class.getSimpleName();

    public static PositionOffsets getPositionOffsets(Location location, GeoModel geoModel) {
        return new PositionOffsets(location, geoModel);
    }

    // TODO: compare correctness of results of both methods
    //    public static float getDistanceBetween2PointsInMeter(Location start, Location end) {
//        float[] results = new float[1];
//        double lat1 = start.getLatitude();
//        double lng1 = start.getLongitude();
//        double lat2 = end.getLatitude();
//        double lng2 = end.getLongitude();
//        Location.distanceBetween(lat1, lng1, lat2, lng2, results);
//        return results[0];
//    }

    public static double getDistanceBetweenTwoPointsInMeter(Location startPoint, Location endPoint) {
        return 1000 * getDistanceBetweenTwoPointsInKilometer(startPoint, endPoint);
    }

    private static double getDistanceBetweenTwoPointsInKilometer(Location startPoint, Location endPoint) {
        if (startPoint == null || endPoint == null) {
            return 0;
        }

        double startPointLatitude = startPoint.getLatitude();
        double endPointLatitude = endPoint.getLatitude();
        double startPointLongitude = startPoint.getLongitude();
        double endPointLongitude = endPoint.getLongitude();

        double deltaLatitudes = toRadians(endPointLatitude - startPointLatitude);
        double deltaLongitudes = toRadians(endPointLongitude - startPointLongitude);

        double a = sin(deltaLatitudes / 2) * sin(deltaLatitudes / 2) + cos(toRadians(startPointLatitude))
                * cos(toRadians(endPointLatitude)) * sin(deltaLongitudes / 2) * sin(deltaLongitudes / 2);
        double c = 2 * asin(Math.sqrt(a));

        return RADIUS_OF_EARTH_IN_KILOMETER * c;
    }

    public static class PositionOffsets {

        public float xOffset, yOffset, zOffset;

        PositionOffsets(Location location, GeoModel geoModel) {
            xOffset = (float) ((TerrainWidget.XZ_DIMENSION * (location.getLongitude() - geoModel.getCenterPoint().getLongitude())) / (geoModel.getBoxEndPoint().getLongitude() - geoModel.getCenterPoint().getLongitude()));
            zOffset = -(float) ((TerrainWidget.XZ_DIMENSION * (location.getLatitude() - geoModel.getCenterPoint().getLatitude())) / (geoModel.getBoxStartPoint().getLatitude() - geoModel.getCenterPoint().getLatitude()));
            yOffset = TerrainWidget.getElevationValueFromLocation(geoModel.getHeightMapBitmap(), (double) xOffset, (double) zOffset);
        }

    }
}
