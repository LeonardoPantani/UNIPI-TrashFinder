package it.unipi.di.pantani.trashfinder.maps;

import static it.unipi.di.pantani.trashfinder.Utils.MARKER_ZOOM;
import static it.unipi.di.pantani.trashfinder.Utils.checkPerms;
import static it.unipi.di.pantani.trashfinder.Utils.getCompassSelectedMarker;
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
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.gson.Gson;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.collections.MarkerManager;

import it.unipi.di.pantani.trashfinder.POIMarkerWindowAdapter;
import it.unipi.di.pantani.trashfinder.data.MyItemOnMap;
import it.unipi.di.pantani.trashfinder.data.POIMarker;
import trashfinder.R;


public class MapsFragment extends Fragment implements OnMapReadyCallback {
    private GoogleMap mMap;
    private Context context;
    private SharedPreferences sp;
    private MapsViewModel mMapViewModel;

    private Bundle bundle;

    // Declare a variable for the cluster manager.
    private ClusterManager<MyItemOnMap> clusterManager;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        sp = PreferenceManager.getDefaultSharedPreferences(context);
        // inizializzazione view
        View view = inflater.inflate(R.layout.fragment_maps, container, false);
        // inizializzazione fragment mappa
        SupportMapFragment supportMapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.google_map);
        if(supportMapFragment == null) return view;
        // mappa asincrona
        supportMapFragment.getMapAsync(this);
        // view model
        mMapViewModel = new ViewModelProvider(this).get(MapsViewModel.class);
        // mi salvo il bundle
        bundle = savedInstanceState;
        // return della view
        return view;
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        ProgressBar pb = getActivity().findViewById(R.id.progressbar);
        pb.setVisibility(View.VISIBLE);

        Log.d("ISTANZA", "maps -> onMapReady");
        mMap = googleMap;

        // Initialize the manager with the context and the map.
        // (Activity extends context, so we can pass 'this' in the constructor.)
        clusterManager = new ClusterManager<>(context, mMap);
        MarkerManager.Collection markerCollection = clusterManager.getMarkerCollection();
        mMap.setOnCameraIdleListener(clusterManager);
        markerCollection.setInfoWindowAdapter(new POIMarkerWindowAdapter(context));
        markerCollection.setOnMarkerClickListener(this::onMarkerClick);
        markerCollection.setOnInfoWindowClickListener(this::onInfoWindowClick);

        // imposto l'adapter delle infowindow su quello mio custom e attivo i listener
        //mMap.setInfoWindowAdapter(new POIMarkerWindowAdapter(context));
        //mMap.setOnMarkerClickListener(this::onMarkerClick);
        //mMap.setOnInfoWindowClickListener(this::onInfoWindowClick);
        // attivo la modalità location enabled
        if(checkPerms(context))
            mMap.setMyLocationEnabled(true);

        // aggiorno lo stile di mappa
        updateMapStyleByPreference(context, sp, mMap);

        // se è la prima volta che apro la mappa (sono alle coordinate 0.0, 0)
        // nota: per un problema di maps, la latitudine al primo avvio dell'app non è 0.0 preciso
        if(mMap.getCameraPosition().target.longitude == 0 && (mMap.getCameraPosition().target.latitude == 0.06392038032682339) || (mMap.getCameraPosition().target.latitude == 0.0f)) {
            pointLocation(context, mMap);
        }

        // aggiungo marker alla mappa
        clusterManager.clearItems();
        mMapViewModel.getNearMarkers().observe(getViewLifecycleOwner(), markers -> {
            for(POIMarker m : markers) {
                clusterManager.addItem(new MyItemOnMap(m.getLatitude(), m.getLongitude(), getTitleFromMarker(context, m), new Gson().toJson(m)));
            }
            clusterManager.cluster();

            if(markers.size() != 0) {
                pb.setVisibility(View.INVISIBLE);
            }
        });
    }

    private boolean onMarkerClick(Marker marker) {
        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(new LatLng(marker.getPosition().latitude, marker.getPosition().longitude))
                .zoom(MARKER_ZOOM)
                .build();
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        marker.showInfoWindow();
        return true;
    }

    private void onInfoWindowClick(Marker marker) {
        if(!marker.equals(getCompassSelectedMarker())) {
            setCompassSelectedMarker(marker);
            Toast.makeText(context, getResources().getString(R.string.infowindow_setcompass), Toast.LENGTH_LONG).show();
            marker.hideInfoWindow();
        }
    }


    // -------------

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
        Log.d("ISTANZA", "maps -> onAttach");
    }

    @Override
    public void onDetach() {
        super.onDetach();
        context = null;
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

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.d("ISTANZA", "maps -> onSaveIstanceState");
    }
}