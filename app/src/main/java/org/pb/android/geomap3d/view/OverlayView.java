package org.pb.android.geomap3d.view;

import android.content.Context;

import android.location.Location;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import androidx.annotation.Nullable;


import com.google.android.gms.maps.model.LatLng;

import org.androidannotations.annotations.EViewGroup;
import org.androidannotations.annotations.ViewById;
import org.pb.android.geomap3d.R;
import org.pb.android.geomap3d.data.persist.geoplace.GeoPlace;
import org.pb.android.geomap3d.data.persist.geoplace.GeoPlaces;
import org.pb.android.geomap3d.util.GeoUtil;

import java.util.ArrayList;
import java.util.List;

@EViewGroup(R.layout.view_overlay)
public class OverlayView extends RelativeLayout {

    private static final String TAG = OverlayView.class.getSimpleName();

    @ViewById(R.id.itemContainer)
    ViewGroup itemContainer;

    private List<GeoPlace> geoPlaceList = new ArrayList<>();

    private int maxViewCount = 0;
    private int addedViewsCount = 0;

    private boolean isUpdating = false;
    private boolean isResetting = false;

    public OverlayView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public void resetGeoPlaces(GeoPlaces geoPlaces) {
        cleanup();

        isResetting = true;
        maxViewCount = geoPlaces.getCount();

        for (GeoPlace geoPlace : geoPlaces.getGeoPlaceList()) {
            addInfoItem(geoPlace);
        }

        isResetting = false;
    }

    public void setMaxViewCount(int maxViewCount) {
        this.maxViewCount = maxViewCount;
    }

    public void addInfoItem(GeoPlace geoPlace) {
        if (addedViewsCount >= maxViewCount || isUpdating) {
            return;
        }

        geoPlaceList.add(geoPlace);

        OverlayInfoItem infoItem = OverlayInfoItem_.build(getContext());
        infoItem.bind(geoPlace);

        itemContainer.addView(infoItem);
        addedViewsCount++;
    }

    public void cleanup() {
        itemContainer.removeAllViews();
        geoPlaceList.clear();

        addedViewsCount = 0;
        maxViewCount = 0;
    }

    public void updateDeviceLocation(Location location) {
        if (isResetting) {
            return;
        }

        isUpdating = true;

        itemContainer.removeAllViews();
        addedViewsCount = 0;

        for (GeoPlace geoPlace : geoPlaceList) {
            geoPlace.setDistance(getDistance(location, geoPlace.getLocation()));

            OverlayInfoItem infoItem = OverlayInfoItem_.build(getContext());
            infoItem.bind(geoPlace);

            itemContainer.addView(infoItem);
            addedViewsCount++;
        }

        isUpdating = false;
    }

    private double getDistance(Location startLocation, Location endLocation) {
        LatLng startPoint = GeoUtil.getLatLngFromLocation(startLocation);
        LatLng endPoint = GeoUtil.getLatLngFromLocation(endLocation);
        return (double) (GeoUtil.getDistanceBetweenTwoPointsInMeter(startPoint, endPoint) / 1000.00);
    }
}
