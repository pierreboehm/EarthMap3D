package org.pb.android.geomap3d.util;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ConfigurationInfo;
import android.location.LocationManager;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.TypedValue;

public class Util {

    public static class OpenGL {

        public enum OpenGLVersion {

            OPEN_GL_VERSION_2(0x20000), OPEN_GL_VERSION_3(0x30000);

            private int versionValue;

            OpenGLVersion(int versionValue) {
                this.versionValue = versionValue;
            }

            private static OpenGLVersion valueOf(int versionValue) {
                for (OpenGLVersion openGLVersion : OpenGLVersion.values()) {
                    if (openGLVersion.versionValue == versionValue) {
                        return openGLVersion;
                    }
                }

                return OPEN_GL_VERSION_2;
            }
        }

        public static OpenGLVersion getOpenGLVersion(ActivityManager activityManager) {
            ConfigurationInfo info = activityManager.getDeviceConfigurationInfo();
            return OpenGLVersion.valueOf(info.reqGlEsVersion);
        }

    }

    public static boolean isGPSEnabled(Context context) {
        final LocationManager manager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

        if (manager == null) {
            return false;
        }

        return manager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    public static int getDisplayHeight(Context context) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        return metrics.heightPixels;
    }

    public static int getDisplayWidth(Context context) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        return metrics.widthPixels;
    }

    public static void openLocationSourceSettings(Context context) {
        Intent intent = new Intent();
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setAction(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        context.startActivity(intent);
    }

    public static class PointF3D {
        public float x, y, z;

        public PointF3D(float x, float y, float z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    public static double roundScale(double d) {
        return Math.rint(d * 100) / 100.;
    }

    public static int roundUp(int num, int multipleOf) {
        return (int) (Math.ceil((double) num / (double) multipleOf) * (double) multipleOf);
    }

    public static float convertDPToPixel(Context context, float valueInDp) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, valueInDp, metrics);
    }

    public static float convertPixelsToDp(Context context, float valueInPx) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        return (valueInPx / ((float) metrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT));
    }

}
