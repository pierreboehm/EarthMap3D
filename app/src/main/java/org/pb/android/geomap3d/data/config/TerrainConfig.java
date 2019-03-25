package org.pb.android.geomap3d.data.config;

import android.location.Location;

import org.pb.android.geomap3d.R;
import org.pb.android.geomap3d.util.GeoUtil;

public enum TerrainConfig {

    BERLIN_STADTMITTE(R.drawable.berlin_stadtmitte_height_map, 52.529731, 13.391537),
    KAUFFUNGER_WALD_1(R.drawable.kauffunger_wald_2_height_map, 51.281761, 9.800702),
    KAUFFUNGER_WALD_2(R.drawable.kauffunger_wald_3_height_map, 51.281761, 9.685705);

    private final int heightMapResourceId;
    private final double centerLatitude;
    private final double centerLongitude;

    TerrainConfig(int heightMapResourceId, double centerLatitude, double centerLongitude) {
        this.heightMapResourceId = heightMapResourceId;
        this.centerLatitude = centerLatitude;
        this.centerLongitude = centerLongitude;
    }

    public Location getLocation() {
        Location location = new Location("");
        location.setLatitude(centerLatitude);
        location.setLongitude(centerLongitude);
        return location;
    }

    public int getHeightMapResourceId() {
        return heightMapResourceId;
    }

    public static TerrainConfig getConfigForLocation(double latitude, double longitude) {
        for (TerrainConfig terrainConfig : TerrainConfig.values()) {
            if (isLocationInsideBounds(terrainConfig, latitude, longitude)) {
                return terrainConfig;
            }
        }

        return KAUFFUNGER_WALD_1;
    }

    private static boolean isLocationInsideBounds(TerrainConfig terrainConfig, double latitude, double longitude) {
        Location boundStartLocation = new Location("");
        boundStartLocation.setLatitude(terrainConfig.centerLatitude + GeoUtil.DELTA_LATITUDE / 2f);
        boundStartLocation.setLongitude(terrainConfig.centerLongitude - GeoUtil.DELTA_LONGITUDE / 2f);

        Location boundEndLocation = new Location("");
        boundEndLocation.setLatitude(terrainConfig.centerLatitude - GeoUtil.DELTA_LATITUDE / 2f);
        boundEndLocation.setLongitude(terrainConfig.centerLongitude + GeoUtil.DELTA_LONGITUDE / 2f);

        Location location = new Location("");
        location.setLatitude(latitude);
        location.setLongitude(longitude);

        return GeoUtil.isLocationInsideBounds(location, boundStartLocation, boundEndLocation);
    }
}
