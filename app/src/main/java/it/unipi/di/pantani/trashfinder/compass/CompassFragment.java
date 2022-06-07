/*
 * Copyright (c) 2021/2022
 * Leonardo Pantani - 598896
 * University of Pisa - Department of Computer Science
 */

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
    private Context mContext;
    private FragmentCompassBinding mBinding;

    private float mAzimuth = 0f;
    private float mCurrentAzimuth = 0f;
    private Location mCurrentLocation = new Location("A");
    private final Location target = new Location("B");

    private boolean mShowTip;
    private Snackbar mTipCloseToTarget;

    private String mMeasureUnitLow;
    private String mMeasureUnitHigh;
    private int mMeasureUnitCode;

    private Compass mCompass;
    private UserLocation mUserLocation;

    private boolean mReadyLocation;
    private boolean mReadySensor;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        sp = PreferenceManager.getDefaultSharedPreferences(mContext);
        mBinding = FragmentCompassBinding.inflate(inflater, container, false);
        View root = mBinding.getRoot();
        // preparo la bussola
        setupCompass();
        setupLocation();
        // onClick su cardview
        mBinding.compassCardviewMain.setOnClickListener(this::onClickCardView);
        // onClick su button
        mBinding.compassCardviewDeselectButton.setOnClickListener(this::onClickDeselectButton);
        return root;
    }

    /**
     * Preparazione bussola.
     */
    private void setupCompass() {
        mCompass = new Compass(mContext);
        Compass.CompassListener cl = getCompassListener();
        mCompass.setListener(cl);
        mReadyLocation = false;
        mReadySensor = false;
    }

    /**
     * Preparazione posizione.
     */
    private void setupLocation() {
        mUserLocation = new UserLocation(mContext);
        UserLocation.UserLocationListener ull = getUserLocationListener();
        mUserLocation.setListener(ull);
        Location temp = mUserLocation.getLastLocation();
        if(temp != null) {
            mCurrentLocation = temp;
            mReadyLocation = true;
        }
    }

    /**
     * Callbacks chiamati dalla classe UserLocation quando cambia qualcosa.
     * @return il corpo delle funzioni da chiamare
     */
    private UserLocation.UserLocationListener getUserLocationListener() {
        return new UserLocation.UserLocationListener() {
            @Override
            public void onNewLocation(Location newLoc) {
                if(newLoc.getAccuracy() != 0f) {
                    mCurrentLocation = newLoc;
                }
            }

            @Override
            public void onAccuracyChanged(float accuracy) {
                if(accuracy != 0f) {
                    mReadyLocation = true;
                    mBinding.compassTextAccuracyGps.setText(getResources().getString(R.string.accuracy_high));
                } else {
                    mReadyLocation = false;
                    mBinding.compassTextAccuracyGps.setText(getResources().getString(R.string.accuracy_low));
                }
            }
        };
    }

    /**
     * Callbacks chiamati dalla classe Compass quando cambia qualcosa.
     * @return il corpo delle funzioni da chiamare
     */
    private Compass.CompassListener getCompassListener() {
        return new Compass.CompassListener() {
            public void onNewAzimuth(float azimuth) {
                ((Activity) mContext).runOnUiThread(() -> {
                    if(!isCompassReady()) return;

                    float dist = mCurrentLocation.distanceTo(target);
                    if (mMeasureUnitCode == 1) {
                        dist *= 3.281;
                    }

                    mAzimuth = (float) (azimuth - Compass.bearing(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude(), target.getLatitude(), target.getLongitude()));

                    if (dist < 1000) {
                        mBinding.compassTextDifferenceDistance.setText(mContext.getResources().getString(R.string.distance_difference, dist, mMeasureUnitLow));

                        if (mShowTip) {
                            if ((dist < 50 && mMeasureUnitCode == 0) || (dist < 164 && mMeasureUnitCode == 1)) {
                                if (mTipCloseToTarget == null) {
                                    mTipCloseToTarget = Snackbar.make(((Activity) mContext).findViewById(android.R.id.content), CompassFragment.this.getResources().getString(R.string.compass_tipswitchtomap_title), Snackbar.LENGTH_INDEFINITE);
                                    mTipCloseToTarget.setAction(R.string.compass_tipswitchtomap_button, view -> {
                                        // ottengo il navController
                                        NavHostFragment navHostFragment = (NavHostFragment) requireActivity().getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment_content_main);
                                        if(navHostFragment == null) { // non dovrebbe mai verificarsi!
                                            Log.d("ISTANZA", "navHostFragment null (fragment)!");
                                            return;
                                        }
                                        NavController navController = navHostFragment.getNavController();
                                        navController.popBackStack();
                                    });
                                    mTipCloseToTarget.show();
                                }
                            } else {
                                if (mTipCloseToTarget != null) {
                                    mTipCloseToTarget.dismiss();
                                }
                            }
                        }
                    } else {
                        if (mMeasureUnitCode == 0) {
                            dist /= 1000;
                        } else {
                            dist /= 5280;
                        }
                        mBinding.compassTextDifferenceDistance.setText(mContext.getResources().getString(R.string.distance_difference, dist, mMeasureUnitHigh));
                    }

                    Animation an = new RotateAnimation(-mCurrentAzimuth, -mAzimuth,
                            Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF,
                            0.5f);

                    mCurrentAzimuth = mAzimuth;

                    an.setDuration(500);
                    an.setRepeatCount(0);
                    an.setFillAfter(true);

                    mBinding.compassIcon.startAnimation(an);
                });
            }

            @Override
            public void onSensorAccuracyChanged(float accuracy) {
                if(accuracy == SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM || accuracy == SensorManager.SENSOR_STATUS_ACCURACY_HIGH) {
                    mReadySensor = true;
                    mBinding.compassTextAccuracyAccelerometer.setText(getResources().getString(R.string.accuracy_high));
                } else {
                    mReadySensor = false;
                    mBinding.compassTextAccuracyAccelerometer.setText(getResources().getString(R.string.accuracy_low));
                }
            }
        };
    }

    /**
     * Listener del click sulla cardview.
     * @param view la view cliccata
     */
    private void onClickCardView(View view) {
        Navigation.findNavController(view).popBackStack(R.id.nav_maps, false); // primo
    }

    /**
     * Listener del click sul pulsante di deselezione del cestino cliccato.
     * @param view non usato
     */
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
            mCompass.start();
            mUserLocation.start(sp.getInt("setting_compass_update_interval", 1)*1000);
            target.setLatitude(targetMarker.getLatitude());
            target.setLongitude(targetMarker.getLongitude());
            showCompassInfo(targetMarker);
        } else { // se non ho un marker selezionato
            mCompass.stop();
            mUserLocation.stop();
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
            mBinding.compassCardviewHeaderimage.setImageResource(R.drawable.compass_cardheader_binselected);
        } else {
            mBinding.compassCardviewHeaderimage.setImageResource(R.drawable.compass_cardheader_rdselected);
        }

        mBinding.compassCardviewTitle.setText(getTitleFromMarker(mContext, selectedMarker));
        mBinding.compassCardviewDeselectButton.setVisibility(View.VISIBLE);

        Set<POIMarker.MarkerType> types = selectedMarker.getTypes();
        types.remove(POIMarker.MarkerType.recyclingdepot);

        String s = types.stream()
                .map(t -> getMarkerTypeName(mContext, t))
                .collect(Collectors.joining(", "));
        if(!s.equals(""))
            mBinding.compassCardviewDesc.setText(s);
        else
            mBinding.compassCardviewDesc.setText(getResources().getString(R.string.compass_cardview_notype));

        // mostro la sezione della precisione e imposto i testi su LOW, tanto saranno aggiornati da altri metodi
        mBinding.compassIcon.setVisibility(View.VISIBLE);
        mBinding.compassTextDifferenceDistance.setVisibility(View.VISIBLE);

        mBinding.compassTextAccuracyGps.setText(getResources().getString(R.string.accuracy_low));
        mBinding.compassTextAccuracyAccelerometer.setText(getResources().getString(R.string.accuracy_low));
        mBinding.compassSectionAccuracy.setVisibility(View.VISIBLE);
    }

    /**
     * Lavora sulla UI. Nasconde la freccia e altre informazioni. Mostra solo una cardview con
     * dove si informa l'utente di scegliere un marker da seguire.
     */
    private void hideCompassInfo() {
        if(mTipCloseToTarget != null) mTipCloseToTarget.dismiss();

        mBinding.compassTextDifferenceDistance.setText("");

        mBinding.compassIcon.clearAnimation();
        mBinding.compassIcon.setVisibility(View.GONE);
        mBinding.compassTextDifferenceDistance.setVisibility(View.GONE);

        mBinding.compassCardviewHeaderimage.setImageResource(R.drawable.compass_cardheader_noselected);
        mBinding.compassCardviewTitle.setText(getResources().getString(R.string.compass_selecteditem_noitem_title));
        mBinding.compassCardviewDesc.setText(getResources().getString(R.string.compass_selecteditem_noitem_desc));
        mBinding.compassCardviewDeselectButton.setVisibility(View.GONE);

        mBinding.compassSectionAccuracy.setVisibility(View.GONE);
    }

    /**
     * Modifica la UI in modo da mostrare un avviso di mancanza di permessi
     */
    private void handleNoLocation() {
        mBinding.compassTextDifferenceDistance.setText("");

        mBinding.compassIcon.clearAnimation();
        mBinding.compassIcon.setVisibility(View.GONE);
        mBinding.compassTextDifferenceDistance.setVisibility(View.GONE);

        mBinding.compassCardviewHeaderimage.setImageResource(R.drawable.compass_cardheader_noselected);
        mBinding.compassCardviewTitle.setText(getResources().getString(R.string.compass_nopermission_title));
        mBinding.compassCardviewDesc.setText(getResources().getString(R.string.compass_nopermission_desc, getResources().getString(R.string.app_name)));
        mBinding.compassCardviewDeselectButton.setVisibility(View.GONE);

        mBinding.compassSectionAccuracy.setVisibility(View.GONE);
    }

    /**
     * Cambia un testo sotto la freccia per indicare se si sta aspettando che sia pronto il gps o i
     * sensori; e restituisce lo stato della bussola.
     * @return vero se la bussola è pronta, falso altrimenti
     */
    private boolean isCompassReady() {
        if(!mReadyLocation) {
            mBinding.compassTextDifferenceDistance.setText(getResources().getString(R.string.waiting_for_gps));
            return false;
        }
        if(!mReadySensor) {
            mBinding.compassTextDifferenceDistance.setText(getResources().getString(R.string.waiting_for_sensors));
            return false;
        }
        return true;
    }

    @Override
    public void onPause() {
        super.onPause();

        mCompass.stop();
        mUserLocation.stop();

        if (mTipCloseToTarget != null) {
            mTipCloseToTarget.dismiss();
            mTipCloseToTarget = null;
        }
        Log.d("ISTANZA", "compass -> onPause");
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d("ISTANZA", "compass -> onResume");

        // aggiorno le variabili con le preferenze
        mShowTip = sp.getBoolean("setting_compass_show_tip_switchtomap", true);
        if(sp.getString("setting_compass_measureunit", "meters").equals("meters")) {
            mMeasureUnitCode = 0;
            mMeasureUnitLow = mContext.getResources().getString(R.string.setting_compass_measureunit_meters);
            mMeasureUnitHigh = mContext.getResources().getString(R.string.setting_compass_measureunit_kilometers);
        } else {
            mMeasureUnitCode = 1;
            mMeasureUnitLow = mContext.getResources().getString(R.string.setting_compass_measureunit_feet);
            mMeasureUnitHigh = mContext.getResources().getString(R.string.setting_compass_measureunit_miles);
        }

        // mostro un avviso in caso di mancanza di permessi
        if(!checkPerms(mContext)) {
            handleNoLocation();
        } else { // altrimenti gestisco il compass
            handleCompass();
        }
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.mContext = context;
        Log.d("ISTANZA", "compass -> onAttach");
    }

    @Override
    public void onDetach() {
        super.onDetach();
        Log.d("ISTANZA", "compass -> onDetach");
    }
}