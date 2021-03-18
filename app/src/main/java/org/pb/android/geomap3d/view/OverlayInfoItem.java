package org.pb.android.geomap3d.view;

import android.content.Context;
import android.util.AttributeSet;

import android.view.MotionEvent;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EViewGroup;
import org.androidannotations.annotations.ViewById;
import org.pb.android.geomap3d.R;
import org.pb.android.geomap3d.data.persist.geoplace.GeoPlace;
import org.pb.android.geomap3d.util.Util;

import java.util.Locale;

@EViewGroup(R.layout.view_overlay_info_item)
public class OverlayInfoItem extends RelativeLayout {

    private static final String TAG = OverlayInfoItem.class.getSimpleName();

    @ViewById(R.id.tvName)
    TextView tvName;

    @ViewById(R.id.tvDistance)
    TextView tvDistance;

    @ViewById(R.id.ivSelected)
    ImageView ivSelected;

    private GeoPlace geoPlace;
    private float defaultNameTextSize;
    private float defaultDistanceTextSize;
    private int defaultTextColor;

    public OverlayInfoItem(Context context) {
        this(context, null);
    }

    public OverlayInfoItem(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    @AfterViews
    public void initViews() {
        defaultNameTextSize = Util.convertPixelsToSp(getContext(), tvName.getTextSize());
        defaultDistanceTextSize = Util.convertPixelsToSp(getContext(), tvDistance.getTextSize());
        defaultTextColor = tvName.getCurrentTextColor();
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return true;
    }

    public void bind(GeoPlace geoPlace) {
        this.geoPlace = geoPlace;

        select(geoPlace.isSelected());
        tvName.setText(geoPlace.getName());

        double distance = geoPlace.getDistance();
        if (distance >= 1.0) {
            tvDistance.setText(String.format(Locale.US, "%.1f km", distance));
        } else {
            tvDistance.setText(String.format(Locale.US, "%d m", (int) (distance * 1000)));
        }
    }

    public void select(boolean select) {
        ivSelected.setVisibility(select ? VISIBLE : INVISIBLE);

        if (select) {
            int selectColor = getContext().getColor(R.color.warm_blue);
            tvName.setTextSize(defaultNameTextSize + (defaultNameTextSize * .2f));
            tvName.setTextColor(selectColor);
            tvDistance.setTextSize(defaultDistanceTextSize + (defaultDistanceTextSize * .2f));
            tvDistance.setTextColor(selectColor);
        } else {
            tvName.setTextSize(defaultNameTextSize);
            tvName.setTextColor(defaultTextColor);
            tvDistance.setTextSize(defaultDistanceTextSize);
            tvDistance.setTextColor(defaultTextColor);
        }
    }
}
