package org.pb.android.geomap3d.compass;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.view.Surface;
import android.view.WindowManager;

import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.SystemService;
import org.pb.android.geomap3d.location.LocationManager;

@EBean(scope = EBean.Scope.Singleton)
public class Compass implements SensorEventListener {

    public static final String TAG = Compass.class.getSimpleName();

    @SystemService
    WindowManager windowManager;

    @Bean
    LocationManager locationManager;

    public interface CompassListener {
        void onNewAzimuth(float azimuth);
    }

    public interface GravityListener {
        void onNewGravityData(float[] gravity);
    }

    private CompassListener compassListener;
    private GravityListener gravityListener;

    private SensorManager sensorManager;
    private Sensor gravitySensor;
    private Sensor compassSensor;

    private float[] gravityData = new float[3];
    private float[] rotationMatrix = new float[9];

    @SuppressWarnings("FieldCanBeLocal")
    private float[] smoothedData = new float[3];

    @SuppressWarnings("WeakerAccess")
    public Compass(Context context) {
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);

        compassSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        // FIXME: Remove! Only use ROTATION_VECTOR!
        gravitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    }

    public void start() {
        sensorManager.registerListener(this, compassSensor, SensorManager.SENSOR_DELAY_UI);
        // FIXME: Remove! Only use compassSensor!
        sensorManager.registerListener(this, gravitySensor, SensorManager.SENSOR_DELAY_UI);
    }

    public void stop() {
        sensorManager.unregisterListener(this);
    }

    public void setListener(CompassListener compassListener) {
        this.compassListener = compassListener;
    }

    // FIXME: Remove! Use CompassListener (pitch) instead!
    public void setListener(GravityListener gravityListener) {
        this.gravityListener = gravityListener;
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

                // TODO: check if correct: azimuth: 0, pitch: 1, roll: 2

                float azimuth = (float) (Math.toDegrees(orientation[0]) + 360f) % 360f;
                compassListener.onNewAzimuth(azimuth);
            }

            // FIXME: Remove! Use ROTATION_VECTOR instead!
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                smoothedData = LowPassFilter.filter(event.values, gravityData);
                System.arraycopy(smoothedData, 0, gravityData, 0, 3);

                if (gravityListener != null) {
                    gravityListener.onNewGravityData(smoothedData);
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }
}
