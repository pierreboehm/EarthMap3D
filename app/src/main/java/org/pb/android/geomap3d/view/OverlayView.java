package org.pb.android.geomap3d.view;

import android.content.Context;
import android.graphics.Bitmap;

import android.util.AttributeSet;
import androidx.annotation.Nullable;

import org.androidannotations.annotations.EView;

@EView
public class OverlayView extends androidx.appcompat.widget.AppCompatImageView {

    public OverlayView(Context context) {
        this(context, null, 0);
    }

    public OverlayView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public OverlayView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void bind(Bitmap bitmap) {
        setImageBitmap(bitmap);
    }
}
