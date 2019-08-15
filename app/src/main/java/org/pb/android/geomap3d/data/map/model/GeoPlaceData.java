package org.pb.android.geomap3d.data.map.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

public class GeoPlaceData {

    @JsonProperty("data")
    private List<GeoPlaceItem> geoPlaceItems;

    public List<GeoPlaceItem> getGeoPlaceItems() {
        return geoPlaceItems != null ? geoPlaceItems : new ArrayList<GeoPlaceItem>();
    }
}
