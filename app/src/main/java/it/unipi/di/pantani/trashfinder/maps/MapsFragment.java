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
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;

import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.preference.PreferenceManager;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;
import com.google.maps.android.clustering.Cluster;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.collections.MarkerManager;

import it.unipi.di.pantani.trashfinder.POIMarkerWindowAdapter;
import it.unipi.di.pantani.trashfinder.R;
import it.unipi.di.pantani.trashfinder.data.MyItemOnMap;
import it.unipi.di.pantani.trashfinder.data.POIMarker;

public class MapsFragment extends Fragment implements OnMapReadyCallback {
    private GoogleMap mMap;
    private Context context;
    private SharedPreferences sp;
    private MapsViewModel mMapViewModel;
    private ProgressBar progressBar;

    private ClusterManager<MyItemOnMap> clusterManager;
    private MarkerManager.Collection markerCollection;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        sp = PreferenceManager.getDefaultSharedPreferences(context);
        // inizializzazione view
        View view = inflater.inflate(R.layout.fragment_maps, container, false);
        // inizializzazione fragment mappa
        SupportMapFragment supportMapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.google_map);
        if(supportMapFragment == null) return view;
        // mappa asincrona
        supportMapFragment.getMapAsync(this);
        // barra di caricamento
        progressBar = view.findViewById(R.id.progressbar);
        // view model
        mMapViewModel = new ViewModelProvider(this).get(MapsViewModel.class);
        // return della view
        return view;
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        Log.d("ISTANZA", "maps -> onMapReady");
        mMap = googleMap;

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

        initializeClusterSystem();
    }

    public void initializeClusterSystem() {
        if(clusterManager != null) return;
        progressBar.setVisibility(View.VISIBLE);

        clusterManager = new ClusterManager<>(context, mMap);
        markerCollection = clusterManager.getMarkerCollection();
        mMap.setOnCameraIdleListener(clusterManager);
        mMap.setOnInfoWindowCloseListener(this::onInfoWindowClose);
        clusterManager.setOnClusterClickListener(this::onClusterClick);
        markerCollection.setInfoWindowAdapter(new POIMarkerWindowAdapter(context, 0));
        markerCollection.setOnMarkerClickListener(this::onMarkerClick);
        markerCollection.setOnInfoWindowClickListener(this::onInfoWindowClick);

        // aggiungo marker alla mappa
        mMapViewModel.getNearMarkers().observe(getViewLifecycleOwner(), markers -> {
            Log.d("ISTANZA", "maps -> inizializzazione sistema cluster");
            for(POIMarker m : markers) {
                clusterManager.addItem(new MyItemOnMap(m.getLatitude(), m.getLongitude(), getTitleFromMarker(context, m), new Gson().toJson(m)));
            }
            clusterManager.cluster();

            if(markers.size() != 0) {
                progressBar.setVisibility(View.INVISIBLE);
            }
        });
    }

    private void onInfoWindowClose(Marker marker) {
        if(getCompassSelectedMarker() == null && getActivity() != null) {
            ExtendedFloatingActionButton a = getActivity().findViewById(R.id.fab);
            a.hide();
        }
    }

    private boolean onMarkerClick(Marker marker) {
        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(new LatLng(marker.getPosition().latitude, marker.getPosition().longitude))
                .zoom(MARKER_ZOOM)
                .build();
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        marker.showInfoWindow();

        // mostro il pulsante per la navigazione
        LatLng selectedMarkerCoordinates = marker.getPosition();
        Uri gmmIntentUri = Uri.parse("google.navigation:q=" + selectedMarkerCoordinates.latitude + "," + selectedMarkerCoordinates.longitude + "&mode=w");
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage("com.google.android.apps.maps");

        // mostro il pulsante "naviga" solo se c'è un app che supporta l'intent di navigazione
        if(mapIntent.resolveActivity(context.getPackageManager()) != null && getActivity() != null) {
            ExtendedFloatingActionButton a = getActivity().findViewById(R.id.fab);
            a.setOnClickListener(view -> {
                startActivity(mapIntent);
            });
            a.show();
        }

        return true;
    }

    private void onInfoWindowClick(Marker marker) {
        if(!marker.equals(getCompassSelectedMarker())) {
            setCompassSelectedMarker(marker);

            Snackbar mySnackbar = Snackbar.make(getActivity().findViewById(R.id.maps), getResources().getString(R.string.infowindow_setcompass), Snackbar.LENGTH_LONG);
            mySnackbar.setAction(R.string.button_open, view -> {
                // ottengo il navController
                NavHostFragment navHostFragment = (NavHostFragment) getActivity().getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment_content_main);
                if(navHostFragment == null) { // non dovrebbe mai verificarsi!
                    Log.d("ISTANZA", "navHostFragment null (fragment)!");
                    return;
                }
                NavController navController = navHostFragment.getNavController();
                navController.popBackStack(R.id.nav_maps, false);
                navController.navigate(R.id.nav_compass);
            });
            mySnackbar.show();
        } else {
            setCompassSelectedMarker(null);
            Snackbar mySnackbar = Snackbar.make(getActivity().findViewById(R.id.maps), getResources().getString(R.string.infowindow_unsetcompass), Snackbar.LENGTH_SHORT);
            mySnackbar.show();
        }
        marker.hideInfoWindow();
    }

    private boolean onClusterClick(Cluster<MyItemOnMap> item) {
        return true;
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