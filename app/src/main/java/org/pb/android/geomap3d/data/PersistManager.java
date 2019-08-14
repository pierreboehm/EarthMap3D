package org.pb.android.geomap3d.data;

import android.graphics.Bitmap;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EBean;
import org.pb.android.geomap3d.data.persist.geolocation.GeoLocation;
import org.pb.android.geomap3d.data.persist.geolocation.GeoLocationDao;

import java.util.List;

@EBean(scope = EBean.Scope.Singleton)
public class PersistManager {

    @Bean
    GeoLocationDao geoLocationDao;

    public List<GeoLocation> getAllGeoModels() {
        return geoLocationDao.getAll();
    }

    public GeoLocation findGeoModelByLocation(LatLng location) {
        return geoLocationDao.findForLocation(location);
    }

    public GeoLocation findGeoModelByName(String geoModelName) {
        return geoLocationDao.findByName(geoModelName);
    }

    public GeoLocation storeGeoModel(Bitmap bitmap, LatLng centerOfMap, LatLngBounds areaBounds) {
        GeoLocation geoLocation = new GeoLocation.Builder()
                .setBitmap(bitmap)
                .setBounds(areaBounds)
                .setCenterOfMap(centerOfMap)
                .build();

        geoLocation.save();

        return geoLocation;
    }

    public void deleteGeoModel(String geoModelName) {
        GeoLocation geoLocation = findGeoModelByName(geoModelName);
        if (geoLocation != null) {
            geoLocation.delete();
        }
    }
}
