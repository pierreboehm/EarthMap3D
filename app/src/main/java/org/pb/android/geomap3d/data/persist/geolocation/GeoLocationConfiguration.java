package org.pb.android.geomap3d.data.persist.geolocation;

import com.raizlabs.android.dbflow.annotation.Database;

@Database(name = GeoLocationConfiguration.NAME, version = GeoLocationConfiguration.VERSION)
public class GeoLocationConfiguration {

    public static final String NAME = "GeoLocationDatabase";
    public static final int VERSION = 1;

}
