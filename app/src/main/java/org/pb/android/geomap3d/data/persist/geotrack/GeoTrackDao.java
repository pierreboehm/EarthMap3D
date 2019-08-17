package org.pb.android.geomap3d.data.persist.geotrack;

import android.content.Context;

import com.raizlabs.android.dbflow.config.FlowConfig;
import com.raizlabs.android.dbflow.config.FlowManager;
import com.raizlabs.android.dbflow.sql.language.SQLite;

import org.androidannotations.annotations.AfterInject;
import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.RootContext;

import java.util.List;

@EBean
public class GeoTrackDao {

    @RootContext
    Context context;

    @AfterInject
    void init() {
        FlowManager.init(new FlowConfig.Builder(context).build());
    }

    public List<GeoTrack> getTracksForArea(String areaName) {
         return SQLite.select().from(GeoTrack.class).where(GeoTrack_Table.areaName.eq(areaName)).queryList();
    }
}
