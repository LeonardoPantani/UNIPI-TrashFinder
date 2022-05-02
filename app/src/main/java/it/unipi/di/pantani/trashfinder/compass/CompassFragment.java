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
import androidx.preference.PreferenceManager;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.Toast;

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
    SharedPreferences sp;
    private float azim = 0f;
    private float currentAzimuth = 0f;
    private Location current_location = new Location("A");
    private final Location target = new Location("B");
    private LocationManager locationManager;
    Snackbar tipCloseToTarget;

    private String measureUnit_low;
    private String measureUnit_high;
    private int measureUnitCode;

    private Context context;

    int location_refresh_time;
    int LOCATION_REFRESH_DISTANCE = 0;

    private Compass compass;
    private boolean showTip;

    private boolean activateCompass = false;

    private CompassViewModel mCompassViewModel;

    private FragmentCompassBinding binding;


    // TODO rendere il layout della bussola non constraint

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        sp = PreferenceManager.getDefaultSharedPreferences(context);
        binding = FragmentCompassBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        setupCompass();

        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

        // view model
        mCompassViewModel = new ViewModelProvider(this).get(CompassViewModel.class);

        Log.d("ISTANZA", "compass -> onCreateView");
        return root;
    }

    private void setupCompass() {
        compass = new Compass(context);
        Compass.CompassListener cl = getCompassListener();
        compass.setListener(cl);
    }

    public void onLocationChanged(Location location) {
        if(getActivity() == null) {
            return;
        }

        if(location.getAccuracy() != 0f) {
            current_location = location;
            binding.compassTextAccuracyGps.setText(getResources().getString(R.string.accuracy_high));
        } else {
            binding.compassTextDifferenceDistance.setText(getActivity().getResources().getString(R.string.waiting_for_gps));
            binding.compassTextAccuracyGps.setText(getResources().getString(R.string.accuracy_low));
        }
    }

    private Compass.CompassListener getCompassListener() {
        return new Compass.CompassListener() {
            public void onNewAzimuth(float azimuth) {
                ((Activity) context).runOnUiThread(() -> {
                    if (!activateCompass) {
                        return;
                    }

                    float dist = current_location.distanceTo(target);
                    if (measureUnitCode == 1) {
                        dist *= 3.281;
                    }

                    azim = (float) (azimuth - CompassFragment.this.bearing(current_location.getLatitude(), current_location.getLongitude(), target.getLatitude(), target.getLongitude()));

                    if (dist < 1000) {
                        binding.compassTextDifferenceDistance.setText(context.getResources().getString(R.string.distance_difference, dist, measureUnit_low));

                        if (showTip) {
                            if ((dist < 10 && measureUnitCode == 0) || (dist < 32 && measureUnitCode == 1)) {
                                if (tipCloseToTarget == null) {
                                    tipCloseToTarget = Snackbar.make(getActivity().findViewById(android.R.id.content), CompassFragment.this.getResources().getString(R.string.compass_tipswitchtomap_title), Snackbar.LENGTH_INDEFINITE);
                                    tipCloseToTarget.setAction(R.string.compass_tipswitchtomap_button, view -> Toast.makeText(context, R.string.app_name, Toast.LENGTH_SHORT).show());
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
                if(accuracy != SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM && accuracy != SensorManager.SENSOR_STATUS_ACCURACY_HIGH) {
                    binding.compassTextDifferenceDistance.setText(getResources().getString(R.string.waiting_for_sensors));
                    binding.compassTextAccuracyAccelerometer.setText(getResources().getString(R.string.accuracy_low));
                } else {
                    binding.compassTextAccuracyAccelerometer.setText(getResources().getString(R.string.accuracy_high));
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

        location_refresh_time = sp.getInt("setting_compass_update_interval", Utils.default_location_refresh_time);
        location_refresh_time *= 1000;
        showTip = sp.getBoolean("setting_compass_show_tip_switchtomap", true);
        if(sp.getString("setting_compass_measureunit", "meters").equals("meters")) {
            measureUnit_low = context.getResources().getString(R.string.setting_compass_measureunit_meters);
            measureUnit_high = context.getResources().getString(R.string.setting_compass_measureunit_kilometers);
            measureUnitCode = 0;
        } else {
            measureUnit_low = context.getResources().getString(R.string.setting_compass_measureunit_feet);
            measureUnit_high = context.getResources().getString(R.string.setting_compass_measureunit_miles);
            measureUnitCode = 1;
        }


        if(!checkPerms(context)) {
            binding.compassCardviewWarning.setVisibility(View.VISIBLE);
            binding.compassTextWarning.setText(getResources().getString(R.string.dialog_nolocationperm_desc));
            return;
        } else {
            binding.compassCardviewWarning.setVisibility(View.GONE);
        }

        Marker selectedMarker = getCompassSelectedMarker();
        if(selectedMarker != null) { // se c'è un elemento selezionato
            if (checkPerms(context)) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, location_refresh_time, LOCATION_REFRESH_DISTANCE, this::onLocationChanged);
            }

            POIMarker tmarker = new Gson().fromJson(selectedMarker.getSnippet(), POIMarker.class);
            if(tmarker != null) {
                mCompassViewModel.getMarkerId(tmarker.getId()).observe(getViewLifecycleOwner(), targetMarker -> {
                    target.setLatitude(targetMarker.getLatitude());
                    target.setLongitude(targetMarker.getLongitude());
                }) ;
            }
            compass.start();
            activateCompass = true;
            binding.compassTextDifferenceDistance.setText(getResources().getString(R.string.waiting_for_sensors));
        } else {
            compass.stop();
            activateCompass = false;
            binding.compassTextDifferenceDistance.setText(Utils.SAD_EMOJI);

            binding.compassCardviewWarning.setVisibility(View.VISIBLE);
            binding.compassTextWarning.setText(getResources().getString(R.string.compass_tipchoosetargetfirst));
        }
        Log.d("ISTANZA", "compass -> onResume");
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