package org.pb.android.geomap3d.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.View;

import org.androidannotations.annotations.EView;
import org.pb.android.geomap3d.util.Util;

import androidx.annotation.Nullable;

@EView
public class ProgressView extends View {

    private static final String TAG = ProgressView.class.getSimpleName();
    private static final float STROKE_WIDTH = 60f;

    private Paint foregroundColor;
    private RectF clipBounds = null;
    private float progressValue = 0f;
    private float strokeWidth = STROKE_WIDTH;
    private int color = Color.argb(255, 0, 204, 255);
    private Handler handler;

    public ProgressView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
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

        super.onDraw(canvas);
    }

    public void setStrokeWidth(float strokeWidth) {
        this.strokeWidth = strokeWidth;
        foregroundColor.setStrokeWidth(strokeWidth);
    }

    public void setColor(int color) {
        this.color = color;
        foregroundColor.setColor(color);
    }

    public void startBlink() {
        progressValue = calculateProgressValue(100f);

        final boolean[] decrease = {true};
        final int[] alpha = {255};

        handler = new Handler();
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (decrease[0] && alpha[0] > 20) {
                    alpha[0] -= 5;
                    if (alpha[0] <= 20) {
                        decrease[0] = false;
                    }
                } else if (!decrease[0] && alpha[0] < 255) {
                    alpha[0] += 5;
                    if (alpha[0] >= 255) {
                        decrease[0] = true;
                    }
                }

                foregroundColor.setAlpha(alpha[0]);

                invalidate();
                handler.postDelayed(this, 20);
            }
        }).start();
    }

    public void stopBlink() {
        if (handler != null) {
            foregroundColor.setAlpha(255);
            handler.removeCallbacksAndMessages(null);
            handler = null;
        }
    }

    public synchronized void update(float percentValue) {
        progressValue = calculateProgressValue(percentValue);
        invalidate();
    }

    private void initView() {
        foregroundColor = new Paint();
        foregroundColor.setColor(color);
        foregroundColor.setStrokeCap(Paint.Cap.BUTT);
        foregroundColor.setStyle(Paint.Style.STROKE);
        foregroundColor.setStrokeWidth(strokeWidth);
    }

    private float calculateProgressValue(float percentValue) {
        return (float) Util.roundScale(percentValue * 360f / 100f);
    }
}
