package it.unipi.di.pantani.trashfinder.mapeditor;

import static it.unipi.di.pantani.trashfinder.Utils.checkPerms;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.MarkerOptions;

import it.unipi.di.pantani.trashfinder.R;

public class MapEditorFragment extends Fragment implements OnMapReadyCallback {
    private GoogleMap mMap;
    private Context context;
    private SharedPreferences sp;
    private SupportMapFragment supportMapFragment;
    int i = 0;

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
        supportMapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.google_map);

        if(supportMapFragment == null) return view; // in caso di problemi

        // mappa asincrona
        supportMapFragment.getMapAsync(this);

        // return della view
        return view;
    }

    public void updateMapStyleByPreference() {
        if(mMap == null) return; // appena avviata l'app, questo metodo è chiamato dalla "onResume" con mMap non valido quindi ignoro

        String setting_map_theme = sp.getString("setting_map_theme", "a");
        if ("classic".equals(setting_map_theme)) {
            mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(context, R.raw.map_style_classic));
        } else if ("light".equals(setting_map_theme)) {
            mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(context, R.raw.map_style_light));
        } else if ("dark".equals(setting_map_theme)) {
            mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(context, R.raw.map_style_dark));
        } else {
            mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(context, R.raw.map_style_classic));
        }
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @SuppressLint("MissingPermission")
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        updateMapStyleByPreference();

        if (checkPerms(context)) {
            mMap.setMyLocationEnabled(true);
        }

        mMap.setOnMapClickListener(point -> {
            MarkerOptions marker = new MarkerOptions().position(new LatLng(point.latitude, point.longitude)).title(context.getResources().getString(R.string.marker) + " " + (++i));
            googleMap.addMarker(marker);
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        updateMapStyleByPreference();
    }
}
