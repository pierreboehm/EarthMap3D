package org.pb.android.geomap3d.data.map.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

public class GeoPlaceData {

    @JsonProperty("data")
    private List<GeoPlace> geoPlaces;

    public List<GeoPlace> getGeoPlaces() {
        return geoPlaces != null ? geoPlaces : new ArrayList<GeoPlace>();
    }
}
