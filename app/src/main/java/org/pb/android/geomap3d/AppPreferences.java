package org.pb.android.geomap3d;

import org.androidannotations.annotations.sharedpreferences.DefaultBoolean;
import org.androidannotations.annotations.sharedpreferences.DefaultInt;
import org.androidannotations.annotations.sharedpreferences.SharedPref;

@SharedPref(value = SharedPref.Scope.UNIQUE)
public interface AppPreferences {

    @DefaultBoolean(true)
    boolean useCompass();

    @DefaultInt(250)
    int defaultTrackDistanceInMeters();
}
