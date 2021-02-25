package org.pb.android.geomap3d.data.persist.geoplace;

import android.location.Location;

import com.raizlabs.android.dbflow.annotation.Column;
import com.raizlabs.android.dbflow.annotation.PrimaryKey;
import com.raizlabs.android.dbflow.annotation.Table;
import com.raizlabs.android.dbflow.structure.BaseModel;

import org.pb.android.geomap3d.util.GeoUtil;

import java.io.Serializable;

@Table(database = GeoPlaceConfiguration.class)
public class GeoPlace extends BaseModel implements Serializable {

    @Column
    @PrimaryKey(autoincrement = true)
    int id;

    @Column
    int placeId;

    @Column
    double latitude;

    @Column
    double longitude;

    @Column
    String city;

    @Column
    String country;

    @Column
    String countryCode;

    @Column
    double distance;

    @Column
    String name;

    @Column
    String region;

    @Column
    String regionCode;

    @Column
    String type;

    public GeoPlace() {
    }

    public int getId() {
        return placeId;
    }

    public String getCity() {
        return city;
    }

    public String getName() {
        return name;
    }

    public String getRegion() {
        return region;
    }

    public String getRegionCode() {
        return regionCode;
    }

    public String getType() {
        return type;
    }

    public Location getLocation() {
        return GeoUtil.getLocation(latitude, longitude);
    }

    public double getDistance() {
        return distance;
    }

    public static class Builder {

        private GeoPlace geoPlace;

        public Builder() {
            this.geoPlace = new GeoPlace();
        }

        public Builder setName(String name) {
            geoPlace.name = name;
            return this;
        }

        public Builder setCity(String city) {
            geoPlace.city = city;
            return this;
        }

        public Builder setCountry(String country) {
            geoPlace.country = country;
            return this;
        }

        public Builder setRegion(String region) {
            geoPlace.region = region;
            return this;
        }

        public Builder setRegionCode(String regionCode) {
            geoPlace.regionCode = regionCode;
            return this;
        }

        public Builder setType(String type) {
            geoPlace.type = type;
            return this;
        }

        public Builder setDistance(double distance) {
            geoPlace.distance = distance;
            return this;
        }

        public Builder setLocation(double latitude, double longitude) {
            geoPlace.latitude = latitude;
            geoPlace.longitude = longitude;
            return this;
        }

        public Builder setId(int id) {
            geoPlace.placeId = id;
            return this;
        }

        public GeoPlace build() {
            return this.geoPlace;
        }
    }
}
