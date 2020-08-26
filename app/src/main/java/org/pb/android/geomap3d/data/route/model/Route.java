package org.pb.android.geomap3d.data.route.model;

import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

import java.util.ArrayList;
import java.util.List;

@Root(name = "Route")
public class Route {

    @ElementList(inline = true)
    List<RoutePoint> routePointList;

    public List<RoutePoint> getRoutePointList() {
        if (routePointList == null) {
            routePointList = new ArrayList<>();
        }
        return routePointList;
    }
}
