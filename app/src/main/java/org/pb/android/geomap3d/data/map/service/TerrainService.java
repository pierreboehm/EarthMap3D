package org.pb.android.geomap3d.data.map.service;

import android.app.Activity;
import android.graphics.Bitmap;
import android.location.Location;
import android.util.Log;

import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.RootContext;
import org.pb.android.geomap3d.data.map.model.TerrainMapData;

import java.io.InterruptedIOException;

import androidx.annotation.Nullable;
import okhttp3.OkHttpClient;
import okhttp3.Request;

import static java.util.concurrent.TimeUnit.SECONDS;

@EBean
public class TerrainService {

    private static final String TAG = TerrainService.class.getSimpleName();
    // http://terrain.party/api/export?name=kaufunger_wald_2&box=9.858200,51.317693,9.743203,51.245828
    private static final String BASEURL = "http://terrain.party/api/export/?";

    @RootContext
    Activity activity;

    @Nullable
    public Bitmap getMapForLocation(Location location) {
        TerrainMapData terrainMapData = loadMapDataForLocation(location);

        if (terrainMapData.getLoadingState() == TerrainMapData.LoadingState.LOADING_SUCCESS) {
            return terrainMapData.getBitmap();
        }

        return null;
    }

    private TerrainMapData loadMapDataForLocation(Location location) {
        OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder();

        clientBuilder.connectTimeout(5, SECONDS);
        clientBuilder.readTimeout(30, SECONDS);

        Request loadMapDataRequest = new Request.Builder().get().url(getMapDataLocationUrl(location)).build();
        okhttp3.Call loadMapDataCall = clientBuilder.build().newCall(loadMapDataRequest);

        try {
            okhttp3.Response loadMapDataResponse = loadMapDataCall.execute();
            if (loadMapDataResponse.isSuccessful()) {
                return new TerrainMapData(activity, loadMapDataResponse.body());
            }
        } catch (InterruptedIOException interruptedException) {
            return new TerrainMapData(TerrainMapData.LoadingState.LOADING_INTERRUPTED);
        } catch (Exception exception) {
            Log.e(TAG, "Error: " + exception.getLocalizedMessage());
        }

        return new TerrainMapData(TerrainMapData.LoadingState.LOADING_FAILED);
    }

    private String getMapDataLocationUrl(Location location) {
        // TODO: this (hard-coded url) is just for test-purposes
        // see comment in TerrainWidget.InitiationThread constructor for implementation details
        return "http://terrain.party/api/export?name=kaufunger_wald_2&box=9.858200,51.317693,9.743203,51.245828";
    }

}
