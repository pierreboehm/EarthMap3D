package org.pb.android.geomap3d.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.RelativeLayout;

import androidx.annotation.Nullable;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EViewGroup;
import org.pb.android.geomap3d.R;

@EViewGroup(R.layout.bionic_eye_view)
public class BionicEyeView extends RelativeLayout {

    public BionicEyeView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    @AfterViews
    public void initViews() {
    }
}
