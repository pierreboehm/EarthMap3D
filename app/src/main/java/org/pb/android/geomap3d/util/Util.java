package org.pb.android.geomap3d.util;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ConfigurationInfo;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Environment;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Size;
import android.util.TypedValue;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;

import androidx.annotation.Nullable;

import org.pb.android.geomap3d.data.route.model.Route;
import org.pb.android.geomap3d.data.route.model.Routes;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static android.content.Context.WINDOW_SERVICE;

public class Util {

    private static final String TAG = Util.class.getSimpleName();
    private static final String ROUTES_XML = "routes.xml";

    public enum Orientation {
        PORTRAIT, LANDSCAPE
    }

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

    public static boolean isNetworkAvailable(@Nullable Context context) {
        if (context == null) {
            return true;
        }

        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null) {
            return true;
        }

        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isAvailable() && activeNetworkInfo.isConnected();
    }

    public static int getDisplayHeight(Context context) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        return metrics.heightPixels;
    }

    public static int getDisplayWidth(Context context) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        return metrics.widthPixels;
    }

    public static Orientation getOrientation(Context context) {
        Display display = ((WindowManager) context.getSystemService(WINDOW_SERVICE)).getDefaultDisplay();
        int displayOrientation = display.getRotation();

        if (displayOrientation == Surface.ROTATION_0 || displayOrientation == Surface.ROTATION_180) {
            return Orientation.PORTRAIT;
        } else {
            return Orientation.LANDSCAPE;
        }
    }

    public static void openLocationSourceSettings(Context context) {
        Intent intent = new Intent();
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setAction(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        context.startActivity(intent);
    }

    public static List<Route> loadAvailableRoutes(Context context) {
        Serializer serializer = new Persister();
        InputStream xmlRoutes; //context.getResources().openRawResource(R.raw.routes);  --> just for test purposes

        File sdCardFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + ROUTES_XML);
        try {
            xmlRoutes = new FileInputStream(sdCardFile);
        } catch (FileNotFoundException exception) {
            Log.i(TAG, exception.getLocalizedMessage());
            return new ArrayList<>();
        }

        try {
            Routes routes = serializer.read(Routes.class, xmlRoutes);
            if (routes != null) {
                return routes.getRouteList();
            }
        } catch (Exception exception) {
            Log.e(TAG, exception.getLocalizedMessage());
        } finally {
            try {
                xmlRoutes.close();
            } catch (Exception exception) {
                // not implemented
            }
        }

        return new ArrayList<>();
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

    public static Size chooseBigEnoughSize(Size[] choices, int width, int height) {
        // Collect the supported resolutions that are at least as big as the preview Surface
        List<Size> bigEnough = new ArrayList<>();
        for (Size option : choices) {
            if (option.getWidth() >= width && option.getHeight() >= height) {
                bigEnough.add(option);
                //Log.d("Util", "  option size: " + option.getWidth() + "x" + option.getHeight());
            }
        }

        // Pick the smallest of those, assuming we found any
        if (bigEnough.size() > 0) {
            return Collections.min(bigEnough, new CompareSizesByArea());
        } else {
            return choices[0];
        }
    }

    public static class CompareSizesByArea implements Comparator<Size> {
        @Override
        public int compare(Size lhs, Size rhs) {
            // We cast here to ensure the multiplications won't overflow
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() - (long) rhs.getWidth() * rhs.getHeight());
        }
    }
}
