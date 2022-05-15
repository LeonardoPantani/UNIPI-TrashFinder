package it.unipi.di.pantani.trashfinder.maps;

import static it.unipi.di.pantani.trashfinder.Utils.EDITMODE_NO_CLUSTER_MIN_ZOOM;
import static it.unipi.di.pantani.trashfinder.Utils.MARKER_ZOOM;
import static it.unipi.di.pantani.trashfinder.Utils.checkPerms;
import static it.unipi.di.pantani.trashfinder.Utils.getEditorSelectedMarker;
import static it.unipi.di.pantani.trashfinder.Utils.pointLocation;
import static it.unipi.di.pantani.trashfinder.Utils.setEditorSelectedMarker;
import static it.unipi.di.pantani.trashfinder.Utils.updateMapStyleByPreference;
import static it.unipi.di.pantani.trashfinder.data.marker.POIMarker.getPOIMarkerByMarker;
import static it.unipi.di.pantani.trashfinder.data.marker.POIMarker.getTitleFromMarker;

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

import it.unipi.di.pantani.trashfinder.R;
import it.unipi.di.pantani.trashfinder.Utils;
import it.unipi.di.pantani.trashfinder.data.marker.MyItemOnMap;
import it.unipi.di.pantani.trashfinder.data.marker.POIMarker;
import it.unipi.di.pantani.trashfinder.databinding.EditorEmptyBinding;
import it.unipi.di.pantani.trashfinder.databinding.EditorFormBinding;
import it.unipi.di.pantani.trashfinder.databinding.EditorFormNewBinding;
import it.unipi.di.pantani.trashfinder.databinding.FragmentMapEditorBinding;

public class MapEditorFragment extends Fragment implements OnMapReadyCallback {
    private GoogleMap mMap;
    private Context context;
    private SharedPreferences sp;
    private MapsViewModel mMapViewModel;
    private ClusterManager<MyItemOnMap> clusterManager;

    private FragmentMapEditorBinding binding;

    private EditorEmptyBinding binding_empty;
    private EditorFormBinding binding_form;
    private EditorFormNewBinding binding_form_new;

    public boolean firstLoad = true;

    private POIMarker poiMarkerSelected;
    private Marker markerSelected;
    private Set<POIMarker.MarkerType> markerTypes;

    private Bundle bundle;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        sp = PreferenceManager.getDefaultSharedPreferences(context);
        binding = FragmentMapEditorBinding.inflate(inflater, container, false);
        binding_empty = EditorEmptyBinding.inflate(inflater, container, false);
        binding_form = EditorFormBinding.inflate(inflater, container, false);
        binding_form_new = EditorFormNewBinding.inflate(inflater, container, false);

        // view radice
        View root = binding.getRoot();

