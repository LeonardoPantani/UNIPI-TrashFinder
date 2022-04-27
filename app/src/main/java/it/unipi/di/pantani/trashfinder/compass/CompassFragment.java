package it.unipi.di.pantani.trashfinder.compass;

import static it.unipi.di.pantani.trashfinder.Utils.checkPerms;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
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

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;

import it.unipi.di.pantani.trashfinder.Utils;
import trashfinder.R;

public class CompassFragment extends Fragment {
    SharedPreferences sp;
    private ImageView compass_icon;
    private float azim = 0f;
    private float currentAzimuth = 0f;
    private TextView compass_text_distance;
    private Location current_location = new Location("A");
    private final Location target = new Location("B");
    private LocationManager locationManager;

    private String measureUnit_low;
    private String measureUnit_high;
    private int measureUnitCode;

    private Context context;

    int location_refresh_time;
    int LOCATION_REFRESH_DISTANCE = 0;

    private View view;
    private Snackbar warning_notif = null;

    private Compass compass;
    private boolean showTip;

    private boolean activateCompass;

    private CompassViewModel mCompassViewModel;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
        Log.d("ISTANZA", "attaccato compass");
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        view = inflater.inflate(R.layout.fragment_compass, container, false);

        sp = PreferenceManager.getDefaultSharedPreferences(context);

        compass_icon = view.findViewById(R.id.compass_icon);
        compass_text_distance = view.findViewById(R.id.compass_text_difference_distance);

        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

        compass = new Compass(context);
        setupCompass();

        AzimuthFormatter azimuthFormatter = new AzimuthFormatter(context);

        // view model
        mCompassViewModel = new ViewModelProvider(this).get(CompassViewModel.class);

        return view;
    }

    private void setupCompass() {
        compass = new Compass(context);
        Compass.CompassListener cl = getCompassListener();
        compass.setListener(cl);
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onResume() {
        super.onResume();
        Log.d("ISTANZA", "resume compass");

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
            warning_notif = Snackbar.make(view.findViewById(R.id.compass), getResources().getString(R.string.warning_noperms, getResources().getString(R.string.app_name)), Snackbar.LENGTH_INDEFINITE);
            View a = warning_notif.getView();
            a.setBackgroundColor(ContextCompat.getColor(context, R.color.red));
            warning_notif.show();

            compass_text_distance.setText("{{{(>_<)}}}");
        }

        compass.start();

        if (checkPerms(context)) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, location_refresh_time,
                    LOCATION_REFRESH_DISTANCE, this::onLocationChanged);
        }

        isTipShown = false;


        String markerid = sp.getString("compass_markerid", "invalid");

        if(!markerid.equals("invalid")) {
            mCompassViewModel.getMarkerId(Integer.parseInt(markerid)).observe(getViewLifecycleOwner(), targetMarker -> {
                target.setLatitude(targetMarker.getLatitude());
                target.setLongitude(targetMarker.getLongitude());
            });
            activateCompass = true;
            warning_notif = null;
        } else {
            activateCompass = false;
            if(warning_notif == null) {
                warning_notif = Snackbar.make(view.findViewById(R.id.compass), getResources().getString(R.string.compass_tipchoosetargetfirst), Snackbar.LENGTH_INDEFINITE);
                View a = warning_notif.getView();
                a.setBackgroundColor(ContextCompat.getColor(context, R.color.darkyellow));
                warning_notif.show();
            }
            compass_text_distance.setText("{{{(>_<)}}}");
        }
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
    }

    public void onLocationChanged(Location location) {
        current_location = location;
    }

    public double bearing(double startLat, double startLng, double endLat, double endLng){
        double latitude1 = Math.toRadians(startLat);
        double latitude2 = Math.toRadians(endLat);
        double longDiff= Math.toRadians(endLng - startLng);
        double y= Math.sin(longDiff)*Math.cos(latitude2);
        double x=Math.cos(latitude1)*Math.sin(latitude2)-Math.sin(latitude1)*Math.cos(latitude2)*Math.cos(longDiff);

        return (Math.toDegrees(Math.atan2(y, x))+360)%360;
    }

    boolean isTipShown = false;
    Snackbar mySnackbar;
    private Compass.CompassListener getCompassListener() {
        return azimuth -> ((Activity) context).runOnUiThread(() -> {
            if(current_location.getAccuracy() == 0f || !activateCompass) return;

            float dist = current_location.distanceTo(target);
            if(measureUnitCode == 1) {
                dist *= 3.281;
            }

            azim = (float) (azimuth - bearing(current_location.getLatitude(), current_location.getLongitude(), target.getLatitude(), target.getLongitude()));

            if(dist < 1000) {
                compass_text_distance.setText(context.getResources().getString(R.string.distance_difference, dist, measureUnit_low));

                if(showTip) {
                    if((dist < 10 && measureUnitCode == 0) || (dist < 32 && measureUnitCode == 1)) {
                        if(!isTipShown) {
                            mySnackbar = Snackbar.make(view.findViewById(R.id.compass), getResources().getString(R.string.compass_tipswitchtomap_title), Snackbar.LENGTH_INDEFINITE);
                            mySnackbar.setAction(R.string.compass_tipswitchtomap_button, view -> {
                                TabLayout tabs = view.getRootView().findViewById(R.id.tabs);
                                // per evitare warning
                                TabLayout.Tab toSelect = tabs.getTabAt(0);
                                if(toSelect != null)
                                    toSelect.select();
                            });
                            isTipShown = true;
                            mySnackbar.show();
                        }
                    } else {
                        if(isTipShown) {
                            isTipShown = false;
                            mySnackbar.dismiss();
                        }
                    }
                }
            } else {
                if(measureUnitCode == 0) {
                    dist /= 1000;
                } else {
                    dist /= 5280;
                }
                compass_text_distance.setText(context.getResources().getString(R.string.distance_difference, dist, measureUnit_high));
            }

            Animation an = new RotateAnimation(-currentAzimuth, -azim,
                    Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF,
                    0.5f);
            currentAzimuth = azim;

            an.setDuration(500);
            an.setRepeatCount(0);
            an.setFillAfter(true);

            compass_icon.startAnimation(an);
        });
    }
}