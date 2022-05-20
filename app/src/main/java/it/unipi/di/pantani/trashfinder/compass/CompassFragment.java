package it.unipi.di.pantani.trashfinder.compass;

import static it.unipi.di.pantani.trashfinder.Utils.checkPerms;
import static it.unipi.di.pantani.trashfinder.Utils.getCompassSelectedMarker;
import static it.unipi.di.pantani.trashfinder.Utils.setCompassSelectedMarker;
import static it.unipi.di.pantani.trashfinder.data.marker.POIMarker.getMarkerTypeName;
import static it.unipi.di.pantani.trashfinder.data.marker.POIMarker.getTitleFromMarker;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Bundle;

import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.preference.PreferenceManager;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.gms.maps.model.Marker;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;

import java.util.Set;
import java.util.stream.Collectors;

import it.unipi.di.pantani.trashfinder.R;
import it.unipi.di.pantani.trashfinder.data.marker.POIMarker;
import it.unipi.di.pantani.trashfinder.databinding.FragmentCompassBinding;

public class CompassFragment extends Fragment {
    private SharedPreferences sp;
    private Context context;
    private FragmentCompassBinding binding;

    private float azim = 0f;
    private float currentAzimuth = 0f;
    private Location currentLocation = new Location("A");
    private final Location target = new Location("B");

    private boolean showTip;
    private Snackbar tipCloseToTarget;

    private String measureUnit_low;
    private String measureUnit_high;
    private int measureUnitCode;

    private Compass compass;
    private UserLocation userLoc;

