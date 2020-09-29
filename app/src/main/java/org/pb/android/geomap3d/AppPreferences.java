package org.pb.android.geomap3d;

import org.androidannotations.annotations.sharedpreferences.DefaultBoolean;
import org.androidannotations.annotations.sharedpreferences.DefaultFloat;
import org.androidannotations.annotations.sharedpreferences.DefaultInt;
import org.androidannotations.annotations.sharedpreferences.SharedPref;

@SharedPref(value = SharedPref.Scope.UNIQUE)
public interface AppPreferences {

    @DefaultBoolean(true)
    boolean useCompass();

    @DefaultBoolean(true)
    boolean trackPosition();

    @DefaultInt(50)
    int defaultTrackDistanceInMeters();

    @DefaultInt(0)
    int lastSession();

    @DefaultInt(0)
    int lastStreamVolume();

    @DefaultFloat(-1f)
    float campLatitude();

    @DefaultFloat(-1f)
    float campLongitude();
}
