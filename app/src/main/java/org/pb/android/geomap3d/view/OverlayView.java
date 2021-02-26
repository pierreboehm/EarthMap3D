package org.pb.android.geomap3d.view;

import android.content.Context;

import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import androidx.annotation.Nullable;


import org.androidannotations.annotations.EViewGroup;
import org.androidannotations.annotations.ViewById;
import org.pb.android.geomap3d.R;
import org.pb.android.geomap3d.data.persist.geoplace.GeoPlace;

@EViewGroup(R.layout.view_overlay)
public class OverlayView extends RelativeLayout {

    private static final String TAG = OverlayView.class.getSimpleName();

    @ViewById(R.id.itemContainer)
    ViewGroup itemContainer;

    private int maxViewCount = 0;
    private int addedViewsCount = 0;

    public OverlayView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public void setMaxViewCount(int maxViewCount) {
        this.maxViewCount = maxViewCount;
    }

    public void addInfoItem(GeoPlace geoPlace) {
        if (addedViewsCount >= maxViewCount) {
            return;
        }

        OverlayInfoItem infoItem = OverlayInfoItem_.build(getContext());
        infoItem.bind(geoPlace);

        itemContainer.addView(infoItem);
        addedViewsCount++;
    }

    public void cleanup() {
        itemContainer.removeAllViews();
        addedViewsCount = 0;
        maxViewCount = 0;
    }
}
