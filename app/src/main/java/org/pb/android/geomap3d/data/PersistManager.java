package org.pb.android.geomap3d.data;

import android.graphics.Bitmap;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EBean;
import org.pb.android.geomap3d.data.persist.geoarea.GeoArea;
import org.pb.android.geomap3d.data.persist.geoarea.GeoAreaDao;

import java.util.List;

@EBean(scope = EBean.Scope.Singleton)
public class PersistManager {

    @Bean
    GeoAreaDao geoAreaDao;

    public List<GeoArea> getAllGeoModels() {
        return geoAreaDao.getAll();
    }

    public GeoArea findGeoModelByLocation(LatLng location) {
        return geoAreaDao.findForLocation(location);
    }

    public GeoArea findGeoModelByName(String geoModelName) {
        return geoAreaDao.findByName(geoModelName);
    }

    public GeoArea storeGeoModel(Bitmap bitmap, LatLng centerOfMap, LatLngBounds areaBounds) {
        GeoArea geoArea = new GeoArea.Builder()
                .setBitmap(bitmap)
                .setBounds(areaBounds)
                .setCenterOfMap(centerOfMap)
                .build();

        geoArea.save();

        return geoArea;
    }

    public void deleteGeoModel(String geoModelName) {
        GeoArea geoArea = findGeoModelByName(geoModelName);
        if (geoArea != null) {
            geoArea.delete();
        }
    }
}
