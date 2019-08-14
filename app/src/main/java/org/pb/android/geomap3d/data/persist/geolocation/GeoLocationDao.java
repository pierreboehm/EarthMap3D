package org.pb.android.geomap3d.data.persist.geolocation;

import android.content.Context;

import com.google.android.gms.maps.model.LatLng;
import com.raizlabs.android.dbflow.config.FlowConfig;
import com.raizlabs.android.dbflow.config.FlowManager;
import com.raizlabs.android.dbflow.sql.language.SQLite;

import org.androidannotations.annotations.AfterInject;
import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.RootContext;

import java.util.List;

import androidx.annotation.Nullable;

@EBean
public class GeoLocationDao {

    @RootContext
    Context context;

    @AfterInject
    void init() {
        FlowManager.init(new FlowConfig.Builder(context).build());
    }

    public List<GeoLocation> getAll() {
        return SQLite.select().from(GeoLocation.class).orderBy(GeoLocation_Table.id, true).queryList();
    }

    @Nullable
    public GeoLocation findForLocation(LatLng location) {
        return SQLite.select()
                .from(GeoLocation.class)
                .where(GeoLocation_Table.northEastLatitude.greaterThan(location.latitude))
                .and(GeoLocation_Table.northEastLongitude.greaterThan(location.longitude))
                .and(GeoLocation_Table.southWestLatitude.lessThan(location.latitude))
                .and(GeoLocation_Table.southWestLongitude.lessThan(location.longitude))
                .querySingle();
    }

    public GeoLocation findByName(String geoModelName) {
        return SQLite.select().from(GeoLocation.class).where(GeoLocation_Table.name.eq(geoModelName)).querySingle();
    }
}
