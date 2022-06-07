/*
 * Copyright (c) 2021/2022
 * Leonardo Pantani - 598896
 * University of Pisa - Department of Computer Science
 */

package it.unipi.di.pantani.trashfinder.maps;

import static it.unipi.di.pantani.trashfinder.Utils.MARKER_ZOOM;
import static it.unipi.di.pantani.trashfinder.Utils.checkPerms;
import static it.unipi.di.pantani.trashfinder.Utils.getCompassSelectedMarker;
import static it.unipi.di.pantani.trashfinder.Utils.pointLocation;
import static it.unipi.di.pantani.trashfinder.Utils.setCompassSelectedMarker;
import static it.unipi.di.pantani.trashfinder.Utils.updateMapStyleByPreference;
import static it.unipi.di.pantani.trashfinder.data.marker.POIMarker.getTitleFromMarker;
import static it.unipi.di.pantani.trashfinder.maps.POIMarkerWindowAdapter.areMarkersEqual;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.preference.PreferenceManager;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
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
import com.google.gson.reflect.TypeToken;
import com.google.maps.android.clustering.Cluster;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.algo.NonHierarchicalViewBasedAlgorithm;
import com.google.maps.android.collections.MarkerManager;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import it.unipi.di.pantani.trashfinder.R;
import it.unipi.di.pantani.trashfinder.Utils;
import it.unipi.di.pantani.trashfinder.data.marker.MyItemOnMap;
import it.unipi.di.pantani.trashfinder.data.marker.POIMarker;

