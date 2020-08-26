package org.pb.android.geomap3d.data.route.model;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Root;

@Root(name = "RoutePoint")
public class RoutePoint {

    @Attribute(name="latitude")
    String latitude;

    @Attribute(name="longitude")
    String longitude;

    public double getLongitude() {
        return Double.parseDouble(longitude);
    }

    public double getLatitude() {
        return Double.parseDouble(latitude);
    }
}