        // inizializzazione fragment mappa
        SupportMapFragment supportMapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.google_map);
        if (supportMapFragment == null) return root;

        // aggiungo inizialmente il layout vuoto (se non è nullo lo aggiungo)

        binding.mapeditorSectionForm.addView(binding_empty.getRoot());

        // mappa asincrona
        supportMapFragment.getMapAsync(this);

        // view model
        mMapViewModel = new ViewModelProvider(this).get(MapsViewModel.class);

        // preparo markerTypes
        markerTypes = new HashSet<>();

        // -- LISTENERS SEZIONE VUOTA
        binding_empty.mapeditorEmpty.setOnClickListener(this::onClickEmptyForm);

        // -- LISTENERS SEZIONE EDIT
        binding_form.mapeditorLeave.setOnClickListener(this::onClickLeave);
        binding_form.mapeditorTypes.setOnClickListener(this::saveEditMarkerType);
        binding_form.mapeditorDelete.setOnClickListener(this::onClickDelete);
        binding_form.mapeditorSave.setOnClickListener(this::onClickSave);

        // -- LISTENERS SEZIONE NEW
        binding_form_new.mapeditorLeaveNew.setOnClickListener(this::onClickLeaveNew);
        binding_form_new.mapeditorSaveNew.setOnClickListener(this::onClickCreateNew);
        binding_form_new.mapeditorTypesNew.setOnClickListener(this::saveEditMarkerType);
        binding_form_new.mapeditorPhotoNew.setOnClickListener(this::onClickPhotoNew);

        // salvo il bundle
        bundle = savedInstanceState;

        // return della view
        return root;
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
        updateMapStyleByPreference(context, mMap);

        if (firstLoad) {
            pointLocation(context, mMap);
            initializeClusterSystem();
            firstLoad = false;
        }
    }

    public void initializeClusterSystem() {
        if(bundle != null) {
            mMap.moveCamera(CameraUpdateFactory.newCameraPosition((CameraPosition) bundle.getParcelable("cp")));
        } else {
            pointLocation(context, mMap);
        }

        binding.progressbar.setVisibility(View.VISIBLE);

        // inizializzo cluster manager
        clusterManager = new ClusterManager<>(context, mMap);
        // uso un custom renderer per la modalità modifica
        // questo renderer non mostra i cluster se lo zoom supera una certa soglia (per una migliore esperienza di modifica)
        CustomClusterRenderer customRenderer = new CustomClusterRenderer(context, mMap, clusterManager,mMap.getCameraPosition().zoom, EDITMODE_NO_CLUSTER_MIN_ZOOM);
        clusterManager.setRenderer(customRenderer);
        clusterManager.setOnClusterClickListener(this::onClusterClick);
        mMap.setOnCameraMoveListener(customRenderer);
        mMap.setOnCameraIdleListener(clusterManager);

        MarkerManager.Collection markerCollection = clusterManager.getMarkerCollection();
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
                binding.progressbar.setVisibility(View.INVISIBLE);
            }
        });
    }

    private boolean onClusterClick(Cluster<MyItemOnMap> item) {
        return true;
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

    // -- MODALITA' EDITING
    private void enableEditingMode(Marker marker) {
        mMap.getUiSettings().setScrollGesturesEnabled(false);
        mMap.getUiSettings().setZoomGesturesEnabled(false);
        mMap.getUiSettings().setMyLocationButtonEnabled(false);

        disableCreationMode();

        POIMarker m = getPOIMarkerByMarker(marker);
        poiMarkerSelected = m;
        markerSelected = marker;
        setEditorSelectedMarker(marker);

        binding.mapeditorSectionForm.removeAllViews();
        binding.mapeditorSectionForm.addView(binding_form.getRoot());

        markerTypes.addAll(m.getTypes());
        binding_form.mapeditorNotes.setText(m.getNotes(), TextView.BufferType.EDITABLE);
    }

    private void disableEditingMode() {
        mMap.getUiSettings().setScrollGesturesEnabled(true);
        mMap.getUiSettings().setZoomGesturesEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(true);

        poiMarkerSelected = null;
        markerSelected = null;

        // chiude la tastiera
        if(getActivity() != null) Utils.closeKeyboard(getActivity());

        setEditorSelectedMarker(null);

        binding.mapeditorSectionForm.removeAllViews();
        binding.mapeditorSectionForm.addView(binding_empty.getRoot());

        markerTypes.clear();
        binding_form.mapeditorNotes.setText("");
    }

    private void onClickLeave(View view) {
        disableEditingMode();
    }

    private void onClickDelete(View view) {
        AlertDialog alertDialog = new AlertDialog.Builder(context).create();
        alertDialog.setTitle(getResources().getString(R.string.mapeditor_suredelete));
        alertDialog.setMessage(getResources().getString(R.string.mapeditor_suredelete_desc));
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

    private void onClickSave(View view) {
        mMapViewModel.update(poiMarkerSelected.getId(), markerTypes, poiMarkerSelected.getLatitude(), poiMarkerSelected.getLongitude(), binding_form.mapeditorNotes.getText().toString());

        disableEditingMode();
    }

    // -- MODALITA' CREAZIONE
    private void enableCreationMode() {
        binding.mapeditorMarkericon.setVisibility(View.VISIBLE);

        binding.mapeditorSectionForm.removeAllViews();
        binding.mapeditorSectionForm.addView(binding_form_new.getRoot());
    }

    private void disableCreationMode() {
        if(getActivity() != null) Utils.closeKeyboard(getActivity()); // chiude la tastiera

        binding.mapeditorMarkericon.setVisibility(View.GONE);

        binding.mapeditorSectionForm.removeAllViews();
        binding.mapeditorSectionForm.addView(binding_empty.getRoot());
    }

    private void onClickEmptyForm(View view) {
        enableCreationMode();
    }

    private void onClickLeaveNew(View view) {
        disableCreationMode();
    }

    private void onClickCreateNew(View view) {
        LatLng centerPoint = mMap.getCameraPosition().target;
        POIMarker newM = new POIMarker(markerTypes, centerPoint.latitude, centerPoint.longitude, binding_form_new.mapeditorNotesNew.getText().toString());

        mMapViewModel.insert(newM);
        clusterManager.addItem(new MyItemOnMap(centerPoint.latitude, centerPoint.longitude, getTitleFromMarker(context,newM), new Gson().toJson(newM)));
        clusterManager.cluster();

        disableCreationMode();
    }

    private void onClickPhotoNew(View view) {
        /*
        ImageCapture.OutputFileOptions outputFileOptions =
                new ImageCapture.OutputFileOptions.Builder(new File(...)).build();
        imageCapture.takePicture(outputFileOptions, cameraExecutor,
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(ImageCapture.OutputFileResults outputFileResults) {
                        // insert your code here.
                    }
                    @Override
                    public void onError(ImageCaptureException error) {
                        // insert your code here.
                    }
                }
        );

         */
    }

    // -- Condivisi (salvataggio della select dei tipi di cestini)
    private void saveEditMarkerType(View view) {
        Set<POIMarker.MarkerType> tempTypes = new HashSet<>();

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(context.getResources().getString(R.string.mapeditor_markertype));

        // Popolo la checkbox list
        POIMarker.MarkerType[] elements = new POIMarker.MarkerType[POIMarker.MarkerType.values().length];
        String[] types = new String[POIMarker.MarkerType.values().length];
        boolean[] checkedItems = new boolean[POIMarker.MarkerType.values().length];

        int i = 0;
        for(POIMarker.MarkerType t : POIMarker.MarkerType.values()) {
            elements[i] = t;
            types[i] = POIMarker.getMarkerTypeName(context, t);
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
        updateMapStyleByPreference(context, mMap);
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
        if(mMap != null) outState.putParcelable("cp", mMap.getCameraPosition());
        Log.d("ISTANZA", "mapeditor -> onSaveIstanceState");
    }
}