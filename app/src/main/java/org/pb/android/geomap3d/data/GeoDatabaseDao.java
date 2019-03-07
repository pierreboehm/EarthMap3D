package org.pb.android.geomap3d.data;

import android.content.Context;

import com.raizlabs.android.dbflow.config.FlowConfig;
import com.raizlabs.android.dbflow.config.FlowManager;
import com.raizlabs.android.dbflow.sql.language.SQLite;

import org.androidannotations.annotations.AfterInject;
import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.RootContext;

import java.util.List;

@EBean
public class GeoDatabaseDao {

    @RootContext
    Context context;

    @AfterInject
    void init() {
        FlowManager.init(new FlowConfig.Builder(context).build());
    }

    public List<GeoModel> getAll() {
        return SQLite.select().from(GeoModel.class).orderBy(GeoModel_Table.id, true).queryList();
    }

}
