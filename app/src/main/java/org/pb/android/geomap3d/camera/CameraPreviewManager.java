package org.pb.android.geomap3d.camera;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.math.MathUtils;

import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.RootContext;
import org.androidannotations.annotations.SystemService;
import org.greenrobot.eventbus.EventBus;
import org.pb.android.geomap3d.event.Events;
import org.pb.android.geomap3d.util.Util;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/*
    TODO:
    This singleton class is designated for being replaced by a TextureView.
    Benefits:
        * size parameters can be taken directly then from itself
        * ...
 */

@EBean(scope = EBean.Scope.Singleton)
public class CameraPreviewManager {

    public static final String TAG = CameraPreviewManager.class.getSimpleName();
    public static final int CROP_REGION_XY = 512;
    public static final float DEFAULT_ZOOM_FACTOR = 1f;

    @RootContext
    Context context;

    @SystemService
    CameraManager cameraManager;

    private ImageProcessingService imageProcessingService;

    private String cameraId;
    private CameraDevice camera;
    private SurfaceView surfaceView;

    private HandlerThread backgroundHandlerThread;
    private Handler backgroundHandler;

    private CaptureRequest.Builder captureRequestBuilder;
    private CameraCaptureSession captureSession;
    private ImageReader captureBuffer;

    private Util.Orientation orientation;
    private State captureState = State.STATE_PREVIEW;
    private static boolean imageBusy = false;

    private boolean isZoomActive = false;
    private boolean isDetectorActive = false;

    private float zoomFactor = DEFAULT_ZOOM_FACTOR;
    private float maxZoom = DEFAULT_ZOOM_FACTOR;
    private Rect screenSize = new Rect();

    private enum State {
        STATE_PREVIEW, STATE_WAITING_LOCK, STATE_WAITING_PRE_CAPTURE, STATE_WAITING_NON_PRE_CAPTURE, STATE_PICTURE_TAKEN
    }

    // TODO: use Semaphore for lock / unlock camera

