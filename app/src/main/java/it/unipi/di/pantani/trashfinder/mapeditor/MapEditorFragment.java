package it.unipi.di.pantani.trashfinder.mapeditor;

import static it.unipi.di.pantani.trashfinder.Utils.MARKER_ZOOM;
import static it.unipi.di.pantani.trashfinder.Utils.checkPerms;
import static it.unipi.di.pantani.trashfinder.Utils.getEditorSelectedMarker;
import static it.unipi.di.pantani.trashfinder.Utils.getPOIMarkerByMarker;
import static it.unipi.di.pantani.trashfinder.Utils.getTitleFromMarker;
import static it.unipi.di.pantani.trashfinder.Utils.pointLocation;
import static it.unipi.di.pantani.trashfinder.Utils.setEditorSelectedMarker;
import static it.unipi.di.pantani.trashfinder.Utils.updateMapStyleByPreference;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

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
import com.google.maps.android.clustering.Cluster;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.collections.MarkerManager;

import java.util.HashSet;
import java.util.Set;

import it.unipi.di.pantani.trashfinder.POIMarkerWindowAdapter;
import it.unipi.di.pantani.trashfinder.R;
import it.unipi.di.pantani.trashfinder.Utils;
import it.unipi.di.pantani.trashfinder.data.MyItemOnMap;
import it.unipi.di.pantani.trashfinder.data.POIMarker;
import it.unipi.di.pantani.trashfinder.maps.MapsViewModel;

public class MapEditorFragment extends Fragment implements OnMapReadyCallback {
    private GoogleMap mMap;
    private Context context;
    private SharedPreferences sp;
    private MapsViewModel mMapViewModel;
    private ClusterManager<MyItemOnMap> clusterManager;

    public boolean firstLoad = true;

    private ProgressBar progressBar;
    private LinearLayout mapeditor_section_form_empty;
    private LinearLayout mapeditor_section_form_new;
    private LinearLayout mapeditor_section_form_form;
    private TextView mapeditor_id;
    private Button mapeditor_types;
    private EditText mapeditor_notes;
    private Button mapeditor_delete;
    private Button mapeditor_save;
    private Button mapeditor_leave;
    private Button mapeditor_leave_new;
    private Button mapeditor_save_new;
    private Button mapeditor_types_new;
    private EditText mapeditor_notes_new;
    private ImageView mapeditor_markericon;

