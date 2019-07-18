package org.pb.android.geomap3d.compass;

import androidx.annotation.NonNull;

public final class LowPassFilter {

    /**
     * Time smoothing constant for low-pass filter 0 ≤ α ≤ 1 ; A smaller value basically means more smoothing.
     * For more details see here:
     *      http://en.wikipedia.org/wiki/Low-pass_filter#Discrete-time_realization
     */
    private static final float ALPHA = 0.2f;

    @SuppressWarnings("WeakerAccess")
    public static float[] filter(@NonNull float[] inputValues, @NonNull float[] previousValues) {
        return filter(inputValues, previousValues, ALPHA);
    }

    @SuppressWarnings("WeakerAccess")
    public static float[] filter(@NonNull float[] inputValues, @NonNull float[] previousValues, float alpha) {
        if (inputValues.length != previousValues.length) {
            throw new IllegalArgumentException("Both arrays must have the same size.");
        }

        for (int i = 0; i < inputValues.length; i++) {
            previousValues[i] = previousValues[i] + alpha * (inputValues[i] - previousValues[i]);
        }

        return previousValues;
    }

    @SuppressWarnings("unused")
    public static float filter(float inputValue, float previousValue) {
        return filter(inputValue, previousValue, ALPHA);
    }

    public static float filter(float inputValue, float previousValue, float alpha) {
        return (previousValue + alpha * (inputValue - previousValue));
    }

    public static float filter(float inputValue, float previousValue, float alpha, float modulo) {
        return (previousValue + alpha * (inputValue - previousValue)) % modulo;
    }
}
