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
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;
import androidx.lifecycle.ViewModelProvider;

import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
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
import com.google.maps.android.clustering.Cluster;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.algo.NonHierarchicalViewBasedAlgorithm;
import com.google.maps.android.collections.MarkerManager;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;

import it.unipi.di.pantani.trashfinder.R;
import it.unipi.di.pantani.trashfinder.Utils;
import it.unipi.di.pantani.trashfinder.data.marker.MyItemOnMap;
import it.unipi.di.pantani.trashfinder.data.marker.POIMarker;
import it.unipi.di.pantani.trashfinder.data.requests.POIRequest;
import it.unipi.di.pantani.trashfinder.databinding.EditorEmptyBinding;
import it.unipi.di.pantani.trashfinder.databinding.EditorFormBinding;
import it.unipi.di.pantani.trashfinder.databinding.EditorFormNewBinding;
import it.unipi.di.pantani.trashfinder.databinding.FragmentMapEditorBinding;

public class MapEditorFragment extends Fragment implements OnMapReadyCallback {
    private GoogleMap mMap;
    private Context mContext;
    private MapsViewModel mMapViewModel;
    private MapEditorViewModel mMapEditorViewModel;
    private ClusterManager<MyItemOnMap> mClusterManager;
    private CustomClusterRenderer mCustomRenderer;

    private FragmentMapEditorBinding mBinding;

    private EditorEmptyBinding mBindingEmpty;
    private EditorFormBinding mBindingForm;
    private EditorFormNewBinding mBindingFormNew;

    private Bundle mBundle;

    private POIMarker mPOIMarkerSelected;
    private HashSet<POIMarker.MarkerType> mMarkerTypes;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        mBinding = FragmentMapEditorBinding.inflate(inflater, container, false);
        mBindingEmpty = EditorEmptyBinding.inflate(inflater, container, false);
        mBindingForm = EditorFormBinding.inflate(inflater, container, false);
        mBindingFormNew = EditorFormNewBinding.inflate(inflater, container, false);

        // view radice
        View root = mBinding.getRoot();

