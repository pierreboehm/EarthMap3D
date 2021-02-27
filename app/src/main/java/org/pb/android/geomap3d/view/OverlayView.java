package org.pb.android.geomap3d.view;

import android.content.Context;

import android.location.Location;
import android.util.AttributeSet;

import android.widget.ListView;
import android.widget.RelativeLayout;

import androidx.annotation.Nullable;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EViewGroup;
import org.androidannotations.annotations.ViewById;
import org.pb.android.geomap3d.R;
import org.pb.android.geomap3d.data.persist.geoplace.GeoPlace;
import org.pb.android.geomap3d.data.persist.geoplace.GeoPlaces;

@EViewGroup(R.layout.view_overlay)
public class OverlayView extends RelativeLayout {

    private static final String TAG = OverlayView.class.getSimpleName();

    @ViewById(R.id.infoItemContainer)
    ListView itemContainer;

    @Bean
    OverlayViewAdapter overlayViewAdapter;

    public OverlayView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    @AfterViews
    public void initViews() {
        itemContainer.setDivider(null);
        itemContainer.setAdapter(overlayViewAdapter);
    }

    public void cleanup() {
        overlayViewAdapter.cleanup();
    }

    public void resetGeoPlaces(GeoPlaces geoPlaces) {
        overlayViewAdapter.setGeoPlaces(geoPlaces);
    }

    public void addGeoPlace(GeoPlace geoPlace) {
        overlayViewAdapter.addGeoPlace(geoPlace);
    }

    public void updateDeviceLocation(Location location) {
        overlayViewAdapter.updateLocation(location);
    }
}