    private boolean readyLocation;
    private boolean readySensor;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        sp = PreferenceManager.getDefaultSharedPreferences(context);
        binding = FragmentCompassBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        // preparo la bussola
        setupCompass();
        setupLocation();
        // onClick su cardview
        binding.compassCardviewMain.setOnClickListener(this::onClickCardView);
        // onClick su button
        binding.compassCardviewDeselectButton.setOnClickListener(this::onClickDeselectButton);
        return root;
    }

    private void setupCompass() {
        compass = new Compass(context);
        Compass.CompassListener cl = getCompassListener();
        compass.setListener(cl);
        readyLocation = false;
        readySensor = false;
    }

    private void setupLocation() {
        userLoc = new UserLocation(context);
        UserLocation.UserLocationListener ull = getUserLocationListener();
        userLoc.setListener(ull);
        Location temp = userLoc.getLastLocation();
        if(temp != null) {
            currentLocation = temp;
            readyLocation = true;
        }
    }

    private UserLocation.UserLocationListener getUserLocationListener() {
        return new UserLocation.UserLocationListener() {
            @Override
            public void onNewLocation(Location newLoc) {
                if(newLoc.getAccuracy() != 0f) {
                    currentLocation = newLoc;
                }
            }

            @Override
            public void onAccuracyChanged(float accuracy) {
                if(accuracy != 0f) {
                    readyLocation = true;
                    binding.compassTextAccuracyGps.setText(getResources().getString(R.string.accuracy_high));
                } else {
                    readyLocation = false;
                    binding.compassTextAccuracyGps.setText(getResources().getString(R.string.accuracy_low));
                }
            }
        };
    }

    private Compass.CompassListener getCompassListener() {
        return new Compass.CompassListener() {
            public void onNewAzimuth(float azimuth) {
                ((Activity) context).runOnUiThread(() -> {
                    if(!isCompassReady()) return;

                    float dist = currentLocation.distanceTo(target);
                    if (measureUnitCode == 1) {
                        dist *= 3.281;
                    }

                    azim = (float) (azimuth - Compass.bearing(currentLocation.getLatitude(), currentLocation.getLongitude(), target.getLatitude(), target.getLongitude()));

                    if (dist < 1000) {
                        binding.compassTextDifferenceDistance.setText(context.getResources().getString(R.string.distance_difference, dist, measureUnit_low));

                        if (showTip) {
                            if ((dist < 50 && measureUnitCode == 0) || (dist < 164 && measureUnitCode == 1)) {
                                if (tipCloseToTarget == null) {
                                    tipCloseToTarget = Snackbar.make(((Activity) context).findViewById(android.R.id.content), CompassFragment.this.getResources().getString(R.string.compass_tipswitchtomap_title), Snackbar.LENGTH_INDEFINITE);
                                    tipCloseToTarget.setAction(R.string.compass_tipswitchtomap_button, view -> {
                                        // ottengo il navController
                                        NavHostFragment navHostFragment = (NavHostFragment) requireActivity().getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment_content_main);
                                        if(navHostFragment == null) { // non dovrebbe mai verificarsi!
                                            Log.d("ISTANZA", "navHostFragment null (fragment)!");
                                            return;
                                        }
                                        NavController navController = navHostFragment.getNavController();
                                        navController.popBackStack();
                                    });
                                    tipCloseToTarget.show();
                                }
                            } else {
                                if (tipCloseToTarget != null) {
                                    tipCloseToTarget.dismiss();
                                }
                            }
                        }
                    } else {
                        if (measureUnitCode == 0) {
                            dist /= 1000;
                        } else {
                            dist /= 5280;
                        }
                        binding.compassTextDifferenceDistance.setText(context.getResources().getString(R.string.distance_difference, dist, measureUnit_high));
                    }

                    Animation an = new RotateAnimation(-currentAzimuth, -azim,
                            Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF,
                            0.5f);

                    currentAzimuth = azim;

                    an.setDuration(500);
                    an.setRepeatCount(0);
                    an.setFillAfter(true);

                    binding.compassIcon.startAnimation(an);
                });
            }

            @Override
            public void onSensorAccuracyChanged(float accuracy) {
                if(accuracy == SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM || accuracy == SensorManager.SENSOR_STATUS_ACCURACY_HIGH) {
                    readySensor = true;
                    binding.compassTextAccuracyAccelerometer.setText(getResources().getString(R.string.accuracy_high));
                } else {
                    readySensor = false;
                    binding.compassTextAccuracyAccelerometer.setText(getResources().getString(R.string.accuracy_low));
                }
            }
        };
    }

    private void onClickCardView(View view) {
        Navigation.findNavController(view).popBackStack(R.id.nav_maps, false); // primo
    }

    private void onClickDeselectButton(View view) {
        setCompassSelectedMarker(null);
        handleCompass();
    }

    /**
     * Metodo principale che gestisce tutta la bussola. Viene chiamato dalla onResume. Si occupa di
     * avviare i sensori e gps per poi chiamare i metodi di aggiornamento della UI. Se non ci sono
     * cestini selezionati blocca tutti i listener e nasconde la bussola.
     */
    private void handleCompass() {
        Marker selectedMarker = getCompassSelectedMarker();
        if(selectedMarker != null) { // se ho un marker selezionato
            POIMarker targetMarker = new Gson().fromJson(selectedMarker.getSnippet(), POIMarker.class);
            if(targetMarker == null) throw new IllegalStateException();
            compass.start();
            userLoc.start(sp.getInt("setting_compass_update_interval", 1)*1000);
            target.setLatitude(targetMarker.getLatitude());
            target.setLongitude(targetMarker.getLongitude());
            showCompassInfo(targetMarker);
        } else { // se non ho un marker selezionato
            compass.stop();
            userLoc.stop();
            hideCompassInfo();
        }
    }

    /**
     * Lavora sulla UI. Mostra la freccia e altre informazioni su schermo.
     * @param selectedMarker il marker selezionato
     */
    private void showCompassInfo(POIMarker selectedMarker) {
        // se non contiene recylingdepot mostro "cestino", altrimenti mostro "isola ecologica"
        if(!selectedMarker.getTypes().contains(POIMarker.MarkerType.recyclingdepot)) {
            binding.compassCardviewHeaderimage.setImageResource(R.drawable.compass_cardheader_binselected);
        } else {
            binding.compassCardviewHeaderimage.setImageResource(R.drawable.compass_cardheader_rdselected);
        }

        binding.compassCardviewTitle.setText(getTitleFromMarker(context, selectedMarker));
        binding.compassCardviewDeselectButton.setVisibility(View.VISIBLE);

        Set<POIMarker.MarkerType> types = selectedMarker.getTypes();
        types.remove(POIMarker.MarkerType.recyclingdepot);

        String s = types.stream()
                .map(t -> getMarkerTypeName(context, t))
                .collect(Collectors.joining(", "));
        if(!s.equals(""))
            binding.compassCardviewDesc.setText(s);
        else
            binding.compassCardviewDesc.setText(getResources().getString(R.string.compass_cardview_notype));

        // mostro la sezione della precisione e imposto i testi su LOW, tanto saranno aggiornati da altri metodi
        binding.compassIcon.setVisibility(View.VISIBLE);
        binding.compassTextDifferenceDistance.setVisibility(View.VISIBLE);

        binding.compassTextAccuracyGps.setText(getResources().getString(R.string.accuracy_low));
        binding.compassTextAccuracyAccelerometer.setText(getResources().getString(R.string.accuracy_low));
        binding.compassSectionAccuracy.setVisibility(View.VISIBLE);
    }

    /**
     * Lavora sulla UI. Nasconde la freccia e altre informazioni. Mostra solo una cardview con
     * dove si informa l'utente di scegliere un marker da seguire.
     */
    private void hideCompassInfo() {
        if(tipCloseToTarget != null) tipCloseToTarget.dismiss();

        binding.compassTextDifferenceDistance.setText("");

        binding.compassIcon.clearAnimation();
        binding.compassIcon.setVisibility(View.GONE);
        binding.compassTextDifferenceDistance.setVisibility(View.GONE);

        binding.compassCardviewHeaderimage.setImageResource(R.drawable.compass_cardheader_noselected);
        binding.compassCardviewTitle.setText(getResources().getString(R.string.compass_selecteditem_noitem_title));
        binding.compassCardviewDesc.setText(getResources().getString(R.string.compass_selecteditem_noitem_desc));
        binding.compassCardviewDeselectButton.setVisibility(View.GONE);

        binding.compassSectionAccuracy.setVisibility(View.GONE);
    }

    /**
     * Modifica la UI in modo da mostrare un avviso di mancanza di permessi
     */
    private void handleNoLocation() {
        binding.compassTextDifferenceDistance.setText("");

        binding.compassIcon.clearAnimation();
        binding.compassIcon.setVisibility(View.GONE);
        binding.compassTextDifferenceDistance.setVisibility(View.GONE);

        binding.compassCardviewHeaderimage.setImageResource(R.drawable.compass_cardheader_noselected);
        binding.compassCardviewTitle.setText(getResources().getString(R.string.compass_nopermission_title));
        binding.compassCardviewDesc.setText(getResources().getString(R.string.compass_nopermission_desc, getResources().getString(R.string.app_name)));
        binding.compassCardviewDeselectButton.setVisibility(View.GONE);

        binding.compassSectionAccuracy.setVisibility(View.GONE);
    }

    /**
     * Cambia un testo sotto la freccia per indicare se si sta aspettando che sia pronto il gps o i
     * sensori; e restituisce lo stato della bussola.
     * @return vero se la bussola è pronta, falso altrimenti
     */
    private boolean isCompassReady() {
        if(!readyLocation) {
            binding.compassTextDifferenceDistance.setText(getResources().getString(R.string.waiting_for_gps));
            return false;
        }
        if(!readySensor) {
            binding.compassTextDifferenceDistance.setText(getResources().getString(R.string.waiting_for_sensors));
            return false;
        }
        return true;
    }

    @Override
    public void onPause() {
        super.onPause();

        compass.stop();
        userLoc.stop();

        if (tipCloseToTarget != null) {
            tipCloseToTarget.dismiss();
            tipCloseToTarget = null;
        }
        Log.d("ISTANZA", "compass -> onPause");
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d("ISTANZA", "compass -> onResume");

        // aggiorno le variabili con le preferenze
        showTip = sp.getBoolean("setting_compass_show_tip_switchtomap", true);
        if(sp.getString("setting_compass_measureunit", "meters").equals("meters")) {
            measureUnitCode = 0;
            measureUnit_low = context.getResources().getString(R.string.setting_compass_measureunit_meters);
            measureUnit_high = context.getResources().getString(R.string.setting_compass_measureunit_kilometers);
        } else {
            measureUnitCode = 1;
            measureUnit_low = context.getResources().getString(R.string.setting_compass_measureunit_feet);
            measureUnit_high = context.getResources().getString(R.string.setting_compass_measureunit_miles);
        }

        // mostro un avviso in caso di mancanza di permessi
        if(!checkPerms(context)) {
            handleNoLocation();
        } else { // altrimenti gestisco il compass
            handleCompass();
        }
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
        Log.d("ISTANZA", "compass -> onAttach");
    }

    @Override
    public void onDetach() {
        super.onDetach();
        Log.d("ISTANZA", "compass -> onDetach");
    }
}