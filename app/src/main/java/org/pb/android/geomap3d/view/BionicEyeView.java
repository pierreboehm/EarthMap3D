package org.pb.android.geomap3d.view;

import android.content.Context;
import android.location.Location;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import androidx.annotation.Nullable;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EViewGroup;
import org.androidannotations.annotations.ViewById;
import org.pb.android.geomap3d.R;

@EViewGroup(R.layout.bionic_eye_view)
public class BionicEyeView extends RelativeLayout {

    public static final String TAG = BionicEyeView.class.getSimpleName();

    @ViewById(R.id.ivHorizon)
    ImageView ivHorizon;

    @ViewById(R.id.ivRect)
    ImageView ivRect;

    public BionicEyeView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    @AfterViews
    public void initViews() {
    }

    public void setAutoFocusState(Integer afState) {
        boolean focused = afState == 2;
        ivRect.setSelected(focused);
    }

    public void updateDeviceLocation(Location location) {
        // TODO
    }

    public void updateDeviceRotation(float azimuth, float pitch, float roll) {
        // TODO
        ivHorizon.setRotation(-roll);
        ivRect.setRotation(-roll);
    }
}
