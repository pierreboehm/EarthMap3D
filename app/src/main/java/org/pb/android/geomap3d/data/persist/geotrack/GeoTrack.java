package org.pb.android.geomap3d.data.persist.geotrack;

import com.google.android.gms.maps.model.LatLng;
import com.raizlabs.android.dbflow.annotation.Column;
import com.raizlabs.android.dbflow.annotation.PrimaryKey;
import com.raizlabs.android.dbflow.annotation.Table;
import com.raizlabs.android.dbflow.structure.BaseModel;

import java.io.Serializable;

@Table(database = GeoTrackConfiguration.class)
public class GeoTrack extends BaseModel implements Serializable {

    @Column
    @PrimaryKey(autoincrement = true)
    int id;

    @Column
    double latitude;

    @Column
    double longitude;

    @Column
    long timestamp;

    @Column
    int sessionId;

    @Column
    String areaName;

    public GeoTrack() {
    }

    public GeoTrack(int sessionId, LatLng location) {
        this.sessionId = sessionId;
        latitude = location.latitude;
        longitude = location.longitude;
        timestamp = System.currentTimeMillis();
    }

    public LatLng getLocation() {
        return new LatLng(latitude, longitude);
    }

}
