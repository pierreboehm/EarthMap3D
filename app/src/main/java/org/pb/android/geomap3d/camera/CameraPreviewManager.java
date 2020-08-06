package org.pb.android.geomap3d.camera;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Handler;
import android.os.HandlerThread;
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

    @RootContext
    Context context;

    @SystemService
    CameraManager cameraManager;

    private CameraDevice camera;
    private SurfaceView surfaceView;

    private HandlerThread backgroundHandlerThread;
    private Handler backgroundHandler;

    private CameraCaptureSession captureSession;

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
            }
        }

        // Finish processing posted messages, then join on the handling thread
        backgroundHandlerThread.quitSafely();
        try {
            backgroundHandlerThread.join();
        } catch (InterruptedException ex) {
            // implement
        }
    }

    final SurfaceHolder.Callback surfaceHolderCallback = new SurfaceHolder.Callback() {

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

    final CameraDevice.StateCallback cameraStateCallback = new CameraDevice.StateCallback() {

        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            camera = cameraDevice;

            try {
                List<Surface> outputs = Arrays.asList(surfaceView.getHolder().getSurface());
                camera.createCaptureSession(outputs, captureSessionListener, backgroundHandler);
            } catch (CameraAccessException ex) {
                //Log.e(TAG, "Failed to create a capture session", ex);
            }
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            // implement
        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int i) {
            // implement
        }
    };

    final CameraCaptureSession.StateCallback captureSessionListener = new CameraCaptureSession.StateCallback() {

        @Override
        public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
            captureSession = cameraCaptureSession;
            SurfaceHolder holder = surfaceView.getHolder();

            if (holder != null) {
                try {
                    // Build a request for preview footage
                    CaptureRequest.Builder requestBuilder = camera.createCaptureRequest(camera.TEMPLATE_PREVIEW);
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

    private String setSurfaceViewSize(int width, int height) {
        try {
            for (String cameraListId : cameraManager.getCameraIdList()) {

                CameraCharacteristics cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraListId);
                if (cameraCharacteristics.get(cameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK) {

                    StreamConfigurationMap info = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                    // Bigger is better when it comes to saving our image
                    //Size largestSize = Collections.max(Arrays.asList(info.getOutputSizes(ImageFormat.JPEG)), new Util.CompareSizesByArea());

                    Size optimalSize = Util.chooseBigEnoughSize(info.getOutputSizes(SurfaceHolder.class), width, height);

                    surfaceView.getHolder().setFixedSize(optimalSize.getWidth(), optimalSize.getHeight());
                    return cameraListId;
                }
            }
        } catch (Exception exception) {
            // implement
        }

        return null;
    }
}
