package org.pb.android.geomap3d.data.route.model;

import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

import java.util.ArrayList;
import java.util.List;

@Root(name = "Routes")
public class Routes {

    @ElementList(inline = true)
    List<Route> routeList;

    public List<Route> getRouteList() {
        if (routeList == null) {
            routeList = new ArrayList<>();
        }
        return routeList;
    }
}
