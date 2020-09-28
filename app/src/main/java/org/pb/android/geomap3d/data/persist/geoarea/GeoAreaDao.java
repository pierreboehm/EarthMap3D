package org.pb.android.geomap3d.data.persist.geoarea;

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
public class GeoAreaDao {

    @RootContext
    Context context;

    @AfterInject
    void init() {
        FlowManager.init(new FlowConfig.Builder(context).build());
    }

    public List<GeoArea> getAll() {
        return SQLite.select().from(GeoArea.class).orderBy(GeoArea_Table.id, true).queryList();
    }

    @Nullable
    public GeoArea findForLocation(LatLng location) {
        return SQLite.select()
                .from(GeoArea.class)
                .where(GeoArea_Table.northEastLatitude.greaterThan(location.latitude))
                .and(GeoArea_Table.northEastLongitude.greaterThan(location.longitude))
                .and(GeoArea_Table.southWestLatitude.lessThan(location.latitude))
                .and(GeoArea_Table.southWestLongitude.lessThan(location.longitude))
                .querySingle();
    }

    public GeoArea findByName(String geoModelName) {
        return SQLite.select().from(GeoArea.class).where(GeoArea_Table.name.eq(geoModelName)).querySingle();
    }

    public void deleteArea(String areaName) {
        // FIXME: delete operation is just temporarily! After reload app all deleted data are still available.
        // SQLite.delete() need to open writeableDatabase instance!
        SQLite.delete().from(GeoArea.class).where(GeoArea_Table.name.eq(areaName));

        /* !!! JUST FOR TEST PURPOSES !!!
        DatabaseWrapper databaseWrapper = FlowManager.getWritableDatabase(GeoArea.class);
        databaseWrapper.execSQL("DELETE FROM " + GeoAreaConfiguration.NAME + " WHERE " + GeoArea_Table.name + " = " + areaName);
         */
    }
}
