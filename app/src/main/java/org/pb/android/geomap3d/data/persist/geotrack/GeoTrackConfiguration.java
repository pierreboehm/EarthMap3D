package org.pb.android.geomap3d.data.persist.geotrack;

import com.raizlabs.android.dbflow.annotation.Database;

@Database(name = GeoTrackConfiguration.NAME, version = GeoTrackConfiguration.VERSION)
public class GeoTrackConfiguration {

    public static final String NAME = "GeoTrackDatabase";
    public static final int VERSION = 1;

}
