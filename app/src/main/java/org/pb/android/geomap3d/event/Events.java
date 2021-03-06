package org.pb.android.geomap3d.event;

import android.graphics.Bitmap;
import android.location.Location;

import com.google.android.gms.maps.model.LatLng;

import org.pb.android.geomap3d.data.map.model.TerrainMapData.LoadingState;
import org.pb.android.geomap3d.data.persist.geoplace.GeoPlace;
import org.pb.android.geomap3d.data.persist.geoplace.GeoPlaces;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class Events {

    public static class ProgressUpdate {
        private final float progressValue;

        public ProgressUpdate(float progressValue) {
            this.progressValue = progressValue;
        }

        public float getProgressValue() {
            return progressValue;
        }
    }

    public static class WidgetReady {
    }

    public static class FragmentLoaded {
        private final String tag;

        public FragmentLoaded(String tag) {
            this.tag = tag;
        }

        public String getTag() {
            return tag;
        }
    }

    public static class VibrationEvent {
    }

    public static class ShowToast {
        private final String message;

        public ShowToast(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }

    public static class OutsideOfMap {
        private final Location location;

        public OutsideOfMap(Location location) {
            this.location = location;
        }

        public Location getLocation() {
            return location;
        }
    }

    public static class LocationUpdate {
        private final List<Location> locations;

        public LocationUpdate(List<Location> locations) {
            this.locations = new ArrayList<>(locations);
        }

        public List<Location> getLocations() {
            return locations;
        }
    }

    public static class HeightMapLoadStart {
    }

    public static class LoadHeightMap {
        private final LatLng targetLocation;

        public LoadHeightMap(LatLng targetLocation) {
            this.targetLocation = targetLocation;
        }

        public Location getTargetLocation() {
            Location targetLocation = new Location("");
            targetLocation.setLatitude(this.targetLocation.latitude);
            targetLocation.setLongitude(this.targetLocation.longitude);
            return targetLocation;
        }
    }

    public static class HeightMapLoaded {
        private final String areaName;
        private final Location areaLocation;
        private final LoadingState loadingState;

        public HeightMapLoaded(@Nullable String areaName, Location areaLocation, LoadingState loadingState) {
            this.areaName = areaName;
            this.areaLocation = areaLocation;
            this.loadingState = loadingState;
        }

        public LatLng getAreaLocation() {
            return new LatLng(areaLocation.getLatitude(), areaLocation.getLongitude());
        }

        public LoadingState getLoadingState() {
            return loadingState;
        }

        @Nullable
        public String getAreaName() {
            return areaName;
        }
    }

    @Deprecated
    public static class ShowFragment {
        private final String tag;
        private final Fragment fragment;

        public ShowFragment(Fragment fragment, String tag) {
            this.tag = tag;
            this.fragment = fragment;
        }

        public String getTag() {
            return tag;
        }

        public Fragment getFragment() {
            return fragment;
        }
    }

    public static class ShowMapFragment {
        private final Location location;

        public ShowMapFragment(@Nullable Location location) {
            this.location = location;
        }

        @Nullable
        public Location getLocation() {
            return location;
        }
    }

    public static class ShowTerrainFragment {
    }

    public static class ShowBionicEyeFragment {
    }

    public static class MapReadyEvent {
        private final Location currentLocation;

        public MapReadyEvent(Location currentLocation) {
            this.currentLocation = currentLocation;
        }

        public Location getCurrentLocation() {
            return currentLocation;
        }
    }

    public static class CameraStateEvent {
        private final Integer afState;

        public CameraStateEvent(Integer afState) {
            this.afState = afState;
        }

        public Integer getAfState() {
            return afState;
        }
    }

    public static class BionicEyeReady {
    }

    public static class ZoomChanged {
        private final float zoomValue;

        public ZoomChanged(float zoomValue) {
            this.zoomValue = zoomValue;
        }

        public float getZoomValue() {
            return zoomValue;
        }
    }

    public static class TrackDistanceChanged {
        private final int trackDistance;

        public TrackDistanceChanged(int trackDistance) {
            this.trackDistance = trackDistance;
        }

        public int getTrackDistance() {
            return trackDistance;
        }
    }

    public static class GeoPlacesAvailable {
        private final GeoPlaces geoPlaces;

        public GeoPlacesAvailable(GeoPlaces geoPlaces) {
            this.geoPlaces = geoPlaces;
        }

        public GeoPlaces getGeoPlaces() {
            return geoPlaces;
        }
    }

    public static class ShowGeoPlaces {
        private final boolean show;

        public ShowGeoPlaces(boolean show) {
            this.show = show;
        }

        public boolean show() {
            return show;
        }
    }

    public static class ShowGeoPlaceInfo {
        private final GeoPlace geoPlace;

        public ShowGeoPlaceInfo(GeoPlace geoPlace) {
            this.geoPlace = geoPlace;
        }

        public GeoPlace getGeoPlace() {
            return geoPlace;
        }
    }

    public static class TargetSelected {
        private final GeoPlace geoPlace;

        public TargetSelected(GeoPlace geoPlace) {
            this.geoPlace = geoPlace;
        }

        public GeoPlace getTarget() {
            return geoPlace;
        }
    }
}
