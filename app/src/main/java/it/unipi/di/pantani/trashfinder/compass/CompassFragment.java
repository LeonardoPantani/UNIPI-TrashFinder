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
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.maps.model.Marker;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;

import it.unipi.di.pantani.trashfinder.Utils;
import it.unipi.di.pantani.trashfinder.data.POIMarker;
import trashfinder.R;
import trashfinder.databinding.FragmentCommunityBinding;
import trashfinder.databinding.FragmentCompassBinding;

public class CompassFragment extends Fragment {
    SharedPreferences sp;
    private float azim = 0f;
    private float currentAzimuth = 0f;
    private Location current_location = new Location("A");
    private final Location target = new Location("B");
    private LocationManager locationManager;

    private String measureUnit_low;
    private String measureUnit_high;
    private int measureUnitCode;

    private Context context;

    int location_refresh_time;
    int LOCATION_REFRESH_DISTANCE = 0;

    private Snackbar warning_notif = null;

    private Compass compass;
    private boolean showTip;

    private boolean activateCompass;

    private CompassViewModel mCompassViewModel;
    
    private FragmentCompassBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        sp = PreferenceManager.getDefaultSharedPreferences(context);
        binding = FragmentCompassBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        compass = new Compass(context);
        setupCompass();

        // view model
        mCompassViewModel = new ViewModelProvider(this).get(CompassViewModel.class);

        return root;
    }

    private void setupCompass() {
        compass = new Compass(context);
        Compass.CompassListener cl = getCompassListener();
        compass.setListener(cl);
    }

    public void onLocationChanged(Location location) {
        if(location.getAccuracy() != 0f) {
            current_location = location;
            binding.compassTextAccuracyGps.setText(getActivity().getResources().getString(R.string.accuracy_high));
        } else {
            binding.compassTextDifferenceDistance.setText(getActivity().getResources().getString(R.string.waiting_for_gps));
            binding.compassTextAccuracyGps.setText(getActivity().getResources().getString(R.string.accuracy_low));
        }
    }

    public double bearing(double startLat, double startLng, double endLat, double endLng){
        double latitude1 = Math.toRadians(startLat);
        double latitude2 = Math.toRadians(endLat);
        double longDiff = Math.toRadians(endLng - startLng);
        double y = Math.sin(longDiff)*Math.cos(latitude2);
        double x = Math.cos(latitude1)*Math.sin(latitude2)-Math.sin(latitude1)*Math.cos(latitude2)*Math.cos(longDiff);

        return (Math.toDegrees(Math.atan2(y, x))+360)%360;
    }

    boolean isTipShown = false;
    Snackbar mySnackbar;

    private Compass.CompassListener getCompassListener() {
        return new Compass.CompassListener() {
            public void onNewAzimuth(float azimuth) {
                ((Activity) context).runOnUiThread(() -> {
                    if (current_location.getAccuracy() == 0f) {
                        Log.d("ISTANZA", "posizione non accurata");
                        return;
                    } else {
                        Log.d("ISTANZA", "posizione accurata");
                    }

                    if (!activateCompass) {
                        Log.d("ISTANZA", "compass non attivo");
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
                                if (!isTipShown) {
                                    mySnackbar = Snackbar.make(getActivity().findViewById(android.R.id.content), CompassFragment.this.getResources().getString(R.string.compass_tipswitchtomap_title), Snackbar.LENGTH_INDEFINITE);
                                    mySnackbar.setAction(R.string.compass_tipswitchtomap_button, view -> {
                                        TabLayout tabs = binding.getRoot().findViewById(R.id.tabs);
                                        // per evitare warning
                                        TabLayout.Tab toSelect = tabs.getTabAt(0);
                                        if (toSelect != null)
                                            toSelect.select();
                                    });
                                    isTipShown = true;
                                    mySnackbar.show();
                                }
                            } else {
                                if (isTipShown) {
                                    isTipShown = false;
                                    mySnackbar.dismiss();
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
                Log.d("ISTANZA", "acc: " + accuracy);
                if(accuracy != SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM && accuracy != SensorManager.SENSOR_STATUS_ACCURACY_HIGH) {
                    binding.compassTextDifferenceDistance.setText(getResources().getString(R.string.waiting_for_sensors));
                    binding.compassTextAccuracyAccelerometer.setText(getResources().getString(R.string.accuracy_low));
                } else {
                    binding.compassTextAccuracyAccelerometer.setText(getResources().getString(R.string.accuracy_high));
                }
            }
        };
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
            warning_notif = Snackbar.make(getActivity().findViewById(android.R.id.content), getResources().getString(R.string.warning_noperms, getResources().getString(R.string.app_name)), Snackbar.LENGTH_INDEFINITE);
            View a = warning_notif.getView();
            a.setBackgroundColor(ContextCompat.getColor(context, R.color.red));
            warning_notif.show();

            binding.compassTextDifferenceDistance.setText("{{{(>_<)}}}");
        }

        if (checkPerms(context)) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, location_refresh_time, LOCATION_REFRESH_DISTANCE, this::onLocationChanged);
        }

        isTipShown = false;

        Marker selectedMarker = getCompassSelectedMarker();
        if(selectedMarker != null && selectedMarker.getTag() != null) {
            POIMarker tmarker = (POIMarker) selectedMarker.getTag();
            if(tmarker != null) {
                mCompassViewModel.getMarkerId(tmarker.getId()).observe(getViewLifecycleOwner(), targetMarker -> {
                    target.setLatitude(targetMarker.getLatitude());
                    target.setLongitude(targetMarker.getLongitude());
                });
            }
            compass.start();
            activateCompass = true;
            warning_notif = null;
            binding.compassTextDifferenceDistance.setText(getResources().getString(R.string.waiting_for_sensors));
        } else {
            compass.stop();
            activateCompass = false;
            if(warning_notif == null) {
                warning_notif = Snackbar.make(getActivity().findViewById(android.R.id.content), getResources().getString(R.string.compass_tipchoosetargetfirst), Snackbar.LENGTH_INDEFINITE);
                View a = warning_notif.getView();
                a.setBackgroundColor(ContextCompat.getColor(context, R.color.darkyellow));
                warning_notif.show();
            }
            binding.compassTextDifferenceDistance.setText("{{{(>_<)}}}");
        }
        Log.d("ISTANZA", "compassFragment -> onResume");
    }

    @Override
    public void onPause() {
        super.onPause();
        locationManager.removeUpdates(this::onLocationChanged);
        compass.stop();
        if (warning_notif != null) {
            warning_notif.dismiss();
            warning_notif = null;
        }
        if (mySnackbar != null) {
            mySnackbar.dismiss();
            mySnackbar = null;
        }
        Log.d("ISTANZA", "compassFragment -> onPause");
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