    private Marker markerSelected;
    private Set<POIMarker.MarkerType> markerTypes;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        sp = PreferenceManager.getDefaultSharedPreferences(context);
        // inizializzazione view
        View view = inflater.inflate(R.layout.fragment_map_editor, container, false);
        // inizializzazione fragment mappa
        SupportMapFragment supportMapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.google_map);
        if (supportMapFragment == null) return view;
        // mappa asincrona
        supportMapFragment.getMapAsync(this);
        // barra di caricamento
        progressBar = view.findViewById(R.id.progressbar);
        // view model
        mMapViewModel = new ViewModelProvider(this).get(MapsViewModel.class);
        // layout (sei in modalità editor)
        mapeditor_section_form_empty = view.findViewById(R.id.mapeditor_section_form_empty);
        // layout (crea nuovo)
        mapeditor_section_form_new = view.findViewById(R.id.mapeditor_section_form_new);
        // listener layout vuoto
        mapeditor_section_form_empty.setOnClickListener(this::onClickEmptyForm);
        // layout (form)
        mapeditor_section_form_form = view.findViewById(R.id.mapeditor_section_form_form);
        // note
        mapeditor_notes = view.findViewById(R.id.mapeditor_notes);
        // button tipi marker
        mapeditor_types = view.findViewById(R.id.mapeditor_types);
        // button leave
        mapeditor_leave = view.findViewById(R.id.mapeditor_leave);
        // button tipi marker new
        mapeditor_types_new = view.findViewById(R.id.mapeditor_types_new);
        // note new
        mapeditor_notes_new = view.findViewById(R.id.mapeditor_notes_new);
        // button esci nuovo marker
        mapeditor_leave_new = view.findViewById(R.id.mapeditor_leave_new);
        // button delete
        mapeditor_delete = view.findViewById(R.id.mapeditor_delete);
        // button save
        mapeditor_save = view.findViewById(R.id.mapeditor_save);
        // button create new
        mapeditor_save_new = view.findViewById(R.id.mapeditor_save_new);
        // image marker icon add
        mapeditor_markericon = view.findViewById(R.id.mapeditor_markericon);
        // leave
        mapeditor_leave.setOnClickListener(this::onClickLeave);
        // listener esci nuovo marker
        mapeditor_leave_new.setOnClickListener(this::onClickLeaveNew);
        // applico il listener al pulsante "tipo marcatore"
        mapeditor_types.setOnClickListener(this::onClickMarkerTypes);
        // delete
        mapeditor_delete.setOnClickListener(this::onClickDelete);
        // save
        mapeditor_save.setOnClickListener(this::onClickSave);
        // create
        mapeditor_save_new.setOnClickListener(this::onClickCreateNew);
        // button types new
        mapeditor_types_new.setOnClickListener(this::onClickMarkerTypesNew);
        // preparo markerTypes
        markerTypes = new HashSet<>();
        // return della view
        return view;
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        Log.d("ISTANZA", "mapeditor -> onMapReady");
        mMap = googleMap;

        // attivo la modalità location enabled
        if (checkPerms(context))
            mMap.setMyLocationEnabled(true);

        // aggiorno lo stile di mappa
        updateMapStyleByPreference(context, sp, mMap);

        if (firstLoad) {
            pointLocation(context, mMap);
            initializeClusterSystem();
            firstLoad = false;
        }
    }

    public void initializeClusterSystem() {
        progressBar.setVisibility(View.VISIBLE);

        // Initialize the manager with the context and the map.
        // (Activity extends context, so we can pass 'this' in the constructor.)
        clusterManager = new ClusterManager<>(context, mMap);
        MarkerManager.Collection markerCollection = clusterManager.getMarkerCollection();
        mMap.setOnCameraIdleListener(clusterManager);
        clusterManager.setOnClusterClickListener(this::onClusterClick);
        markerCollection.setInfoWindowAdapter(new POIMarkerWindowAdapter(context, 1));
        markerCollection.setOnMarkerClickListener(this::onMarkerClick);

        // aggiungo marker alla mappa
        mMapViewModel.getNearMarkers().observe(getViewLifecycleOwner(), markers -> {
            Log.d("ISTANZA", "mapeditor -> inizializzazione sistema cluster");
            for (POIMarker m : markers) {
                clusterManager.addItem(new MyItemOnMap(m.getLatitude(), m.getLongitude(), getTitleFromMarker(context, m), new Gson().toJson(m)));
            }
            clusterManager.cluster();

            if (markers.size() != 0) {
                progressBar.setVisibility(View.INVISIBLE);
            }
        });
    }

    private boolean onMarkerClick(Marker marker) {
        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(new LatLng(marker.getPosition().latitude, marker.getPosition().longitude))
                .zoom(MARKER_ZOOM)
                .build();
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));

        if(!marker.equals(getEditorSelectedMarker())) {
            enableEditingMode(marker);
        } else {
            disableEditingMode();
        }

        return true;
    }

    private void enableEditingMode(Marker marker) {
        disableCreationMode();

        markerSelected = marker;

        POIMarker m = getPOIMarkerByMarker(marker);
        setEditorSelectedMarker(marker);

        mapeditor_section_form_empty.setVisibility(View.GONE);
        mapeditor_section_form_form.setVisibility(View.VISIBLE);

        mapeditor_types.setTag(m);
        mapeditor_notes.setText(m.getNotes(), TextView.BufferType.EDITABLE);

        markerTypes.addAll(m.getType());

        mMap.getUiSettings().setScrollGesturesEnabled(false);
        mMap.getUiSettings().setZoomGesturesEnabled(false);
        mMap.getUiSettings().setMyLocationButtonEnabled(false);
    }

    private void disableEditingMode() {
        markerSelected = null;

        // chiude la tastiera
        if(getActivity() != null)
            Utils.closeKeyboard(getActivity());

        setEditorSelectedMarker(null);

        mapeditor_section_form_empty.setVisibility(View.VISIBLE);
        mapeditor_section_form_form.setVisibility(View.GONE);

        mapeditor_types.setTag(null);
        mapeditor_notes.setText("");

        markerTypes.clear();

        mMap.getUiSettings().setScrollGesturesEnabled(true);
        mMap.getUiSettings().setZoomGesturesEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(true);
    }

    private boolean onClusterClick(Cluster<MyItemOnMap> item) {
        return true;
    }

    public void onClickEmptyForm(View view) {
        enableCreationMode();
    }

    public void onClickLeaveNew(View view) {
        disableCreationMode();
    }

    public void onClickLeave(View view) {
        disableEditingMode();
    }

    public void onClickCreateNew(View view) {
        LatLng centerPoint = mMap.getCameraPosition().target;
        POIMarker newM = new POIMarker(markerTypes, centerPoint.latitude, centerPoint.longitude, mapeditor_notes_new.getText().toString());

        mMapViewModel.insert(newM);
        clusterManager.addItem(new MyItemOnMap(centerPoint.latitude, centerPoint.longitude, getTitleFromMarker(context,newM), new Gson().toJson(newM)));
        clusterManager.cluster();

        disableCreationMode();
    }

    public void onClickMarkerTypesNew(View view) {
        saveEditMarkerType(view);
    }

    public void enableCreationMode() {
        mapeditor_markericon.setVisibility(View.VISIBLE);

        mapeditor_section_form_empty.setVisibility(View.GONE);
        mapeditor_section_form_new.setVisibility(View.VISIBLE);
    }

    public void disableCreationMode() {
        // chiude la tastiera
        if(getActivity() != null)
            Utils.closeKeyboard(getActivity());

        mapeditor_markericon.setVisibility(View.GONE);

        mapeditor_section_form_empty.setVisibility(View.VISIBLE);
        mapeditor_section_form_new.setVisibility(View.GONE);
    }


    public void onClickMarkerTypes(View view) {
        saveEditMarkerType(view);
    }

    private void saveEditMarkerType(View view) {
        Set<POIMarker.MarkerType> tempTypes = new HashSet<>();

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(view.getResources().getString(R.string.mapeditor_markertype));

        // Popolo la checkbox list
        POIMarker.MarkerType[] elements = new POIMarker.MarkerType[POIMarker.MarkerType.values().length];
        String[] types = new String[POIMarker.MarkerType.values().length];
        boolean[] checkedItems = new boolean[POIMarker.MarkerType.values().length];

        int i = 0;
        for(POIMarker.MarkerType t : POIMarker.MarkerType.values()) {
            elements[i] = t;
            types[i] = POIMarker.getMarkerTypeName(view.getContext(), t);
            checkedItems[i] = markerTypes.contains(t);
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

        builder.setPositiveButton(R.string.button_ok, (dialog, which) -> markerTypes = tempTypes);
        builder.setNegativeButton(R.string.button_cancel, null);

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    public void onClickDelete(View view) {
        POIMarker poiMarkerSelected = (POIMarker) mapeditor_types.getTag();

        AlertDialog alertDialog = new AlertDialog.Builder(view.getContext()).create();
        alertDialog.setTitle(getResources().getString(R.string.mapeditor_suredelete));
        alertDialog.setMessage(getResources().getString(R.string.mapeditor_suredelete_desc, poiMarkerSelected.getId()));
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, getResources().getString(R.string.button_cancel),
                (dialog, which) -> dialog.dismiss());
        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, getResources().getString(R.string.button_ok),
                (dialog, which) -> {
                    // solo se premo ok
                    mMapViewModel.delete(poiMarkerSelected);
                    markerSelected.remove();

                    disableEditingMode();
                });
        alertDialog.show();
    }

    public void onClickSave(View view) {
        POIMarker poiMarkerSelected = (POIMarker) mapeditor_types.getTag();

        mMapViewModel.update(poiMarkerSelected.getId(), markerTypes, poiMarkerSelected.getLatitude(), poiMarkerSelected.getLongitude(), mapeditor_notes.getText().toString());

        disableEditingMode();
    }

    // -------------

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
        Log.d("ISTANZA", "mapeditor -> onAttach");
    }

    @Override
    public void onDetach() {
        super.onDetach();
        context = null;
        Log.d("ISTANZA", "mapeditor -> onDetach");
    }

    @Override
    public void onResume() {
        super.onResume();
        updateMapStyleByPreference(context, sp, mMap);
        Log.d("ISTANZA", "mapeditor -> onResume");
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d("ISTANZA", "mapeditor -> onPause");
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.d("ISTANZA", "mapeditor -> onSaveIstanceState");
    }
}