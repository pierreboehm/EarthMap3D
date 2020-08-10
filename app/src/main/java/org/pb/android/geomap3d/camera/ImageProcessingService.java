package org.pb.android.geomap3d.camera;

import android.app.Service;
import android.content.Intent;
import android.media.Image;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.EService;

@EService
public class ImageProcessingService extends Service {

    public static final String TAG = ImageProcessingService.class.getSimpleName();

    private IBinder binder = new LocalBinder();
    private boolean imageBusy = false;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public boolean isImageProcessing() {
        return imageBusy;
    }

    @Background
    public void processImage(Image image) {
        Log.d(TAG, "received image for processing");
        imageBusy = true;

        // simulate processing
        try {
            Thread.sleep(10000);
        } catch (Exception ex) {

        }

        imageBusy = false;
    }

    public class LocalBinder extends Binder {
        public ImageProcessingService getService() {
            return ImageProcessingService.this;
        }
    }
}
