package org.pb.android.geomap3d.data.map.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class GeoPlaceResponse {

    @JsonProperty("data")
    private List<GeoPlace> geoPlaces;

    public List<GeoPlace> getGeoPlaces() {
        return geoPlaces;
    }
}
