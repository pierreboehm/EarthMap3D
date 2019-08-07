package org.pb.android.geomap3d.data.map.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class GeoPlace {

    @JsonProperty("city")
    private String city;
    @JsonProperty("country")
    private String country;
    @JsonProperty("countryCode")
    private String countryCode;
    @JsonProperty("distance")
    private Double distanceInKilometers;
    @JsonProperty("id")
    private Integer id;
    @JsonProperty("latitude")
    private Double latitude;
    @JsonProperty("longitude")
    private Double longitude;
    @JsonProperty("name")
    private String name;
    @JsonProperty("region")
    private String region;
    @JsonProperty("regionCode")
    private String regionCode;
    @JsonProperty("type")
    private String type;
    @JsonProperty("wikiDataId")
    private String wikiDataId;

    public String getCity() {
        return city;
    }

    public Double getDistanceInKilometers() {
        return distanceInKilometers;
    }

    public Double getLatitude() {
        return latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public String getName() {
        return name;
    }
}