    public void resume(SurfaceView previewSurfaceView) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "No camera permissions set!");
            return;
        }

        if (previewSurfaceView == null) {
            Log.w(TAG, "No surfaceView available!");
            return;
        }

        cameraId = getBackFacingCameraId();
        if (cameraId == null) {
            Log.w(TAG, "No back-facing camera found!");
            return;
        }

        // Start and Bind image processing service
        startAndBindImageProcessService();

        // Start a background thread to manage camera requests
        backgroundHandlerThread = new HandlerThread("background");
        backgroundHandlerThread.start();
        backgroundHandler = new Handler(backgroundHandlerThread.getLooper());

        surfaceView = previewSurfaceView;
        surfaceView.getHolder().addCallback(surfaceHolderCallback);

        Log.d(TAG, "resumed");
    }


    public void pause() {
        // Unbind and Stop image processing service
        stopAndUnbindImageProcessService();

        try {
            // Ensure SurfaceHolderCallback#surfaceChanged() will run again if the user returns
            surfaceView.getHolder().setFixedSize(0, 0);

            if (captureSession != null) {
                captureSession.abortCaptures();
                captureSession.close();
                captureSession = null;
                Log.d(TAG, "capture session closed");
            }
        } catch (Exception exception) {
            Log.e(TAG, Objects.requireNonNull(exception.getLocalizedMessage()));
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
            Log.d(TAG, "background handler thread closed");
        } catch (InterruptedException exception) {
            Log.e(TAG, Objects.requireNonNull(exception.getLocalizedMessage()));
        } finally {
            backgroundHandlerThread = null;
            backgroundHandler = null;
        }

        if (captureBuffer != null) {
            captureBuffer.close();
            captureBuffer = null;
            Log.d(TAG, "image reader closed");
        }

        Log.d(TAG, "paused");
    }


    public synchronized void setZoomActive(boolean zoomState) {
        isZoomActive = zoomState;
        backgroundHandler.post(unlockFocus);
    }


    public synchronized void increaseZoom() {
        zoomFactor = MathUtils.clamp(zoomFactor + .1f, DEFAULT_ZOOM_FACTOR, maxZoom);
        EventBus.getDefault().postSticky(new Events.ZoomChanged(zoomFactor));
        backgroundHandler.post(unlockFocus);
    }


    public synchronized void decreaseZoom() {
        zoomFactor = MathUtils.clamp(zoomFactor - .1f, DEFAULT_ZOOM_FACTOR, maxZoom);
        EventBus.getDefault().postSticky(new Events.ZoomChanged(zoomFactor));
        backgroundHandler.post(unlockFocus);
    }


    public void setDetectorActive(boolean detectorState) {
        isDetectorActive = detectorState;
    }


    public void orientationChanged(Util.Orientation orientation) {
        this.orientation = orientation;
    }


    public void captureImage() {
        if (backgroundHandler == null) {
            return;
        }

        Log.d(TAG, "start capturing image");
        backgroundHandler.post(captureState == State.STATE_PREVIEW ? lockFocus : unlockFocus);
    }


    @Nullable
    public Bitmap getLatestBitmap() {
        if (imageProcessingService != null) {
            return imageProcessingService.getLatestBitmap();
        }
        return null;
    }


    private Runnable lockFocus = new Runnable() {
        @Override
        public void run() {
            if (captureSession == null) {
                return;
            }

            //Log.d(TAG, "lock focus called");

            try {
                captureRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_START);
                captureState = State.STATE_WAITING_LOCK;
                captureSession.capture(captureRequestBuilder.build(), captureCallback, null);
            } catch (Exception exception) {
                Log.e(TAG, Objects.requireNonNull(exception.getMessage()));
            }
        }
    };


    private Runnable unlockFocus = new Runnable() {
        @Override
        public void run() {
            if (captureSession == null) {
                return;
            }

            //Log.d(TAG, "unlock focus called. (zoom active: " + isZoomActive + ")");

            try {
                captureRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);
                captureSession.capture(captureRequestBuilder.build(), null, null);
                captureState = State.STATE_PREVIEW;

                if (isZoomActive) {
                    Rect cropRect = getCropRegion();
                    captureRequestBuilder.set(CaptureRequest.SCALER_CROP_REGION, cropRect);
                }

                captureSession.setRepeatingRequest(captureRequestBuilder.build(), captureCallback, null);
            } catch (Exception exception) {
                Log.e(TAG, Objects.requireNonNull(exception.getMessage()));
            }
        }
    };


    private Runnable runPrecaptureSequence = new Runnable() {
        @Override
        public void run() {

            Log.d(TAG, "run precapture sequence");

            try {
                captureRequestBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER, CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START);
                captureState = State.STATE_WAITING_PRE_CAPTURE;
                captureSession.capture(captureRequestBuilder.build(), captureCallback, null);
            } catch (Exception exception) {
                Log.e(TAG, Objects.requireNonNull(exception.getMessage()));
            }
        }
    };


    private Runnable captureStillImage = new Runnable() {
        @Override
        public void run() {

            Log.d(TAG, "run capture still image");

            try {
                CaptureRequest.Builder captureRequest = camera.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
                captureRequest.addTarget(captureBuffer.getSurface());

                captureRequest.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_OFF);
                captureRequest.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);

                if (isZoomActive) {
                    Rect cropRect = getCropRegion();
                    captureRequest.set(CaptureRequest.SCALER_CROP_REGION, cropRect);
                }

                CameraCaptureSession.CaptureCallback inlineCaptureCallback = new CameraCaptureSession.CaptureCallback() {
                    @Override
                    public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                        Log.d(TAG, "capturing completed");
                        backgroundHandler.post(unlockFocus);
                    }
                };

                captureSession.stopRepeating();
                captureSession.capture(captureRequest.build(), inlineCaptureCallback, null);

            } catch (Exception exception) {
                Log.e(TAG, Objects.requireNonNull(exception.getMessage()));
            }
        }
    };


    private Rect getCropRegion() {
        int centerX = screenSize.width() / 2;
        int centerY = screenSize.height() / 2;
        int deltaX = (int)(centerX / zoomFactor);
        int deltaY = (int)(centerY / zoomFactor);

        return new Rect(centerX - deltaX, centerY - deltaY, centerX + deltaX, centerY + deltaY);
    }


    private void startAndBindImageProcessService() {
        context.bindService(ImageProcessingService_.intent(context).get(), serviceConnection, Context.BIND_AUTO_CREATE);
    }


    private void stopAndUnbindImageProcessService() {
        context.unbindService(serviceConnection);
        context.stopService(ImageProcessingService_.intent(context).get());
    }


    private void setSurfaceViewSize(int requestedWidth, int requestedHeight) {
        Size optimalSize = getOptimalPreviewSize(requestedWidth, requestedHeight);

        if (optimalSize.getWidth() != requestedWidth || optimalSize.getHeight() != requestedHeight) {
            surfaceView.getHolder().setFixedSize(optimalSize.getWidth(), optimalSize.getHeight());
            Log.d(TAG, "Surface new size: " + optimalSize.getWidth() + "x" + optimalSize.getHeight());
            return;
        }

        if (captureBuffer == null) {
            captureBuffer = ImageReader.newInstance(optimalSize.getWidth(), optimalSize.getHeight(), ImageFormat.YUV_420_888, 2);
            captureBuffer.setOnImageAvailableListener(imageCaptureListener, backgroundHandler);
        }

        if (captureSession == null) {
            Surface surfaceViewSurface = surfaceView.getHolder().getSurface();
            Surface captureBufferSurface = captureBuffer.getSurface();

            if (surfaceViewSurface == null || captureBufferSurface == null) {
                Log.w(TAG, "One surface view is NULL. (" + surfaceViewSurface + ", " + captureBufferSurface + ")");
                return;
            }

            if (camera == null) {
                Log.w(TAG, "Camera is NULL");
                return;
            }

            try {
                List<Surface> outputs = Arrays.asList(surfaceViewSurface, captureBufferSurface);
                camera.createCaptureSession(outputs, captureSessionCallback, null);
            } catch (Exception ex) {
                Log.e(TAG, Objects.requireNonNull(ex.getLocalizedMessage()));
            } finally {
                Log.d(TAG, "Camera session created");
            }
        }
    }


    private String getBackFacingCameraId() {
        try {
            for (String cameraListId : cameraManager.getCameraIdList()) {
                CameraCharacteristics cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraListId);
                Integer lensFacing = cameraCharacteristics.get(CameraCharacteristics.LENS_FACING);

                if (lensFacing != null && lensFacing == CameraCharacteristics.LENS_FACING_BACK) {
                    return cameraListId;
                }
            }
        } catch (Exception exception) {
            Log.e(TAG, Objects.requireNonNull(exception.getLocalizedMessage()));
        }

        return null;
    }


    private Size getOptimalPreviewSize(int requestedWidth, int requestedHeight) {
        int width = requestedWidth;
        int height = requestedHeight;

        if (orientation == Util.Orientation.PORTRAIT) {
            width = requestedHeight;
            height = requestedWidth;
        }

        //Log.d(TAG, "surface old size: " + requestedWidth + "x" + requestedHeight);

        try {
            CameraCharacteristics cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId);

            screenSize = cameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
            Float maxZoomValue = cameraCharacteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM);
            maxZoom = maxZoomValue == null || maxZoomValue < DEFAULT_ZOOM_FACTOR ? DEFAULT_ZOOM_FACTOR : maxZoomValue;

            StreamConfigurationMap info = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            assert info != null;
            Size[] outputSizes = info.getOutputSizes(SurfaceHolder.class);
            return Util.chooseBigEnoughSize(outputSizes, width, height);
        } catch (Exception exception) {
            Log.e(TAG, Objects.requireNonNull(exception.getLocalizedMessage()));
        }

        return new Size(width, height);
    }


    private void processCaptureResult(CaptureResult captureResult) {
        switch (captureState) {
            case STATE_PREVIEW:
            case STATE_PICTURE_TAKEN: {
                // do nothing
                break;
            }
            case STATE_WAITING_LOCK: {
                final Integer afState = captureResult.get(CaptureResult.CONTROL_AF_STATE);

                if (afState == null ||
                        afState == CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED ||
                        afState == CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED ||
                        afState == CaptureResult.CONTROL_AF_STATE_INACTIVE) {

                    int aeState = captureResult.get(CaptureResult.CONTROL_AE_STATE);
                    if (aeState == CaptureResult.CONTROL_AE_STATE_CONVERGED) {
                        captureState = State.STATE_PICTURE_TAKEN;
                        backgroundHandler.post(captureStillImage);
                    } else {
                        backgroundHandler.post(runPrecaptureSequence);
                    }
                }
                break;
            }
            case STATE_WAITING_PRE_CAPTURE: {
                final Integer aeState = captureResult.get(CaptureResult.CONTROL_AE_STATE);
                if (aeState == null
                        || CaptureResult.CONTROL_AE_STATE_PRECAPTURE == aeState
                        || CaptureRequest.CONTROL_AE_STATE_FLASH_REQUIRED == aeState) {
                    captureState = State.STATE_WAITING_NON_PRE_CAPTURE;
                }
                break;
            }
            case STATE_WAITING_NON_PRE_CAPTURE: {
                final Integer aeState = captureResult.get(CaptureResult.CONTROL_AE_STATE);
                if (aeState != CaptureResult.CONTROL_AE_STATE_PRECAPTURE) {
                    captureState = State.STATE_PICTURE_TAKEN;
                    backgroundHandler.post(captureStillImage);
                }
                break;
            }
        }
    }


    private final SurfaceHolder.Callback surfaceHolderCallback = new SurfaceHolder.Callback() {

        @SuppressLint("MissingPermission")
        @Override
        public void surfaceCreated(SurfaceHolder surfaceHolder) {
            try {
                cameraManager.openCamera(cameraId, cameraStateCallback, backgroundHandler);
            } catch (Exception exception) {
                Log.e(TAG, Objects.requireNonNull(exception.getLocalizedMessage()));
            }
        }

        @Override
        public void surfaceChanged(SurfaceHolder surfaceHolder, int format, int width, int height) {
            setSurfaceViewSize(width, height);
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
            Log.d(TAG, "Camera opened");
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            Log.d(TAG, "Camera has been disconnected");
        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int i) {
            Log.e(TAG, "Camera error");
        }
    };


    private final CameraCaptureSession.StateCallback captureSessionCallback = new CameraCaptureSession.StateCallback() {

        @Override
        public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
            captureSession = cameraCaptureSession;
            SurfaceHolder holder = surfaceView.getHolder();

            if (holder != null) {
                try {
                    // Build a request for preview footage
                    captureRequestBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                    captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                    captureRequestBuilder.addTarget(holder.getSurface());

                    if (isZoomActive) {
                        Rect cropRect = getCropRegion();
                        captureRequestBuilder.set(CaptureRequest.SCALER_CROP_REGION, cropRect);
                    }

                    // Start displaying preview images
                    captureSession.setRepeatingRequest(captureRequestBuilder.build(), captureCallback, null);
                } catch (Exception exception) {
                    Log.e(TAG, Objects.requireNonNull(exception.getLocalizedMessage()));
                } finally {
                    EventBus.getDefault().postSticky(new Events.BionicEyeReady());
                    Log.d(TAG, "Camera capture session started (repeating request)");
                }
            }
        }

        @Override
        public void onClosed(CameraCaptureSession session) {
            captureSession = null;
        }

        @Override
        public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
            Log.e(TAG, "Camera capture session configuration failed.");
        }
    };


    private final CameraCaptureSession.CaptureCallback captureCallback = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureProgressed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureResult partialResult) {
            processCaptureResult(partialResult);
        }

        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
            processCaptureResult(result);
        }
    };


    private final ImageReader.OnImageAvailableListener imageCaptureListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(final ImageReader reader) {
            if (!imageBusy) {
                new Thread(new CapturedImageSaver(reader)).start();
            }
        }
    };


    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            ImageProcessingService.LocalBinder binder = (ImageProcessingService.LocalBinder) iBinder;
            imageProcessingService = binder.getService();
            Log.d(TAG, "bind image processing service");
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            imageProcessingService = null;
            Log.d(TAG, "unbind image processing service");
        }
    };


    private class CapturedImageSaver implements Runnable {
        private ImageReader imageReader;

        public CapturedImageSaver(ImageReader reader) {
            this.imageReader = reader;
            imageBusy = true;
        }

        @Override
        public void run() {
            if (imageProcessingService != null) {
                if (!imageProcessingService.isImageProcessing()) {
                    imageProcessingService.processImage(imageReader, isDetectorActive);
                }
            }

            imageBusy = false;
        }
    }
}