        // inizializzazione fragment mappa
        SupportMapFragment supportMapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.google_map);
        if (supportMapFragment == null) return root;

        // aggiungo inizialmente il layout vuoto (se non è nullo lo aggiungo)
        mBinding.mapeditorSectionForm.addView(mBindingEmpty.getRoot());

        // mappa asincrona
        supportMapFragment.getMapAsync(this);

        // view model mappa
        mMapViewModel = new ViewModelProvider(this).get(MapsViewModel.class);

        // view model mapeditor
        mMapEditorViewModel = new ViewModelProvider(this).get(MapEditorViewModel.class);

        // preparo markerTypes
        mMarkerTypes = new HashSet<>();

        // -- LISTENERS SEZIONE VUOTA
        mBindingEmpty.mapeditorEmpty.setOnClickListener(this::onClickEmptyForm);

        // -- LISTENERS SEZIONE EDIT
        mBindingForm.mapeditorLeave.setOnClickListener(this::onClickLeave);
        mBindingForm.mapeditorTypes.setOnClickListener(this::saveEditMarkerType);
        mBindingForm.mapeditorDelete.setOnClickListener(this::onClickDelete);
        mBindingForm.mapeditorSave.setOnClickListener(this::onClickSave);

        // -- LISTENERS SEZIONE NEW
        mBindingFormNew.mapeditorLeaveNew.setOnClickListener(this::onClickLeaveNew);
        mBindingFormNew.mapeditorSaveNew.setOnClickListener(this::onClickCreateNew);
        mBindingFormNew.mapeditorTypesNew.setOnClickListener(this::saveEditMarkerType);
        mBindingFormNew.mapeditorPhotoNew.setOnClickListener(this::onClickPhotoNew);

        // salvo il bundle
        mBundle = savedInstanceState;

        // return della view
        return root;
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        Log.d("ISTANZA", "mapeditor -> onMapReady");
        mMap = googleMap;

        // attivo la modalità location enabled
        if (checkPerms(mContext))
            mMap.setMyLocationEnabled(true);

        // aggiorno lo stile di mappa
        updateMapStyleByPreference(mContext, mMap);

        initializeClusterSystem();
    }

    public void initializeClusterSystem() {
        Log.d("ISTANZA", "mapeditor -> inizializzazione sistema cluster");
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
        // uso un custom renderer per la modalità modifica
        // questo renderer non mostra i cluster se lo zoom supera una certa soglia (per una migliore esperienza di modifica)
        mCustomRenderer = new CustomClusterRenderer(mContext, mMap, mClusterManager,mMap.getCameraPosition().zoom, EDITMODE_NO_CLUSTER_MIN_ZOOM);
        mClusterManager.setRenderer(mCustomRenderer);
        mClusterManager.setOnClusterClickListener(this::onClusterClick);
        mMap.setOnCameraMoveListener(mCustomRenderer);
        mMap.setOnCameraIdleListener(mClusterManager);

        MarkerManager.Collection markerCollection = mClusterManager.getMarkerCollection();
        markerCollection.setInfoWindowAdapter(new POIMarkerWindowAdapter(mContext, 1));
        markerCollection.setOnMarkerClickListener(this::onMarkerClick);

        mBinding.progressbar.setVisibility(View.VISIBLE);
        refreshMap();
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
                mClusterManager.addItem(new MyItemOnMap(m.getLatitude(), m.getLongitude(), getTitleFromMarker(mContext, m), gson.toJson(m)));
            }
            mClusterManager.cluster();

            if(markers.size() != 0) {
                mBinding.progressbar.setVisibility(View.INVISIBLE);
                Log.d("ISTANZA", "maps -> aggiornamento ai marcatori su mappa applicato");
            }
        });
    }

    public void onZoomChange(float newZoom) {
        if(newZoom < MARKER_ZOOM) {
            mBindingFormNew.mapeditorSaveNew.setEnabled(false);
            mBindingFormNew.mapeditorWarningNew.setVisibility(View.VISIBLE);
        } else {
            mBindingFormNew.mapeditorSaveNew.setEnabled(true);
            mBindingFormNew.mapeditorWarningNew.setVisibility(View.GONE);
        }
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
        mPOIMarkerSelected = m;
        setEditorSelectedMarker(marker);

        mBinding.mapeditorSectionForm.removeAllViews();
        mBinding.mapeditorSectionForm.addView(mBindingForm.getRoot());

        mMarkerTypes.addAll(m.getTypes());
        mBindingForm.mapeditorNotes.setText(m.getNotes(), TextView.BufferType.EDITABLE);
    }

    private void disableEditingMode() {
        mMap.getUiSettings().setScrollGesturesEnabled(true);
        mMap.getUiSettings().setZoomGesturesEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(true);

        mPOIMarkerSelected = null;

        // chiude la tastiera
        if(getActivity() != null) Utils.closeKeyboard(getActivity());

        setEditorSelectedMarker(null);

        mBinding.mapeditorSectionForm.removeAllViews();
        mBinding.mapeditorSectionForm.addView(mBindingEmpty.getRoot());

        mMarkerTypes.clear();
        mBindingForm.mapeditorNotes.setText("");
    }

    private void onClickLeave(View view) {
        disableEditingMode();
    }

    private void onClickDelete(View view) {
        AlertDialog alertDialog = new AlertDialog.Builder(mContext).create();
        alertDialog.setTitle(getResources().getString(R.string.mapeditor_suredelete));
        alertDialog.setMessage(getResources().getString(R.string.mapeditor_suredelete_desc));
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, getResources().getString(R.string.button_cancel),
                (dialog, which) -> dialog.dismiss());
        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, getResources().getString(R.string.button_ok),
                (dialog, which) -> {
                    // inserisco una nuova richiesta
                    mMapEditorViewModel.insert(new POIRequest(mPOIMarkerSelected, System.currentTimeMillis(), null, Utils.getCurrentUserAccount().getEmail(), true));

                    // SOLO PERCHE' E' UNA DEMO
                    mMapViewModel.delete(mPOIMarkerSelected);

                    disableEditingMode();
                });
        alertDialog.show();
    }

    private void onClickSave(View view) {
        if(!validateEdit()) return;

        mMapViewModel.update(mPOIMarkerSelected.getId(), mMarkerTypes, mPOIMarkerSelected.getLatitude(), mPOIMarkerSelected.getLongitude(), mBindingForm.mapeditorNotes.getText().toString());

        // inserisco una nuova richiesta (nota: non c'è un'immagine da inserire nel caso di modifiche!)
        mMapEditorViewModel.insert(new POIRequest(new POIMarker(mMarkerTypes, mPOIMarkerSelected.getLatitude(), mPOIMarkerSelected.getLongitude(), mBindingForm.mapeditorNotes.getText().toString()), System.currentTimeMillis(), null, Utils.getCurrentUserAccount().getEmail(), false));

        disableEditingMode();
    }

    private boolean validateEdit() {
        /*
        Controlli:
        markerTypes non deve essere vuoto
        se non vuoto ed è selezionata "isola ecologica", si deve indicare almeno un tipo
         */
        if(mMarkerTypes.size() == 0) {
            createDialogForm(getResources().getString(R.string.mapeditor_dialog_warning_title), getResources().getString(R.string.mapeditor_dialog_warning_desc, getResources().getString(R.string.mapeditor_form_errorempty)));
            return false;
        }
        if(mMarkerTypes.size() == 1 && mMarkerTypes.contains(POIMarker.MarkerType.recyclingdepot)) {
            createDialogForm(getResources().getString(R.string.mapeditor_dialog_warning_title), getResources().getString(R.string.mapeditor_dialog_warning_desc, getResources().getString(R.string.mapeditor_form_errorrdemptytypes)));
            return false;
        }

        return true;
    }

    // -- MODALITA' CREAZIONE
    private void enableCreationMode() {
        mCustomRenderer.setZoomChangeListener(this::onZoomChange);

        mBinding.mapeditorMarkericon.setVisibility(View.VISIBLE);

        mBinding.mapeditorSectionForm.removeAllViews();
        mBinding.mapeditorSectionForm.addView(mBindingFormNew.getRoot());
    }

    private void disableCreationMode() {
        mCustomRenderer.unsetZoomChangeListener();

        if(getActivity() != null) Utils.closeKeyboard(getActivity()); // chiude la tastiera

        mBindingFormNew.mapeditorPhotoNew.setTag(null);
        mBindingFormNew.mapeditorPhotoNew.setText(R.string.mapeditor_photo_new);

        mBinding.mapeditorMarkericon.setVisibility(View.GONE);

        mBinding.mapeditorSectionForm.removeAllViews();
        mBinding.mapeditorSectionForm.addView(mBindingEmpty.getRoot());
    }

    private void onClickEmptyForm(View view) {
        enableCreationMode();
    }

    private void onClickLeaveNew(View view) {
        disableCreationMode();
    }

    private void onClickCreateNew(View view) {
        if(!validateCreate()) return;

        LatLng centerPoint = mMap.getCameraPosition().target;
        POIMarker newM = new POIMarker(mMarkerTypes, centerPoint.latitude, centerPoint.longitude, mBindingFormNew.mapeditorNotesNew.getText().toString());

        mMapViewModel.insert(newM);
        mClusterManager.addItem(new MyItemOnMap(centerPoint.latitude, centerPoint.longitude, getTitleFromMarker(mContext, newM), new Gson().toJson(newM)));
        mClusterManager.cluster();

        if(uri != null) {
            mMapEditorViewModel.insert(new POIRequest(newM, System.currentTimeMillis(), uri.toString(), Utils.getCurrentUserAccount().getEmail(), false));
        } else {
            Toast.makeText(mContext, R.string.generic_error, Toast.LENGTH_SHORT).show();
        }

        disableCreationMode();
    }

    Uri uri;
    private void onClickPhotoNew(View view) {
        if(view.getTag() != null) { // se è già stata salvata una foto
            AlertDialog alertDialog = new AlertDialog.Builder(mContext).create();
            alertDialog.setTitle(getResources().getString(R.string.mapeditor_photo_alreadytaken_title));
            alertDialog.setMessage(getResources().getString(R.string.mapeditor_photo_alreadytaken_desc));
            alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, getResources().getString(R.string.button_cancel),
                    (dialog, which) -> dialog.dismiss());
            alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, getResources().getString(R.string.button_ok),
                    (dialog, which) -> takePhoto());
            alertDialog.show();
        } else {
            takePhoto();
        }
    }

    private void takePhoto() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(mContext.getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                uri = FileProvider.getUriForFile(mContext,
                        "it.unipi.di.pantani.trashfinder.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
                someActivityResultLauncher.launch(takePictureIntent);
            }
        }
    }

    ActivityResultLauncher<Intent> someActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    mBindingFormNew.mapeditorPhotoNew.setText(R.string.mapeditor_photo_new_taken);
                    mBindingFormNew.mapeditorPhotoNew.setTag(true);
                }
            });

    private File createImageFile() throws IOException {
        String imageFileName = "trashbinimage_" + System.currentTimeMillis();
        File storageDir = mContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return new File(storageDir, imageFileName + ".jpg");
    }

    // -- Condivisi (salvataggio della select dei tipi di cestini)
    private void saveEditMarkerType(View view) {
        HashSet<POIMarker.MarkerType> tempTypes = new HashSet<>();

        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle(mContext.getResources().getString(R.string.mapeditor_markertype));

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

        builder.setPositiveButton(R.string.button_ok, (dialog, which) -> mMarkerTypes = tempTypes);
        builder.setNegativeButton(R.string.button_cancel, null);
        builder.setNeutralButton(R.string.maps_bulkeditfilterbutton, null);

        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(dialogInterface -> {
            Button button = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);
            button.setOnClickListener(notused -> {
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

    private boolean validateCreate() {
        ArrayList<String> errors = new ArrayList<>();
        /*
        Controlli:
        markerTypes non deve essere vuoto
        se non vuoto ed è selezionata "isola ecologica", si deve indicare almeno un tipo
        deve essere stata scattata una foto
         */
        if(mMarkerTypes.size() == 0) {
            errors.add(getResources().getString(R.string.mapeditor_form_errorempty));
        }
        if(mMarkerTypes.size() == 1 && mMarkerTypes.contains(POIMarker.MarkerType.recyclingdepot)) {
            errors.add(getResources().getString(R.string.mapeditor_form_errorrdemptytypes));
        }
        if(!Utils.isImageFile(uri)) {
            errors.add(getResources().getString(R.string.mapeditor_form_errorphotonottaken));
        }

        if(errors.size() == 0) {
            return true;
        } else {
            createDialogForm(getResources().getString(R.string.mapeditor_dialog_warning_title), getResources().getString(R.string.mapeditor_dialog_warning_desc, String.join("\n", errors)));
            return false;
        }
    }

    // Utilità
    private void createDialogForm(String title, String desc) {
        AlertDialog alertDialog = new AlertDialog.Builder(mContext).create();
        alertDialog.setTitle(title);
        alertDialog.setMessage(desc);
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, getResources().getString(R.string.button_ok),
                (dialog, which) -> dialog.dismiss());

        alertDialog.show();
    }

    // -------------
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.mContext = context;
        Log.d("ISTANZA", "mapeditor -> onAttach");
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mContext = null;
        Log.d("ISTANZA", "mapeditor -> onDetach");
    }

    @Override
    public void onResume() {
        super.onResume();
        updateMapStyleByPreference(mContext, mMap);
        Log.d("ISTANZA", "mapeditor -> onResume");
    }

    @Override
    public void onStart() {
        super.onStart();
        if(!Utils.canTakePhoto(mContext)) {
            mBindingEmpty.mapeditorEmptyYouareineditingmodeImage.setImageResource(R.drawable.ic_baseline_edit_location_alt_24);
            mBindingEmpty.mapeditorEmptyYouareineditingmodeText.setText(getResources().getString(R.string.mapeditor_youareineditingmode2));
            mBindingEmpty.mapeditorEmpty.setOnClickListener(null);
        }
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