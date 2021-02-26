package org.pb.android.geomap3d.data.persist.geoplace;

import java.util.ArrayList;
import java.util.List;

public class GeoPlaces {

    private final String areaName;
    private final List<GeoPlace> geoPlaceList;

    public GeoPlaces(String areaName, List<GeoPlace> geoPlaceList) {
        this.areaName = areaName;
        this.geoPlaceList = geoPlaceList;
    }

    public int getCount() {
        return geoPlaceList.size();
    }

    public String getAreaName() {
        return areaName;
    }

    public List<GeoPlace> getGeoPlaceList() {
        return geoPlaceList == null ? new ArrayList<GeoPlace>() : geoPlaceList;
    }

    public boolean isEmpty() {
        return getGeoPlaceList().isEmpty();
    }
}
