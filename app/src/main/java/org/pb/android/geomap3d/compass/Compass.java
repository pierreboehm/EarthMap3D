package org.pb.android.geomap3d.compass;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;
import android.view.Surface;
import android.view.WindowManager;

import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.SystemService;
import org.pb.android.geomap3d.location.LocationManager;

import java.util.Locale;

@EBean(scope = EBean.Scope.Singleton)
public class Compass implements SensorEventListener {

    public static final String TAG = Compass.class.getSimpleName();

    @SystemService
    WindowManager windowManager;

    @Bean
    LocationManager locationManager;

    public interface CompassListener {
        void onRotationChanged(float azimuth, float pitch, float roll);
    }

    private CompassListener compassListener;

    private SensorManager sensorManager;
    private Sensor compassSensor;

    private float[] rotationMatrix = new float[9];

    @SuppressWarnings("WeakerAccess")
    public Compass(Context context) {
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        compassSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
    }

    public void start() {
        sensorManager.registerListener(this, compassSensor, SensorManager.SENSOR_DELAY_UI);
    }

    public void stop() {
        sensorManager.unregisterListener(this);
    }

    public void setListener(CompassListener compassListener) {
        this.compassListener = compassListener;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        synchronized (this) {
            if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR && compassListener != null) {
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values);

                int worldAxisForDeviceAxisX;
                int worldAxisForDeviceAxisY;

                switch (windowManager.getDefaultDisplay().getRotation()) {
                    case Surface.ROTATION_90:
                        worldAxisForDeviceAxisX = SensorManager.AXIS_Z;
                        worldAxisForDeviceAxisY = SensorManager.AXIS_MINUS_X;
                        break;
                    case Surface.ROTATION_180:
                        worldAxisForDeviceAxisX = SensorManager.AXIS_MINUS_X;
                        worldAxisForDeviceAxisY = SensorManager.AXIS_MINUS_Z;
                        break;
                    case Surface.ROTATION_270:
                        worldAxisForDeviceAxisX = SensorManager.AXIS_MINUS_Z;
                        worldAxisForDeviceAxisY = SensorManager.AXIS_X;
                        break;
                    case Surface.ROTATION_0:
                    default:
                        worldAxisForDeviceAxisX = SensorManager.AXIS_X;
                        worldAxisForDeviceAxisY = SensorManager.AXIS_Z;
                        break;
                }

                float[] adjustedRotationMatrix = new float[9];
                SensorManager.remapCoordinateSystem(rotationMatrix, worldAxisForDeviceAxisX, worldAxisForDeviceAxisY, adjustedRotationMatrix);

                // Transform rotation matrix into azimuth/pitch/roll
                float[] orientation = new float[3];
                SensorManager.getOrientation(adjustedRotationMatrix, orientation);

                float azimuth = (float) Math.toDegrees(orientation[0]);
                float pitch = (float) Math.toDegrees(orientation[1]);
                float roll = (float) Math.toDegrees(orientation[2]);

                //Log.v(TAG, String.format(Locale.getDefault(), "raw: %.2f° %.2f° %.2f°", azimuth, pitch, roll));

                azimuth = (azimuth + 360f) % 360f;
                pitch = (pitch + 360f) % 360f;
                roll = (roll + 360f) % 360;

                //Log.v(TAG, String.format(Locale.getDefault(), "%.2f° %.2f° %.2f°", azimuth, pitch, roll));

                compassListener.onRotationChanged(azimuth, pitch, roll);
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        Log.d(TAG, "Orientation sensor accuracy level: " + accuracy);

        if (accuracy != SensorManager.SENSOR_STATUS_ACCURACY_HIGH) {
            Log.w(TAG, "Sensor needs to be calibrated!");
        }
    }
}
