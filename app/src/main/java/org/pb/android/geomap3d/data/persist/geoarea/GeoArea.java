package org.pb.android.geomap3d.data.persist.geoarea;

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

@Table(database = GeoAreaConfiguration.class)
public class GeoArea extends BaseModel implements Serializable {

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

    public GeoArea() {
    }

    public String getName() {
        return name;
    }

    public Location getCenterPoint() {
        return GeoUtil.getLocation(centerPointLatitude, centerPointLongitude);
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
        return GeoUtil.getLocation(centerPointLatitude + GeoUtil.DELTA_LATITUDE / 2.0,
                centerPointLongitude - GeoUtil.DELTA_LONGITUDE / 2.0);
    }

    public Location getBoxEndPoint() {
        return GeoUtil.getLocation(centerPointLatitude - GeoUtil.DELTA_LATITUDE / 2.0,
                centerPointLongitude + GeoUtil.DELTA_LONGITUDE / 2.0);
    }

    private String convertBitmapToString(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG,100, byteArrayOutputStream);
        byte [] byteArray = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(byteArray, Base64.DEFAULT);
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

        private GeoArea geoArea;

        public Builder() {
            this.geoArea = new GeoArea();
        }

        public Builder setCenterOfMap(LatLng centerOfMap) {
            this.geoArea.centerPointLatitude = centerOfMap.latitude;
            this.geoArea.centerPointLongitude = centerOfMap.longitude;
            return this;
        }

        public Builder setBounds(LatLngBounds bounds) {
            this.geoArea.northEastLatitude = bounds.northeast.latitude;
            this.geoArea.northEastLongitude = bounds.northeast.longitude;
            this.geoArea.southWestLatitude = bounds.southwest.latitude;
            this.geoArea.southWestLongitude = bounds.southwest.longitude;

            return this;
        }

        public Builder setHeightMap(Bitmap bitmap) {
            this.geoArea.heightMapBitmap = this.geoArea.convertBitmapToBlob(bitmap);
            return this;
        }

        public GeoArea build() {
            if (this.geoArea.name == null) {
                this.geoArea.name = Long.toString(System.currentTimeMillis());
            }
            return this.geoArea;
        }

    }
}
