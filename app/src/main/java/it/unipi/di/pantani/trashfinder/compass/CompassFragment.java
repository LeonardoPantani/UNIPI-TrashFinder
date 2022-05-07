package it.unipi.di.pantani.trashfinder.compass;

import static it.unipi.di.pantani.trashfinder.Utils.checkPerms;
import static it.unipi.di.pantani.trashfinder.Utils.getCompassSelectedMarker;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;

import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
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

import it.unipi.di.pantani.trashfinder.R;
import it.unipi.di.pantani.trashfinder.Utils;
import it.unipi.di.pantani.trashfinder.data.POIMarker;
import it.unipi.di.pantani.trashfinder.databinding.FragmentCompassBinding;

public class CompassFragment extends Fragment {
    private SharedPreferences sp;
    private Context context;
    private CompassViewModel mCompassViewModel;
    private FragmentCompassBinding binding;

    private float azim = 0f;
    private float currentAzimuth = 0f;
    private Location currentLocation = new Location("A");
    private final Location target = new Location("B");
    private LocationManager locationManager;

    private boolean showTip;
    private Snackbar tipCloseToTarget;

    private String measureUnit_low;
    private String measureUnit_high;
    private int measureUnitCode;

    int location_refresh_time;

    private Compass compass;

    private boolean readyLocation;
    private boolean readySensor;

    Marker selectedMarker;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        sp = PreferenceManager.getDefaultSharedPreferences(context);
        binding = FragmentCompassBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        // preparo la bussola
        setupCompass();
        // preparo il manager della posizione
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        // view model
        mCompassViewModel = new ViewModelProvider(this).get(CompassViewModel.class);
        return root;
    }

    private void setupCompass() {
        compass = new Compass(context);
        Compass.CompassListener cl = getCompassListener();
        compass.setListener(cl);

        readyLocation = false;
        readySensor = false;
    }

    public void onLocationChanged(Location location) {
        if(getActivity() == null) {
            return;
        }

        if(location.getAccuracy() != 0f) {
            readyLocation = true;
            currentLocation = location;
            binding.compassTextAccuracyGps.setText(getResources().getString(R.string.accuracy_high));
        } else {
            readyLocation = false;
            binding.compassTextAccuracyGps.setText(getResources().getString(R.string.accuracy_low));
        }
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

                    azim = (float) (azimuth - CompassFragment.this.bearing(currentLocation.getLatitude(), currentLocation.getLongitude(), target.getLatitude(), target.getLongitude()));

                    if (dist < 1000) {
                        binding.compassTextDifferenceDistance.setText(context.getResources().getString(R.string.distance_difference, dist, measureUnit_low));

                        if (showTip) {
                            if ((dist < 50 && measureUnitCode == 0) || (dist < 164 && measureUnitCode == 1)) {
                                if (tipCloseToTarget == null && getActivity() != null) {
                                    tipCloseToTarget = Snackbar.make(getActivity().findViewById(android.R.id.content), CompassFragment.this.getResources().getString(R.string.compass_tipswitchtomap_title), Snackbar.LENGTH_INDEFINITE);
                                    tipCloseToTarget.setAction(R.string.compass_tipswitchtomap_button, view -> {
                                        // ottengo il navController
                                        NavHostFragment navHostFragment = (NavHostFragment) getActivity().getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment_content_main);
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

    /**
     * Calcolo traiettoria per l'orientamento della freccia
     * @param startLat latitudine iniziale
     * @param startLng longitudine iniziale
     * @param endLat latitudine finale
     * @param endLng longitudine finale
     * @return angolo
     */
    public double bearing(double startLat, double startLng, double endLat, double endLng){
        double latitude1 = Math.toRadians(startLat);
        double latitude2 = Math.toRadians(endLat);
        double longDiff = Math.toRadians(endLng - startLng);
        double y = Math.sin(longDiff)*Math.cos(latitude2);
        double x = Math.cos(latitude1)*Math.sin(latitude2)-Math.sin(latitude1)*Math.cos(latitude2)*Math.cos(longDiff);
        return (Math.toDegrees(Math.atan2(y, x))+360)%360;
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onResume() {
        super.onResume();
        // aggiorno le variabili con le preferenze
        location_refresh_time = sp.getInt("setting_compass_update_interval", Utils.default_location_refresh_time)*1000;
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
            binding.compassCardviewWarning.setVisibility(View.VISIBLE);
            binding.compassTextWarning.setText(getResources().getString(R.string.dialog_nolocationperm_desc));
        } else {
            binding.compassCardviewWarning.setVisibility(View.GONE);
        }

        selectedMarker = getCompassSelectedMarker();
        if(selectedMarker != null) { // se c'è un elemento selezionato
            // mostro la sezione della precisione e imposto i testi su LOW, tanto saranno aggiornati da altri metodi
            binding.compassSectionAccuracy.setVisibility(View.VISIBLE);
            binding.compassTextAccuracyGps.setText(getResources().getString(R.string.accuracy_low));
            binding.compassTextAccuracyAccelerometer.setText(getResources().getString(R.string.accuracy_low));

            if (checkPerms(context)) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, location_refresh_time, 0, this::onLocationChanged);
            }

            POIMarker targetMarker = new Gson().fromJson(selectedMarker.getSnippet(), POIMarker.class);
            if(targetMarker != null) {
                target.setLatitude(targetMarker.getLatitude());
                target.setLongitude(targetMarker.getLongitude());
            }
            compass.start();
        } else {
            compass.stop();
            binding.compassTextDifferenceDistance.setText(Utils.SAD_EMOJI);

            binding.compassCardviewWarning.setVisibility(View.VISIBLE);
            binding.compassTextWarning.setText(getResources().getString(R.string.compass_tipchoosetargetfirst));
        }
        Log.d("ISTANZA", "compass -> onResume");
    }

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
        locationManager.removeUpdates(this::onLocationChanged);
        compass.stop();
        if (tipCloseToTarget != null) {
            tipCloseToTarget.dismiss();
            tipCloseToTarget = null;
        }
        Log.d("ISTANZA", "compass -> onPause");
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