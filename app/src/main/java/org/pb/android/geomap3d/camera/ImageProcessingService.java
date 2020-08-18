package org.pb.android.geomap3d.camera;

import android.app.Service;
import android.content.Intent;
import android.media.Image;
import android.media.ImageReader;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.util.Size;

import androidx.annotation.Nullable;

import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.EService;

@EService
public class ImageProcessingService extends Service {

    public static final String TAG = ImageProcessingService.class.getSimpleName();

    private IBinder binder = new LocalBinder();
    private boolean imageBusy = false;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate()");
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy()");
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public boolean isImageProcessing() {
        return imageBusy;
    }

    /*public void setPreviewSize(Size previewSize) {
        this.previewSize = previewSize;
        Log.i(TAG, "preview size set: " + previewSize);
    }*/

    @Background
    public void processImage(ImageReader imageReader) {

        Log.d(TAG,"received image for processing");
        imageBusy = true;

        Image image = imageReader.acquireNextImage();
        if (image != null) {
            Log.d(TAG, "image dimension: " + image.getWidth() + "x" + image.getHeight());
            image.close();
        } else {
            Log.w(TAG, "processImage() image is NULL");
        }

        // simulate processing
        try {
            Thread.sleep(200);
        } catch (Exception ex) {

        }

        imageBusy = false;
        return;
    }

    public class LocalBinder extends Binder {
        public ImageProcessingService getService() {
            return ImageProcessingService.this;
        }
    }
}
