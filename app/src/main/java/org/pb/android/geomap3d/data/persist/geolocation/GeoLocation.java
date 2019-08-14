package org.pb.android.geomap3d.data.persist.geolocation;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.util.Base64;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.raizlabs.android.dbflow.annotation.Column;
import com.raizlabs.android.dbflow.annotation.PrimaryKey;
import com.raizlabs.android.dbflow.annotation.Table;
import com.raizlabs.android.dbflow.data.Blob;
import com.raizlabs.android.dbflow.structure.BaseModel;

import org.pb.android.geomap3d.util.GeoUtil;

import java.io.ByteArrayOutputStream;
import java.io.Serializable;

@Table(database = GeoLocationConfiguration.class)
public class GeoLocation extends BaseModel implements Serializable {

    @Column
    @PrimaryKey(autoincrement = true)
    int id;

    @Column
    String name;

    @Column
    double centerPointLatitude;

    @Column
    double centerPointLongitude;

    @Column
    double northEastLatitude;

    @Column
    double northEastLongitude;

    @Column
    double southWestLatitude;

    @Column
    double southWestLongitude;

    @Column
    Blob heightMapBitmap;

    public GeoLocation() {
    }

    public GeoLocation(String name, Location centerPoint, Bitmap heightMapBitmap) {
        this.name = name;
        this.centerPointLatitude = centerPoint.getLatitude();
        this.centerPointLongitude = centerPoint.getLongitude();
        this.heightMapBitmap = convertBitmapToBlob(heightMapBitmap);
    }

    public String getName() {
        return name;
    }

    public Location getCenterPoint() {
        Location centerPoint = new Location("");
        centerPoint.setLatitude(centerPointLatitude);
        centerPoint.setLongitude(centerPointLongitude);
        return centerPoint;
    }

    public LatLng getCenter() {
        return new LatLng(centerPointLatitude, centerPointLongitude);
    }

    public void setHeightMap(Bitmap heightMapBitmap) {
        this.heightMapBitmap = convertBitmapToBlob(heightMapBitmap);
    }

    public Bitmap getHeightMapBitmap() {
        return convertBlobToBitmap();
    }

    public boolean hasHeightMapBitmap() {
        return heightMapBitmap != null;
    }

    public Location getBoxStartPoint() {
        Location boxStartLocation = new Location("");
        boxStartLocation.setLatitude(centerPointLatitude + GeoUtil.DELTA_LATITUDE / 2.0);
        boxStartLocation.setLongitude(centerPointLongitude - GeoUtil.DELTA_LONGITUDE / 2.0);
        return boxStartLocation;
    }

    public Location getBoxEndPoint() {
        Location boxEndLocation = new Location("");
        boxEndLocation.setLatitude(centerPointLatitude - GeoUtil.DELTA_LATITUDE / 2.0);
        boxEndLocation.setLongitude(centerPointLongitude + GeoUtil.DELTA_LONGITUDE / 2.0);
        return boxEndLocation;
    }

    private String convertBitmapToString(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG,100, byteArrayOutputStream);
        byte [] byteArray = byteArrayOutputStream.toByteArray();
        String encodeToString = Base64.encodeToString(byteArray, Base64.DEFAULT);
        return encodeToString;
    }

    private Blob convertBitmapToBlob(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG,100, byteArrayOutputStream);
        byte [] byteArray = byteArrayOutputStream.toByteArray();
        return new Blob(byteArray);
    }

    private Bitmap convertBlobToBitmap() {
        byte[] encodeByte = heightMapBitmap.getBlob();
        return BitmapFactory.decodeByteArray(encodeByte, 0, encodeByte.length);
    }

    public static class Builder {

        private GeoLocation geoLocation;

        public Builder() {
            this.geoLocation = new GeoLocation();
        }

        public Builder setCenterOfMap(LatLng centerOfMap) {
            this.geoLocation.centerPointLatitude = centerOfMap.latitude;
            this.geoLocation.centerPointLongitude = centerOfMap.longitude;
            return this;
        }

        public Builder setBounds(LatLngBounds bounds) {
            this.geoLocation.northEastLatitude = bounds.northeast.latitude;
            this.geoLocation.northEastLongitude = bounds.northeast.longitude;
            this.geoLocation.southWestLatitude = bounds.southwest.latitude;
            this.geoLocation.southWestLongitude = bounds.southwest.longitude;

            return this;
        }

        public Builder setBitmap(Bitmap bitmap) {
            this.geoLocation.heightMapBitmap = this.geoLocation.convertBitmapToBlob(bitmap);
            return this;
        }

        public GeoLocation build() {
            if (this.geoLocation.name == null) {
                this.geoLocation.name = Long.toString(System.currentTimeMillis());
            }
            return this.geoLocation;
        }

    }
}
