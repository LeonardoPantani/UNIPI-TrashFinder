package it.unipi.di.pantani.trashfinder.home;

import static it.unipi.di.pantani.trashfinder.Utils.MARKER_ZOOM;
import static it.unipi.di.pantani.trashfinder.Utils.checkPerms;
import static it.unipi.di.pantani.trashfinder.Utils.getTitleFromMarker;
import static it.unipi.di.pantani.trashfinder.Utils.getSnippetFromMarker;
import static it.unipi.di.pantani.trashfinder.Utils.pointLocation;
import static it.unipi.di.pantani.trashfinder.Utils.setPreference;
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
import com.google.android.gms.maps.model.MarkerOptions;

import it.unipi.di.pantani.trashfinder.MarkerWindowAdapter;
import it.unipi.di.pantani.trashfinder.data.Marker;
import trashfinder.R;


public class MapsFragment extends Fragment implements OnMapReadyCallback {
    private GoogleMap mMap;
    private Context context;
    private SharedPreferences sp;

    private MapsViewModel mMapViewModel;

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

        // view model
        mMapViewModel = new ViewModelProvider(this).get(MapsViewModel.class);

        // return della view
        return view;
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        mMap.setInfoWindowAdapter(new MarkerWindowAdapter(context));

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

        mMapViewModel.getNearMarkers().observe(getViewLifecycleOwner(), markers -> {
            //LatLngBounds.Builder builder = new LatLngBounds.Builder();

            for(Marker m : markers) {
                Log.d("ISTANZA", "marcatore " + m.getLatitude() + " | " + m.getLongitude());
                mMap.addMarker(new MarkerOptions()
                .position(new LatLng(m.getLatitude(), m.getLongitude()))
                .title(getTitleFromMarker(context, m))
                .snippet(getSnippetFromMarker(m))
                .icon(BitmapDescriptorFactory.defaultMarker(getMarkerColorByType(m))));

                //builder.include(new LatLng(m.getLatitude(), m.getLongitude()));
            }
            //LatLngBounds bounds = builder.build();
            //mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 0));
        });

        mMap.setOnInfoWindowClickListener(marker -> {
            String marker_title = marker.getTitle();
            if(marker_title != null) {
                setPreference(context, "compass_markerid", marker_title.substring(1, marker.getTitle().indexOf(' ')));
                Toast.makeText(context, getResources().getString(R.string.infowindow_setcompass), Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(context, getResources().getString(R.string.generic_error), Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        updateMapStyleByPreference(context, sp, mMap);
    }

    private float getMarkerColorByType(Marker m) {
        float ret;

        switch(m.getType()) {
            case trashbin_indifferenziato: {
                ret = BitmapDescriptorFactory.HUE_RED;
                break;
            }
            case trashbin_plastica: {
                ret = BitmapDescriptorFactory.HUE_AZURE;
                break;
            }
            case trashbin_alluminio: {
                ret = BitmapDescriptorFactory.HUE_BLUE;
                break;
            }
            case trashbin_carta: {
                ret = BitmapDescriptorFactory.HUE_GREEN;
                break;
            }
            case trashbin_organico: {
                ret = BitmapDescriptorFactory.HUE_ORANGE;
                break;
            }
            case trashbin_farmaci: {
                ret = BitmapDescriptorFactory.HUE_VIOLET;
                break;
            }
            case trashbin_pile: {
                ret = BitmapDescriptorFactory.HUE_YELLOW;
                break;
            }
            case trashbin_olio: {
                ret = BitmapDescriptorFactory.HUE_ROSE;
                break;
            }
            case recyclingdepot: {
                ret = BitmapDescriptorFactory.HUE_MAGENTA;
                break;
            }
            default: {
                ret = BitmapDescriptorFactory.HUE_RED;
            }
        }
        return ret;
    }
}