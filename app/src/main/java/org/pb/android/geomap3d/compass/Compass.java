package org.pb.android.geomap3d.compass;

import android.content.Context;
import android.hardware.GeomagneticField;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EBean;
import org.pb.android.geomap3d.location.LocationManager;

@EBean(scope = EBean.Scope.Singleton)
public class Compass implements SensorEventListener {

    public static final String TAG = Compass.class.getSimpleName();

    @Bean
    LocationManager locationManager;

    public interface CompassListener {
        void onNewAzimuth(float azimuth);
    }

    public interface GravityListener {
        void onNewGravityData(float[] gravity);
    }

    public interface PitchAndRollListener {
        void onNewPitchAndRoll(float pitch, float roll);
    }

    private CompassListener compassListener;
    private GravityListener gravityListener;
    private PitchAndRollListener pitchAndRollListener;

    private SensorManager sensorManager;
    private Sensor gravitySensor;
    private Sensor magneticFieldSensor;

    private float[] gravityData = new float[3];
    private float[] geomagneticData = new float[3];
    private float[] rotationMatrix = new float[9];

    @SuppressWarnings("FieldCanBeLocal")
    private float[] smoothedData = new float[3];

    @SuppressWarnings("WeakerAccess")
    public Compass(Context context) {
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);

        // FIXME: add null-check
        gravitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magneticFieldSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
    }

    public void start() {
        sensorManager.registerListener(this, gravitySensor, SensorManager.SENSOR_DELAY_UI);
        sensorManager.registerListener(this, magneticFieldSensor, SensorManager.SENSOR_DELAY_UI);
    }

    public void stop() {
        sensorManager.unregisterListener(this);
    }

    public void setListener(CompassListener compassListener) {
        this.compassListener = compassListener;
    }

    public void setListener(GravityListener gravityListener) {
        this.gravityListener = gravityListener;
    }

    public void setListener(PitchAndRollListener pitchAndRollListener) {
        this.pitchAndRollListener = pitchAndRollListener;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        synchronized (this) {
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                smoothedData = LowPassFilter.filter(event.values, gravityData);
                System.arraycopy(smoothedData, 0, gravityData, 0, 3);

                if (gravityListener != null) {
                    gravityListener.onNewGravityData(smoothedData);
                }
            }

            if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
                smoothedData = LowPassFilter.filter(event.values, geomagneticData);
                System.arraycopy(smoothedData, 0, geomagneticData, 0, 3);
            }

            boolean success = SensorManager.getRotationMatrix(rotationMatrix, null, gravityData, geomagneticData);

            if (success) {

                if (pitchAndRollListener != null) {
                    float[] outR = new float[9];
                    SensorManager.remapCoordinateSystem(rotationMatrix, SensorManager.AXIS_X, SensorManager.AXIS_Z, outR);
                    SensorManager.getOrientation(outR, smoothedData);

                    float pitch = smoothedData[1] * 57.2957795f;
                    float roll = smoothedData[2] * 57.2957795f;

                    pitchAndRollListener.onNewPitchAndRoll(-pitch, -roll);
                }

                if (compassListener != null) {
                    float orientation[] = new float[3];
                    SensorManager.getOrientation(rotationMatrix, orientation);

                    float azimuth = (float) Math.toDegrees(orientation[0]);
                    GeomagneticField geomagneticField = locationManager.getGeomagneticField();

                    if (geomagneticField != null) {
                        azimuth += geomagneticField.getDeclination();
                    }

                    if (azimuth < 0) {
                        azimuth += 360;
                    }

                    compassListener.onNewAzimuth(azimuth);
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }
}
