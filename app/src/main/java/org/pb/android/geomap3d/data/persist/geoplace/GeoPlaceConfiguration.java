package org.pb.android.geomap3d.data.persist.geoplace;

import com.raizlabs.android.dbflow.annotation.Database;

@Database(name = GeoPlaceConfiguration.NAME, version = GeoPlaceConfiguration.VERSION)
public class GeoPlaceConfiguration {

    public static final String NAME = "GeoPlaceDatabase";
    public static final int VERSION = 1;

}
