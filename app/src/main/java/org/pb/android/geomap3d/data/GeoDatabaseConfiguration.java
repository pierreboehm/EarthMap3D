package org.pb.android.geomap3d.data;

import com.raizlabs.android.dbflow.annotation.Database;

@Database(name = GeoDatabaseConfiguration.NAME, version = GeoDatabaseConfiguration.VERSION)
public class GeoDatabaseConfiguration {

    public static final String NAME = "GeoDatabase";
    public static final int VERSION = 1;

}
