package org.pb.android.geomap3d.data.persist.geoplace;

import android.content.Context;

import com.raizlabs.android.dbflow.config.FlowConfig;
import com.raizlabs.android.dbflow.config.FlowManager;
import com.raizlabs.android.dbflow.sql.language.SQLite;

import org.androidannotations.annotations.AfterInject;
import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.RootContext;

import java.util.List;

@EBean
public class GeoPlaceDao {

    @RootContext
    Context context;

    @AfterInject
    void init() {
        FlowManager.init(new FlowConfig.Builder(context).build());
    }

    public GeoPlaces getGeoPlacesForArea(String areaName) {
        List<GeoPlace> geoPlaceList = SQLite.select().from(GeoPlace.class).where(GeoPlace_Table.areaName.eq(areaName)).queryList();
        return new GeoPlaces(areaName, geoPlaceList);
    }
}
