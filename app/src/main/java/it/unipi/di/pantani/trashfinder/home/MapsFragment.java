package it.unipi.di.pantani.trashfinder.home;

import static it.unipi.di.pantani.trashfinder.Utils.checkPerms;
import static it.unipi.di.pantani.trashfinder.Utils.pointLocation;
import static it.unipi.di.pantani.trashfinder.Utils.updateMapStyleByPreference;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import trashfinder.R;


public class MapsFragment extends Fragment implements OnMapReadyCallback {
    private GoogleMap mMap;
    private Context context;
    private SharedPreferences sp;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
    }

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

        // return della view
        return view;
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        if (checkPerms(context)) {
            mMap.setMyLocationEnabled(true);
        }
        // aggiorna lo stile di mappa
        updateMapStyleByPreference(context, sp, mMap);
        // punta la mappa sulla posizione attuale
        pointLocation(context, mMap);

        // biliardo (test)
        LatLng t = new LatLng(43.7231751360382, 10.435376588242777);
        mMap.addMarker(new MarkerOptions().position(t).title("Biliardo"));

        // guardare video su infowindows
        // https://www.youtube.com/watch?v=DhYofrJPzlI
    }

    @Override
    public void onResume() {
        super.onResume();
        updateMapStyleByPreference(context, sp, mMap);
    }
}