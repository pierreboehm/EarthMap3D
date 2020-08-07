package org.pb.android.geomap3d.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import androidx.annotation.Nullable;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EViewGroup;
import org.androidannotations.annotations.ViewById;
import org.pb.android.geomap3d.R;
import org.pb.android.geomap3d.compass.LowPassFilter;
import org.pb.android.geomap3d.util.Util;

@EViewGroup(R.layout.bionic_eye_view)
public class BionicEyeView extends RelativeLayout {

    public static final String TAG = BionicEyeView.class.getSimpleName();

    @ViewById(R.id.ivHorizon)
    ImageView ivHorizon;

    @ViewById(R.id.ivRect)
    ImageView ivRect;

    private float zRotation;

    public BionicEyeView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    @AfterViews
    public void initViews() {
        ivRect.setVisibility(INVISIBLE);
    }

    public void updateDeviceRotation(float azimuth) {
        // TODO: implement
    }

    public void updateDeviceOrientation(Util.Orientation orientation, float[] gravity) {
        int orientationBasedIndex = orientation == Util.Orientation.PORTRAIT ? 0 : 1;
        float smoothedZRotation = LowPassFilter.filter(gravity[orientationBasedIndex], zRotation, .05f);

        // FIXME: how to calculate the correct degree values? (360 is too much)
        //  Current formula only works (approximately) in LANDSCAPE_MODE!
        zRotation = smoothedZRotation;
        ivHorizon.setRotation(-zRotation * 100.f / 15.f);
    }
}
