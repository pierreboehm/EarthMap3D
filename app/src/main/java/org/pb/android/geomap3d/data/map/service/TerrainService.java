package org.pb.android.geomap3d.data.map.service;

import android.app.Activity;
import android.graphics.Bitmap;
import android.location.Location;
import android.util.Log;

import com.google.android.gms.maps.model.LatLngBounds;

import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.RootContext;
import org.greenrobot.eventbus.EventBus;
import org.pb.android.geomap3d.data.map.model.TerrainMapData;
import org.pb.android.geomap3d.event.Events;
import org.pb.android.geomap3d.util.GeoUtil;

import java.io.InterruptedIOException;
import java.util.Locale;

import androidx.annotation.Nullable;
import okhttp3.OkHttpClient;
import okhttp3.Request;

import static java.util.concurrent.TimeUnit.SECONDS;

@EBean
public class TerrainService {

    private static final String TAG = TerrainService.class.getSimpleName();
    // http://terrain.party/api/export?name=kaufunger_wald_2&box=9.858200,51.317693,9.743203,51.245828

    private static final String BASEURL = "http://terrain.party/api/export/";

    private LatLngBounds lastTargetBounds;

    @RootContext
    Activity activity;

    @Nullable
    public Bitmap getMapForLocation(Location location) {
        TerrainMapData terrainMapData = loadMapDataForLocation(location);

        if (terrainMapData.getLoadingState() == TerrainMapData.LoadingState.LOADING_SUCCESS) {
            Log.v(TAG, "getMapForLocation(): LOADING_SUCCESS");
            return terrainMapData.getBitmap();
        } else if (terrainMapData.getLoadingState() == TerrainMapData.LoadingState.LOADING_EMPTY) {
            Log.v(TAG, "getMapForLocation(): LOADING_EMPTY");
            EventBus.getDefault().post(new Events.ShowToast("No heightmap found"));
        }

        return null;
    }

    @Nullable
    public LatLngBounds getLastTargetBounds() {
        return lastTargetBounds;
    }

    private TerrainMapData loadMapDataForLocation(Location location) {
        OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder();

        clientBuilder.connectTimeout(5, SECONDS);
        clientBuilder.readTimeout(30, SECONDS);

        lastTargetBounds = GeoUtil.getDefaultRectangleLatLngBounds(GeoUtil.getLatLngFromLocation(location));

        Request loadMapDataRequest = new Request.Builder().get().url(getMapDataLocationUrl(lastTargetBounds)).build();
        okhttp3.Call loadMapDataCall = clientBuilder.build().newCall(loadMapDataRequest);

        try {
            okhttp3.Response loadMapDataResponse = loadMapDataCall.execute();
            if (loadMapDataResponse.isSuccessful()) {
                return new TerrainMapData(activity, loadMapDataResponse.body());
            }
        } catch (InterruptedIOException interruptedException) {
            Log.e(TAG, "Error: " + interruptedException.getLocalizedMessage());
            return new TerrainMapData(TerrainMapData.LoadingState.LOADING_INTERRUPTED);
        } catch (Exception exception) {
            Log.e(TAG, "Error: " + exception.getLocalizedMessage());
        }

        lastTargetBounds = null;    // be sure it's cleared again in error case
        return new TerrainMapData(TerrainMapData.LoadingState.LOADING_FAILED);
    }

    private String getMapDataLocationUrl(LatLngBounds targetBounds) {
        return String.format(Locale.US,"%s?name=%s&box=%.06f,%.06f,%.06f,%.06f",
                BASEURL, Long.toHexString(System.currentTimeMillis()),
                targetBounds.northeast.longitude, targetBounds.northeast.latitude,
                targetBounds.southwest.longitude, targetBounds.southwest.latitude);
    }

}
