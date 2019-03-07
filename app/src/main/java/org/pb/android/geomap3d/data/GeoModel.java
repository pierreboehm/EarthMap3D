package org.pb.android.geomap3d.data;

import com.raizlabs.android.dbflow.annotation.Column;
import com.raizlabs.android.dbflow.annotation.PrimaryKey;
import com.raizlabs.android.dbflow.annotation.Table;
import com.raizlabs.android.dbflow.structure.BaseModel;

import java.io.Serializable;

@Table(database = GeoDatabaseConfiguration.class)
public class GeoModel extends BaseModel implements Serializable {

    @Column
    @PrimaryKey(autoincrement = true)
    int id;

    @Column
    String name;

    public GeoModel() {
    }

    public GeoModel(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
