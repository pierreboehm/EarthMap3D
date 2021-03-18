package org.pb.android.geomap3d.view;

import android.content.Context;
import android.location.Location;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.google.android.gms.maps.model.LatLng;

import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.RootContext;
import org.greenrobot.eventbus.EventBus;
import org.pb.android.geomap3d.data.persist.geoplace.GeoPlace;
import org.pb.android.geomap3d.data.persist.geoplace.GeoPlaces;
import org.pb.android.geomap3d.event.Events;
import org.pb.android.geomap3d.util.GeoUtil;

import java.util.ArrayList;
import java.util.List;

@EBean
public class OverlayViewAdapter extends BaseAdapter {

    private static final String TAG = OverlayViewAdapter.class.getSimpleName();

    @RootContext
    Context context;

    private List<GeoPlace> geoPlaceList = new ArrayList<>();

    @Override
    public int getCount() {
        return geoPlaceList.size();
    }

    @Override
    public GeoPlace getItem(int index) {
        return geoPlaceList.get(index);
    }

    @Override
    public long getItemId(int index) {
        return index;
    }

    @Override
    public View getView(final int index, View convertView, ViewGroup parent) {
        OverlayInfoItem infoItem;

        if (convertView == null) {
            infoItem = OverlayInfoItem_.build(context);
            infoItem.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    setSelection(index);
                }
            });
        } else {
            infoItem = (OverlayInfoItem) convertView;
        }

        infoItem.bind(getItem(index));
        return infoItem;
    }

    public void cleanup() {
        if (!geoPlaceList.isEmpty()) {
            geoPlaceList.clear();
        }
    }

    public void addGeoPlace(GeoPlace geoPlace) {
        if (!geoPlaceList.contains(geoPlace)) {
            geoPlaceList.add(geoPlace);
            notifyDataSetChanged();
        }
    }

    public void setGeoPlaces(GeoPlaces geoPlaces) {
        if (!geoPlaceList.isEmpty()) {
            geoPlaceList.clear();
        }

        geoPlaceList.addAll(geoPlaces.getGeoPlaceList());
        notifyDataSetChanged();
    }

    public void updateLocation(Location location) {
        for (GeoPlace geoPlace : geoPlaceList) {
            geoPlace.setDistance(getDistance(location, geoPlace.getLocation()));
        }
        notifyDataSetChanged();
    }

    private double getDistance(Location startLocation, Location endLocation) {
        LatLng startPoint = GeoUtil.getLatLngFromLocation(startLocation);
        LatLng endPoint = GeoUtil.getLatLngFromLocation(endLocation);
        return (double) (GeoUtil.getDistanceBetweenTwoPointsInMeter(startPoint, endPoint) / 1000.00);
    }

    private void setSelection(int indexSelected) {
        for (int index = 0; index < geoPlaceList.size(); index++) {
            boolean selected = indexSelected == index;
            GeoPlace geoPlace = getItem(index);
            geoPlace.setSelected(selected);

            if (selected) {
                EventBus.getDefault().post(new Events.TargetSelected(geoPlace));
            }
        }
        notifyDataSetChanged();
    }
}
