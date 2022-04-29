package it.unipi.di.pantani.trashfinder.home;

import static it.unipi.di.pantani.trashfinder.Utils.MARKER_ZOOM;
import static it.unipi.di.pantani.trashfinder.Utils.checkPerms;
import static it.unipi.di.pantani.trashfinder.Utils.getCompassSelectedMarker;
import static it.unipi.di.pantani.trashfinder.Utils.getMarkerColorByType;
import static it.unipi.di.pantani.trashfinder.Utils.getTitleFromMarker;
import static it.unipi.di.pantani.trashfinder.Utils.pointLocation;
import static it.unipi.di.pantani.trashfinder.Utils.setCompassSelectedMarker;
import static it.unipi.di.pantani.trashfinder.Utils.updateMapStyleByPreference;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.snackbar.Snackbar;

import it.unipi.di.pantani.trashfinder.POIMarkerWindowAdapter;
import it.unipi.di.pantani.trashfinder.data.POIMarker;
import trashfinder.R;


public class MapsFragment extends Fragment implements OnMapReadyCallback {
    private GoogleMap mMap;
    private Context context;
    private SharedPreferences sp;

    private MapsViewModel mMapViewModel;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        sp = PreferenceManager.getDefaultSharedPreferences(context);
        // inizializzazione view
        View view = inflater.inflate(R.layout.fragment_maps, container, false);

        // inizializzazione fragment mappa
        SupportMapFragment supportMapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.google_map);

        if(supportMapFragment == null) return view; // in caso di problemi

        // mappa asincrona
        supportMapFragment.getMapAsync(this);

        // view model
        mMapViewModel = new ViewModelProvider(this).get(MapsViewModel.class);

        // return della view
        return view;
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        mMap.setInfoWindowAdapter(new POIMarkerWindowAdapter(context));

        mMap.setOnMarkerClickListener(marker -> {
            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(new LatLng(marker.getPosition().latitude, marker.getPosition().longitude))
                    .zoom(MARKER_ZOOM)
                    .build();
            mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
            // Show the info window (as the overloaded method would)
            marker.showInfoWindow();
            return true; // Consume the event since it was dealt with
        });

        if (checkPerms(context)) {
            mMap.setMyLocationEnabled(true);
        }
        // aggiorna lo stile di mappa
        updateMapStyleByPreference(context, sp, mMap);
        // punta la mappa sulla posizione attuale
        pointLocation(context, mMap);


        Snackbar bar = Snackbar.make(getActivity().findViewById(android.R.id.content), R.string.loading, Snackbar.LENGTH_INDEFINITE);
        Snackbar complete = Snackbar.make(getActivity().findViewById(android.R.id.content), R.string.loading_complete, Snackbar.LENGTH_SHORT);
        ViewGroup contentLay = (ViewGroup) bar.getView().findViewById(com.google.android.material.R.id.snackbar_text).getParent();
        ProgressBar item = new ProgressBar(context);
        item.setIndeterminate(true);
        contentLay.addView(item,0);
        bar.show();



        mMapViewModel.getNearMarkers().observe(getViewLifecycleOwner(), markers -> {
            for(POIMarker m : markers) {
                Marker newMarker = mMap.addMarker(new MarkerOptions()
                .position(new LatLng(m.getLatitude(), m.getLongitude()))
                .title(getTitleFromMarker(context, m))
                .snippet(m.getNotes())
                .icon(BitmapDescriptorFactory.defaultMarker(getMarkerColorByType(m))));

                if(newMarker != null) // imposto come tag il marker
                    newMarker.setTag(m);
            }

            if(markers.size() != 0) { // se i dati sono stati caricati completamente levo il caricamento
                bar.dismiss();
                complete.show();
            }
        });

        mMap.setOnInfoWindowClickListener(marker -> {
            if(!marker.equals(getCompassSelectedMarker())) {
                setCompassSelectedMarker(marker);
                Toast.makeText(context, getResources().getString(R.string.infowindow_setcompass), Toast.LENGTH_LONG).show();
                marker.hideInfoWindow();
            }
        });
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
        Log.d("ISTANZA", "maps -> onAttach");
    }

    @Override
    public void onDetach() {
        super.onDetach();
        Log.d("ISTANZA", "maps -> onDetach");
    }

    @Override
    public void onResume() {
        super.onResume();
        updateMapStyleByPreference(context, sp, mMap);
        Log.d("ISTANZA", "maps -> onResume");
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d("ISTANZA", "maps -> onPause");
    }
}