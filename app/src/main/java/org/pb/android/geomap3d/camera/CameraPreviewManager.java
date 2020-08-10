package org.pb.android.geomap3d.camera;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.RootContext;
import org.androidannotations.annotations.SystemService;
import org.pb.android.geomap3d.util.Util;

import java.util.Arrays;
import java.util.List;

@EBean(scope = EBean.Scope.Singleton)
public class CameraPreviewManager {

    public static final String TAG = CameraPreviewManager.class.getSimpleName();

    @RootContext
    Context context;

    @SystemService
    CameraManager cameraManager;

    private CameraDevice camera;
    private SurfaceView surfaceView;

    private HandlerThread backgroundHandlerThread;
    private Handler backgroundHandler;

    private CameraCaptureSession captureSession;
    private ImageReader captureBuffer;

    private Util.Orientation orientation;
    private static boolean imageBusy = false;


    public void resume(SurfaceView previewSurfaceView) {
        if (previewSurfaceView == null) {
            return;
        }

        // Start a background thread to manage camera requests
        backgroundHandlerThread = new HandlerThread("background");
        backgroundHandlerThread.start();
        backgroundHandler = new Handler(backgroundHandlerThread.getLooper());

        surfaceView = previewSurfaceView;
        surfaceView.getHolder().addCallback(surfaceHolderCallback);

        Log.d(TAG, "resumed");
    }


    public void pause() {
        try {
            // Ensure SurfaceHolderCallback#surfaceChanged() will run again if the user returns
            surfaceView.getHolder().setFixedSize(0, 0);

            if (captureSession != null) {
                captureSession.close();
                captureSession = null;
            }

        } finally {
            if (camera != null) {
                camera.close();
                camera = null;

                Log.d(TAG, "camera closed");
            }
        }

        // Finish processing posted messages, then join on the handling thread
        backgroundHandlerThread.quitSafely();
        try {
            backgroundHandlerThread.join();
        } catch (InterruptedException ex) {
            // implement
        }

        if (captureBuffer != null) {
            captureBuffer.close();
        }

        Log.d(TAG, "paused");
    }


    public void orientationChanged(Util.Orientation orientation) {
        this.orientation = orientation;
    }

    private String setSurfaceViewSize(int width, int height) {
        try {
            for (String cameraListId : cameraManager.getCameraIdList()) {
                CameraCharacteristics cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraListId);

                if (cameraCharacteristics.get(cameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK) {

                    StreamConfigurationMap info = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                    Size optimalSize = Util.chooseBigEnoughSize(info.getOutputSizes(SurfaceHolder.class), width, height);
                    surfaceView.getHolder().setFixedSize(optimalSize.getWidth(), optimalSize.getHeight());

                    Log.d(TAG, "surface new size: " + optimalSize.getWidth() + "x" + optimalSize.getHeight());

                    captureBuffer = ImageReader.newInstance(300, 300, ImageFormat.JPEG, 2);
                    captureBuffer.setOnImageAvailableListener(imageCaptureListener, backgroundHandler);

                    return cameraListId;
                }
            }
        } catch (Exception exception) {
            // implement
        }

        return null;
    }


    private final SurfaceHolder.Callback surfaceHolderCallback = new SurfaceHolder.Callback() {

        private String cameraId;
        /** Whether we received a change callback after setting our fixed surface size. */
        private boolean gotSecondCallback;

        @Override
        public void surfaceCreated(SurfaceHolder surfaceHolder) {
            cameraId = null;
            gotSecondCallback = false;
        }

        @Override
        public void surfaceChanged(SurfaceHolder surfaceHolder, int format, int height, int width) {
            if (cameraId == null) { // 1st time
                cameraId = setSurfaceViewSize(width, height);

            } else if (!gotSecondCallback) { // 2nd time
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }

                try {
                    cameraManager.openCamera(cameraId, cameraStateCallback, backgroundHandler);
                } catch (Exception exception) {
                    // implement
                } finally {
                    gotSecondCallback = true;
                }

            } else {    // any other times
                setSurfaceViewSize(width, height);
            }
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
            surfaceHolder.removeCallback(this);
        }
    };


    private final CameraDevice.StateCallback cameraStateCallback = new CameraDevice.StateCallback() {

        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            camera = cameraDevice;

            try {
                List<Surface> outputs = Arrays.asList(surfaceView.getHolder().getSurface(), captureBuffer.getSurface());
                camera.createCaptureSession(outputs, captureSessionListener, backgroundHandler);
            } catch (CameraAccessException ex) {
                Log.e(TAG, ex.getLocalizedMessage());
            }

            Log.d(TAG, "camera opened and capture session started on surfaceView");
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            // implement
            Log.d(TAG, "camera has been disconnected");
        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int i) {
            // implement
        }
    };


    private final CameraCaptureSession.StateCallback captureSessionListener = new CameraCaptureSession.StateCallback() {

        @Override
        public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
            captureSession = cameraCaptureSession;
            SurfaceHolder holder = surfaceView.getHolder();

            if (holder != null) {
                try {
                    // Build a request for preview footage
                    CaptureRequest.Builder requestBuilder = camera.createCaptureRequest(camera.TEMPLATE_PREVIEW);
                    requestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                    requestBuilder.addTarget(holder.getSurface());
                    CaptureRequest previewRequest = requestBuilder.build();

                    // Start displaying preview images
                    try {
                        captureSession.setRepeatingRequest(previewRequest, /*listener*/null, /*handler*/null);
                    } catch (CameraAccessException ex) {
                        // implement
                    }
                } catch (CameraAccessException ex) {
                    // implement
                }

                Log.d(TAG, "captured session started (repeating request)");
            }
            else {
                // implement
            }
        }

        @Override
        public void onClosed(CameraCaptureSession session) {
            captureSession = null;
        }

        @Override
        public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
            // implement
        }
    };


    private final CameraCaptureSession.CaptureCallback captureCallback = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
            super.onCaptureCompleted(session, request, result);
        }
    };


    private final ImageReader.OnImageAvailableListener imageCaptureListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {
            if (!imageBusy) {
                new Thread(new CapturedImageSaver(reader.acquireNextImage())).start();
            }
        }
    };


    /*@Background
    public void handleCapturedImage(final Image image) {
        imageBusy = true;
        Log.v(TAG, "got image. size: " + image.getWidth() + "x" + image.getHeight());

        try {
            Thread.sleep(1000);
        } catch (Exception exception) {
            // not implemented
        }

        image.close();
        imageBusy = false;
    }*/


    private static class CapturedImageSaver implements Runnable {
        private Image image;

        public CapturedImageSaver(Image image) {
            this.image = image;
            imageBusy = true;
        }

        @Override
        public void run() {
            Log.v(TAG, "got image. size: " + image.getWidth() + "x" + image.getHeight());

            try {
                Thread.sleep(1000);
            } catch (Exception exception) {
                // not implemented
            }

            image.close();

            imageBusy = false;
        }
    }
}