public class MapsFragment extends Fragment implements OnMapReadyCallback {
    private GoogleMap mMap;
    private Context mContext;
    private SharedPreferences sp;
    private MapsViewModel mMapViewModel;
    private ProgressBar mProgressBar;
    private ClusterManager<MyItemOnMap> mClusterManager;
    private Bundle mBundle;
    private HashSet<POIMarker.MarkerType> mMarkerTypes;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        sp = PreferenceManager.getDefaultSharedPreferences(mContext);
        setHasOptionsMenu(true);
        // inizializzazione view
        View view = inflater.inflate(R.layout.fragment_maps, container, false);
        // inizializzazione fragment mappa
        SupportMapFragment supportMapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.google_map);
        if(supportMapFragment == null) return view;
        // mappa asincrona
        supportMapFragment.getMapAsync(this);
        // barra di caricamento
        mProgressBar = view.findViewById(R.id.progressbar);
        // menù filtraggio
        initializeFilter();
        // view model
        mMapViewModel = new ViewModelProvider(this).get(MapsViewModel.class);
        // salvo il bundle
        mBundle = savedInstanceState;
        // return della view
        return view;
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        Log.d("ISTANZA", "maps -> onMapReady");
        mMap = googleMap;

        // attivo la modalità location enabled
        if(checkPerms(mContext))
            mMap.setMyLocationEnabled(true);

        // aggiorno lo stile di mappa
        updateMapStyleByPreference(mContext, mMap);

        initializeClusterSystem();
    }

    public void initializeClusterSystem() {
        Log.d("ISTANZA", "maps -> inizializzazione sistema cluster");
        if(mClusterManager != null) return;

        if(mBundle != null) {
            CameraPosition cp = mBundle.getParcelable("cp");
            if(cp != null)
                mMap.moveCamera(CameraUpdateFactory.newCameraPosition(cp));
        } else {
            pointLocation(mContext, mMap);
        }

        mClusterManager = new ClusterManager<>(mContext, mMap);
        mClusterManager.setAlgorithm(new NonHierarchicalViewBasedAlgorithm<>(Utils.getScreenWidth(), Utils.getScreenHeight()));
        MarkerManager.Collection markerCollection = mClusterManager.getMarkerCollection();
        mMap.setOnCameraIdleListener(mClusterManager);
        mMap.setOnInfoWindowCloseListener(this::onInfoWindowClose);
        mClusterManager.setOnClusterClickListener(this::onClusterClick);
        markerCollection.setInfoWindowAdapter(new POIMarkerWindowAdapter(mContext, 0));
        markerCollection.setOnMarkerClickListener(this::onMarkerClick);
        markerCollection.setOnInfoWindowClickListener(this::onInfoWindowClick);

        mProgressBar.setVisibility(View.VISIBLE);
        refreshMap();
    }

    private void onInfoWindowClose(Marker marker) {
        if(getCompassSelectedMarker() == null && getActivity() != null) {
            ExtendedFloatingActionButton a = getActivity().findViewById(R.id.fab);
            a.hide();
        }
    }

    @SuppressWarnings("SameReturnValue")
    private boolean onClusterClick(Cluster<MyItemOnMap> item) {
        return true;
    }

    @SuppressWarnings("SameReturnValue")
    private boolean onMarkerClick(Marker marker) {
        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(new LatLng((marker.getPosition().latitude+0.00025), marker.getPosition().longitude))
                .zoom(MARKER_ZOOM)
                .bearing(0)
                .build();
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        marker.showInfoWindow();

        // mostro il pulsante per la navigazione
        LatLng selectedMarkerCoordinates = marker.getPosition();
        Uri gmmIntentUri = Uri.parse("google.navigation:q=" + selectedMarkerCoordinates.latitude + "," + selectedMarkerCoordinates.longitude);
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage("com.google.android.apps.maps");

        // mostro il pulsante "naviga" solo se c'è un app che supporta l'intent di navigazione
        if(Utils.canNavigate(mContext) && getActivity() != null) {
            ExtendedFloatingActionButton a = getActivity().findViewById(R.id.fab);
            a.setOnClickListener(view -> startActivity(mapIntent));
            a.show();
        }

        return true;
    }

    private void onInfoWindowClick(Marker marker) {
        if(getActivity() == null) return; // per rimuovere un warning

        if(!areMarkersEqual(marker, getCompassSelectedMarker())) {
            setCompassSelectedMarker(marker);

            Snackbar mySnackbar = Snackbar.make(getActivity().findViewById(R.id.maps), getResources().getString(R.string.infowindow_setcompass), Snackbar.LENGTH_SHORT);
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

    private void initializeFilter() {
        mMarkerTypes = new HashSet<>();
        Gson gson = new Gson();
        Type type = new TypeToken<HashSet<POIMarker.MarkerType>>() {}.getType();

        // recupero da preferenze
        String toConvert = sp.getString("setting_markerfilter", "");
        if(toConvert.equals("")) {
            mMarkerTypes.addAll(Arrays.asList(POIMarker.MarkerType.values()));
        } else {
            mMarkerTypes = gson.fromJson(toConvert, type);
        }
    }

    // -- Condivisi (salvataggio della select dei tipi di cestini)
    public void clickFilterMap() {
        HashSet<POIMarker.MarkerType> tempTypes = new HashSet<>();

        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle(mContext.getResources().getString(R.string.maps_filter_title));

        // Popolo la checkbox list
        POIMarker.MarkerType[] elements = new POIMarker.MarkerType[POIMarker.MarkerType.values().length];
        String[] types = new String[POIMarker.MarkerType.values().length];
        boolean[] checkedItems = new boolean[POIMarker.MarkerType.values().length];

        int i = 0;
        for(POIMarker.MarkerType t : POIMarker.MarkerType.values()) {
            elements[i] = t;
            types[i] = POIMarker.getMarkerTypeName(mContext, t);
            checkedItems[i] = mMarkerTypes.contains(t);
            if(checkedItems[i]) {
                tempTypes.add(t);
            }
            i++;
        }

        builder.setMultiChoiceItems(types, checkedItems, (dialog, which, isChecked) -> {
            if(isChecked) {
                tempTypes.add(elements[which]);
            } else {
                tempTypes.remove(elements[which]);
            }
        });

        builder.setPositiveButton(R.string.button_ok, (dialog, which) -> {
            mMarkerTypes = tempTypes;
            Gson gson = new Gson();
            Type type = new TypeToken<HashSet<POIMarker.MarkerType>>() {}.getType();
            Utils.setPreference(mContext, "setting_markerfilter", gson.toJson(mMarkerTypes, type));

            mProgressBar.setVisibility(View.VISIBLE);
            final Handler handler = new Handler(Looper.getMainLooper());
            handler.postDelayed(this::refreshMap, 250);
        });
        builder.setNegativeButton(R.string.button_cancel, null);
        builder.setNeutralButton(R.string.maps_bulkeditfilterbutton, null);

        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(dialogInterface -> {
            Button button = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);
            button.setOnClickListener(view -> {
                ListView checkboxlist = dialog.getListView();
                boolean newValue;
                if(tempTypes.size() == POIMarker.MarkerType.values().length) { // se son selezionati tutti
                    newValue = false;
                } else if(tempTypes.size() == 0) { // non ce n'è nessuno selezionato
                    newValue = true;
                } else { // alcuni sono selezionati
                    newValue = true;
                }
                for (int j = 0; j < checkboxlist.getAdapter().getCount(); j++) {
                    checkboxlist.setItemChecked(j, newValue);
                    checkedItems[j] = newValue;
                    if(!newValue) {
                        tempTypes.remove(elements[j]);
                    } else {
                        tempTypes.add(elements[j]);
                    }
                }
            });
        });

        dialog.show();
    }

    /**
     * Specifica se il marcatore attuale deve essere aggiunto in mappa o no.
     * @param elem l'elemento da aggiungere
     * @return vero se l'elemento andrebbe aggiunto, falso altrimenti
     */
    private boolean shallAddMarker(Set<POIMarker.MarkerType> elem) {
        HashSet<POIMarker.MarkerType> a = new HashSet<>(elem);
        a.retainAll(mMarkerTypes);

        return 0 < a.size();
    }

    /**
     * Aggiunge i marcatori alla mappa.
     */
    private void refreshMap() {
        mClusterManager.clearItems();

        mMapViewModel.getNearMarkers().observe(getViewLifecycleOwner(), markers -> {
            Log.d("ISTANZA", "maps -> aggiornamento marcatori su mappa");
            Gson gson = new Gson();

            for(POIMarker m : markers) {
                if(shallAddMarker(m.getTypes())) {
                    mClusterManager.addItem(new MyItemOnMap(m.getLatitude(), m.getLongitude(), getTitleFromMarker(mContext, m), gson.toJson(m)));
                }
            }
            mClusterManager.cluster();

            if(markers.size() != 0) {
                mProgressBar.setVisibility(View.INVISIBLE);
                Log.d("ISTANZA", "maps -> aggiornamento ai marcatori su mappa applicato");
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.maps_filter) {
            clickFilterMap();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.maps_filter).setVisible(true);
        super.onPrepareOptionsMenu(menu);
    }

    // -------------

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.mContext = context;
        Log.d("ISTANZA", "maps -> onAttach");
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mContext = null;
        Log.d("ISTANZA", "maps -> onDetach");
    }

    @Override
    public void onResume() {
        super.onResume();
        updateMapStyleByPreference(mContext, mMap);
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
        if(mMap != null) outState.putParcelable("cp", mMap.getCameraPosition());
        Log.d("ISTANZA", "maps -> onSaveIstanceState");
    }
}