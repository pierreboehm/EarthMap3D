package org.pb.android.geomap3d.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import androidx.annotation.Nullable;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EView;

@EView
public class CompassView extends View {

    private static final String TAG = CompassView.class.getSimpleName();

    private Rect clipBounds = null;
    private Bitmap scala;

    private float deltaX = Float.MIN_VALUE;

    public CompassView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    @SuppressLint("DrawAllocation")
    @Override
    protected void onDraw(Canvas canvas) {
        if (clipBounds == null) {
            clipBounds = canvas.getClipBounds();
            scala = generateScala(clipBounds.width() * 2, clipBounds.height());
            // TODO
            // 1) calculate initial deltaX
            // 2) draw scala onto canvas
            // 3) save current matrix
            // 4) call super.onDraw(canvas)
            // 5) return
        }

        if (deltaX != Float.MIN_VALUE) {
            // restore current matrix
            canvas.drawBitmap(scala, deltaX, 0, null);
        }

        super.onDraw(canvas);
    }

    public void updateRotation(float azimuth) {
        float radian = (float) (Math.PI * azimuth / 360f);
        deltaX = (float) (scala.getWidth() / 2 * Math.cos(radian));
        Log.v(TAG, "azimuth: " + azimuth + ", deltaX: " + deltaX);
        invalidate();
    }

    private Bitmap generateScala(int width, int height) {
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        int bottom = bitmap.getHeight() - 5;

        // draw scala
        for (int angle = 0; angle < 360; angle += 2) {
            int x = angle * bitmap.getWidth() / 360;
            int h = angle % 10 == 0 ? 5 : height / 2;

            for (int y = h; y < bottom; y++) {
                bitmap.setPixel(x, y, Color.WHITE);
            }
        }

        return bitmap;
    }
}
