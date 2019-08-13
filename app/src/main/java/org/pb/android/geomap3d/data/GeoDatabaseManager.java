package org.pb.android.geomap3d.data;

import android.graphics.Bitmap;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EBean;

import java.util.List;

@EBean(scope = EBean.Scope.Singleton)
public class GeoDatabaseManager {

    @Bean
    GeoDatabaseDao geoDatabaseDao;

    public List<GeoModel> getAllGeoModels() {
        return geoDatabaseDao.getAll();
    }

    public GeoModel findGeoModelByLocation(LatLng location) {
        return geoDatabaseDao.findForLocation(location);
    }

    public GeoModel findGeoModelByName(String geoModelName) {
        return geoDatabaseDao.findByName(geoModelName);
    }

    public GeoModel storeGeoModel(Bitmap bitmap, LatLng centerOfMap, LatLngBounds areaBounds) {
        GeoModel geoModel = new GeoModel.Builder()
                .setBitmap(bitmap)
                .setBounds(areaBounds)
                .setCenterOfMap(centerOfMap)
                .build();

        geoModel.save();

        return geoModel;
    }

    public void deleteGeoModel(String geoModelName) {
        GeoModel geoModel = findGeoModelByName(geoModelName);
        if (geoModel != null) {
            geoModel.delete();
        }
    }
}
