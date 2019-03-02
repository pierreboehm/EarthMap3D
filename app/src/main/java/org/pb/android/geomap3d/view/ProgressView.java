package org.pb.android.geomap3d.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import org.androidannotations.annotations.EView;
import org.greenrobot.eventbus.EventBus;
import org.pb.android.geomap3d.event.Events;
import org.pb.android.geomap3d.util.Util;

@EView
public class ProgressView extends View {

    private static final String TAG = ProgressView.class.getSimpleName();
    private static final float STROKE_WIDTH = 120f;

    private Paint foregroundColor;
    private RectF clipBounds = null;
    private float progressValue = 0f;

    public ProgressView(Context context) {
        this(context, null, 0);
    }

    public ProgressView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ProgressView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (clipBounds == null) {
            Rect canvasClipBounds = canvas.getClipBounds();
            float halfStrokeWidth = STROKE_WIDTH / 2f;
            clipBounds = new RectF(canvasClipBounds.left + halfStrokeWidth, canvasClipBounds.top + halfStrokeWidth,
                    canvasClipBounds.right - halfStrokeWidth, canvasClipBounds.bottom - halfStrokeWidth);
        }

        canvas.rotate(-90f, clipBounds.centerX(), clipBounds.centerY());
        canvas.drawArc(clipBounds, 0f, progressValue, false, foregroundColor);

        Log.v(TAG, "onDraw()");

        super.onDraw(canvas);
    }

    public void update(float percentValue) {
        progressValue = calculateProgressValue(percentValue);
//        invalidate();

        Log.v(TAG, ">> update(): " + percentValue + "%, " + progressValue + "°, visible=" + (getVisibility() == VISIBLE));

        if (percentValue >= 100f) {
            EventBus.getDefault().post(new Events.WidgetReady());
        }
    }

    private void initView() {
        foregroundColor = new Paint();
        foregroundColor.setColor(Color.argb(96, 0, 204, 255));
        foregroundColor.setStrokeCap(Paint.Cap.BUTT);
        foregroundColor.setStyle(Paint.Style.STROKE);
        foregroundColor.setStrokeWidth(STROKE_WIDTH);
    }

    private float calculateProgressValue(float percentValue) {
        return (float) Util.roundScale(percentValue * 360f / 100f);
    }
}
