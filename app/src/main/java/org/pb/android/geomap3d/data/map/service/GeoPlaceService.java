package org.pb.android.geomap3d.data.map.service;

import android.util.Log;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.android.gms.maps.model.LatLng;

import org.androidannotations.annotations.EBean;
import org.pb.android.geomap3d.data.map.model.GeoPlace;
import org.pb.android.geomap3d.data.map.model.GeoPlaceData;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import okhttp3.OkHttpClient;
import okhttp3.Request;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static java.util.concurrent.TimeUnit.SECONDS;

@EBean
public class GeoPlaceService {

    public static final String TAG = GeoPlaceService.class.getSimpleName();

    private static final String BASEURL = "http://geodb-free-service.wirefreethought.com";

    public List<GeoPlace> findGeoPlacesForLocation(LatLng location) {
        return loadGeoPlacesForLocation(location);
    }

    private List<GeoPlace> loadGeoPlacesForLocation(LatLng location) {
        OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder();

        clientBuilder.connectTimeout(5, SECONDS);
        clientBuilder.readTimeout(30, SECONDS);

        Request loadGeoPlacesRequest = new Request.Builder().get().url(getGeoPlacesForLocationUrl(location)).build();
        okhttp3.Call loadGeoPlacesCall = clientBuilder.build().newCall(loadGeoPlacesRequest);

        try {
            okhttp3.Response loadGeoPlacesResponse = loadGeoPlacesCall.execute();

            if (loadGeoPlacesResponse.isSuccessful()) {
                ObjectMapper objectMapper = new ObjectMapper();
                objectMapper.configure(FAIL_ON_UNKNOWN_PROPERTIES, false);

                String jsonData = loadGeoPlacesResponse.body().string();

                GeoPlaceData geoPlaceData = objectMapper.readValue(jsonData, GeoPlaceData.class);
                if (geoPlaceData != null) {
                    return geoPlaceData.getGeoPlaces();
                }
            }
        } catch (Exception exception) {
            Log.e(TAG, "Error: " + exception.getLocalizedMessage());
        }

        return new ArrayList<>();
    }

    private String getGeoPlacesForLocationUrl(LatLng location) {
        return String.format(Locale.US,
                "%s/v1/geo/locations/+%.04f+%.04f/nearbyCities?radius=20",
                BASEURL,
                location.latitude,
                location.longitude
        );
    }
}
