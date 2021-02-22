package org.pb.android.geomap3d.camera;

import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.Image;
import android.media.ImageReader;
import android.media.ThumbnailUtils;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.EService;
import org.pb.android.geomap3d.tensorflow.ImageUtils;

@EService
public class ImageProcessingService extends Service {

    public static final String TAG = ImageProcessingService.class.getSimpleName();

    private IBinder binder = new LocalBinder();
    private Bitmap latestBitmap;
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

    public synchronized Bitmap getLatestBitmap() {
        return latestBitmap;
    }

    @Background
    public void processImage(ImageReader imageReader, boolean isDetectorActive) {

        Log.d(TAG,"received image for processing");
        imageBusy = true;

        Image image = imageReader.acquireNextImage();
        if (image != null) {
            int width = image.getWidth();
            int height = image.getHeight();

            Log.d(TAG, "image dimension: " + width + "x" + height);
            Bitmap rgbFrameBitmap = extractBitmapFromImage(image);
            image.close();

            latestBitmap = rgbFrameBitmap;

            if (isDetectorActive) {
                // TODO: handle bitmap for detector logic
            }

        } else {
            Log.w(TAG, "processImage() image is NULL");
        }

        imageBusy = false;
    }

    public class LocalBinder extends Binder {
        public ImageProcessingService getService() {
            return ImageProcessingService.this;
        }
    }

    private Bitmap extractBitmapFromImage(Image image) {
        byte[][] yuvBytes = new byte[3][];
        Image.Plane[] planes = image.getPlanes();

        ImageUtils.fillBytes(planes, yuvBytes);

        int yRowStride = planes[0].getRowStride();
        int uvRowStride = planes[1].getRowStride();
        int uvPixelStride = planes[1].getPixelStride();

        int width = image.getWidth();
        int height = image.getHeight();

        int[] rgbBytes = new int[width * height];

        ImageUtils.convertYUV420ToARGB8888(yuvBytes[0], yuvBytes[1], yuvBytes[2], width, height, yRowStride, uvRowStride, uvPixelStride, rgbBytes);

        Bitmap rgbFrameBitmap =  Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        rgbFrameBitmap.setPixels(rgbBytes, 0, width, 0, 0, width, height);

        //return cropCenter(rgbFrameBitmap);
        return rgbFrameBitmap;
    }

    private Bitmap cropCenter(Bitmap bitmap) {
        int dimension = 384;    // FIXME: set expected tensorflow detector size
        return ThumbnailUtils.extractThumbnail(bitmap, dimension, dimension);
    }
}
