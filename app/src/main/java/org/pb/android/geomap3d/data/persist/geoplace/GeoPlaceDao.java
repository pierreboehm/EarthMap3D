package org.pb.android.geomap3d.data.persist.geoplace;

import android.content.Context;

import com.raizlabs.android.dbflow.config.FlowConfig;
import com.raizlabs.android.dbflow.config.FlowManager;

import org.androidannotations.annotations.AfterInject;
import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.RootContext;

@EBean
public class GeoPlaceDao {

    @RootContext
    Context context;

    @AfterInject
    void init() {
        FlowManager.init(new FlowConfig.Builder(context).build());
    }


}
