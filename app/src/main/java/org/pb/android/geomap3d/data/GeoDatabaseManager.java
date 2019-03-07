package org.pb.android.geomap3d.data;

import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EBean;

@EBean(scope = EBean.Scope.Singleton)
public class GeoDatabaseManager {

    @Bean
    GeoDatabaseDao geoDatabaseDao;


}
