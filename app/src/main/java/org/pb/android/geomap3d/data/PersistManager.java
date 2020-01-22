package org.pb.android.geomap3d.data;

import android.graphics.Bitmap;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

import org.androidannotations.annotations.AfterInject;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.sharedpreferences.Pref;
import org.androidannotations.annotations.sharedpreferences.SharedPref;
import org.pb.android.geomap3d.AppPreferences_;
import org.pb.android.geomap3d.data.persist.geoarea.GeoArea;
import org.pb.android.geomap3d.data.persist.geoarea.GeoAreaDao;
import org.pb.android.geomap3d.data.persist.geotrack.GeoTrack;
import org.pb.android.geomap3d.data.persist.geotrack.GeoTrackDao;

import java.util.List;

@EBean(scope = EBean.Scope.Singleton)
public class PersistManager {

    @Bean
    GeoAreaDao geoAreaDao;

    @Bean
    GeoTrackDao geoTrackDao;

    @Pref
    AppPreferences_ preferences;

    public List<GeoArea> getAllGeoAreas() {
        return geoAreaDao.getAll();
    }

    public GeoArea findGeoAreaByLocation(LatLng location) {
        return geoAreaDao.findForLocation(location);
    }

    public GeoArea findGeoAreaByName(String geoAreaName) {
        return geoAreaDao.findByName(geoAreaName);
    }

    public GeoArea storeGeoArea(Bitmap bitmap, LatLng centerOfMap, LatLngBounds areaBounds) {
        GeoArea geoArea = new GeoArea.Builder()
                .setHeightMap(bitmap)
                .setBounds(areaBounds)
                .setCenterOfMap(centerOfMap)
                .build();

        geoArea.save();

        return geoArea;
    }

    public void deleteGeoArea(String geoAreaName) {
        geoAreaDao.deleteArea(geoAreaName);
        geoTrackDao.deleteTracksOfArea(geoAreaName);

//        GeoArea geoArea = findGeoAreaByName(geoAreaName);
//        if (geoArea != null) {
//            geoArea.delete();
//        }
    }

    public List<GeoTrack> findGeoTracksForArea(String areaName) {
        return geoTrackDao.getTracksForArea(areaName);
    }

    public void storeGeoTrack(LatLng location) {
        int sessionId = preferences.lastSession().getOr(0);
        GeoTrack geoTrack = new GeoTrack(sessionId, location);
        geoTrack.save();
    }
}
