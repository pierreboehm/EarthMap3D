package org.pb.android.geomap3d.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import org.androidannotations.annotations.EViewGroup;
import org.androidannotations.annotations.ViewById;
import org.pb.android.geomap3d.R;
import org.pb.android.geomap3d.data.persist.geoplace.GeoPlace;

import java.util.Locale;

@EViewGroup(R.layout.view_overlay_info_item)
public class OverlayInfoItem extends RelativeLayout {

    @ViewById(R.id.tvName)
    TextView tvName;

    @ViewById(R.id.tvDistance)
    TextView tvDistance;

    public OverlayInfoItem(Context context) {
        this(context, null);
    }

    public OverlayInfoItem(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public void bind(GeoPlace geoPlace) {
        tvName.setText(geoPlace.getName());
        updateDistance(geoPlace.getDistance());
    }

    public void updateDistance(double distance) {
        if (distance >= 1.0) {
            tvDistance.setText(String.format(Locale.US, "%.1f km", distance));
        } else {
            tvDistance.setText(String.format(Locale.US, "%d m", (int) (distance * 1000)));
        }
    }
}
