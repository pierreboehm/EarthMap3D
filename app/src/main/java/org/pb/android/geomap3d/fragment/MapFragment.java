package org.pb.android.geomap3d.fragment;

import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.ViewById;
import org.pb.android.geomap3d.R;
import org.pb.android.geomap3d.fragment.ui.MapView;

import androidx.fragment.app.Fragment;

@EFragment(R.layout.map_fragment)
public class MapFragment extends Fragment {

    public static final String TAG = MapFragment.class.getSimpleName();

    @ViewById(R.id.mapView)
    MapView mapView;

}
