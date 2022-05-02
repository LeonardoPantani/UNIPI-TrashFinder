package it.unipi.di.pantani.trashfinder.mapeditor;

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

import it.unipi.di.pantani.trashfinder.R;
import it.unipi.di.pantani.trashfinder.databinding.FragmentMapEditorBinding;

public class MapEditorFragment extends Fragment implements OnMapReadyCallback {
    private GoogleMap mMap;
    private Context context;
    private SharedPreferences sp;

    private FragmentMapEditorBinding binding;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentMapEditorBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        sp = PreferenceManager.getDefaultSharedPreferences(context);

        // inizializzazione fragment mappa
        SupportMapFragment supportMapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.google_map);

        if(supportMapFragment == null) return root; // in caso di problemi

        // mappa asincrona
        supportMapFragment.getMapAsync(this);

        return root;
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
    }

    @Override
    public void onResume() {
        super.onResume();
        updateMapStyleByPreference(context, sp, mMap);
    }
}