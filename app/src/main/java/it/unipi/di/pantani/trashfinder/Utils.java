package it.unipi.di.pantani.trashfinder;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;

import androidx.core.app.ActivityCompat;
import androidx.preference.PreferenceManager;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;

public abstract class Utils extends Application {
    // indirizzo email a cui sono mandati i feedback
    public static final String FEEDBACK_MAIL = "l.pantani5@studenti.unipi.it";
    // link api. codice pisa: 3600042527 | codice italia: 3600365331 | codice toscana: 3600041977
    public static final String OSM_IMPORT_STRING = "https://www.overpass-api.de/api/interpreter?data=[out:json];area(id:3600365331)-%3E.searchArea;(node[%22amenity%22=%22waste_basket%22](area.searchArea);node[%22amenity%22=%22waste_disposal%22](area.searchArea);node[%22amenity%22=%22recycling%22](area.searchArea););out%20body;%3E;out%20skel%20qt;";

    // coordinate di default nel caso l'utente non dia l'accesso alla posizione
    public static final double DEFAULT_LOCATION_LAT = 41.902782;
    public static final double DEFAULT_LOCATION_LON = 12.496366;

    // vari livelli di zoom usati in vari contesti su una mappa
    public static final int DEFAULT_ZOOM = 5;
    public static final int LOCATION_ZOOM = 18;
    public static final int MARKER_ZOOM = 19;
    public static final int EDITMODE_NO_CLUSTER_MIN_ZOOM = 16;

    // codice richiesta permessi nell'introduzione all'app
    public static final int REQUIRE_PERMISSION_CODE_INTRO = 1;


    /**
     * Verifica che determinati permessi siano stati forniti.
     * @param context contesto
     * @return vero se ho i permessi necessari, falso altrimenti
     */
    public static boolean checkPerms(Context context) {
        return ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Forza il riavvio dell'app
     * @param context contesto
     */
    public static void triggerRebirth(Context context) {
        PackageManager packageManager = context.getPackageManager();
        Intent intent = packageManager.getLaunchIntentForPackage(context.getPackageName());
        ComponentName componentName = intent.getComponent();
        Intent mainIntent = Intent.makeRestartActivityTask(componentName);
        context.startActivity(mainIntent);
        Runtime.getRuntime().exit(0);
    }

    /**
     * Restituisce se il dispositivo ha la modalità notte attiva o meno
     * @param context contesto
     * @return 1 se la modalità notte è attiva, 0 altrimenti, -1 in altri casi
     */
    public static int getThemeMode(Context context) {
        int nightModeFlags = context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        switch (nightModeFlags) {
            case Configuration.UI_MODE_NIGHT_YES:
                return 1;

            case Configuration.UI_MODE_NIGHT_UNDEFINED:
            case Configuration.UI_MODE_NIGHT_NO:
                return 0;

            default:
                return -1;
        }
    }

    /**
     * Restituisce se il dispositivo è connesso ad Internet o no.
     * @return vero se il dispositivo è connesso, falso altrimenti
     */
    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    /**
     * Imposta una preferenza
     * @param context contesto
     * @param preferenceName nome della preferenza da impostare
     * @param preferenceValue il valore della preferenza
     */
    public static void setPreference(Context context, String preferenceName, boolean preferenceValue) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putBoolean(preferenceName, preferenceValue);
        editor.apply();
    }

    /**
     * Imposta una preferenza
     * @param context contesto
     * @param preferenceName nome della preferenza da impostare
     * @param preferenceValue il valore della preferenza
     */
    public static void setPreference(Context context, String preferenceName, String preferenceValue) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putString(preferenceName, preferenceValue);
        editor.apply();
    }

    /**
     * Fa puntare la mappa a delle coordinate specifiche. Se ho i permessi validi, punto la mappa
     * sulla posizione dell'utente, altrimenti vado a coordinate generiche.
     * @param context contesto
     * @param gmap la mappa su cui agire
     */
    @SuppressLint("MissingPermission")
    public static void pointLocation(Context context, GoogleMap gmap) {
        if(checkPerms(context)) {
            LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            Location location = locationManager.getLastKnownLocation(locationManager.getBestProvider(new Criteria(), false));

            CameraPosition cameraPosition;
            if (location != null) {
                cameraPosition = new CameraPosition.Builder()
                        .target(new LatLng(location.getLatitude(), location.getLongitude()))
                        .zoom(LOCATION_ZOOM)
                        .build();
            } else {
                cameraPosition = new CameraPosition.Builder()
                        .target(new LatLng(DEFAULT_LOCATION_LAT, DEFAULT_LOCATION_LON))
                        .zoom(DEFAULT_ZOOM)
                        .build();
            }
            gmap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        } else {
            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(new LatLng(DEFAULT_LOCATION_LAT, DEFAULT_LOCATION_LON))
                    .zoom(DEFAULT_ZOOM)
                    .build();
            gmap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        }
    }


    /**
     * Aggiorno lo stile di mappa secondo le preferenze dell'utente
     * @param context contesto
     * @param gmap la mappa su cui agire
     */
    public static void updateMapStyleByPreference(Context context, GoogleMap gmap) {
        if(gmap == null) return;
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        String setting_map_theme = sp.getString("setting_map_theme", null);

        if ("classic".equals(setting_map_theme)) {
            gmap.setMapStyle(MapStyleOptions.loadRawResourceStyle(context, R.raw.map_style_classic));
        } else if ("light".equals(setting_map_theme)) {
            gmap.setMapStyle(MapStyleOptions.loadRawResourceStyle(context, R.raw.map_style_light));
        } else if ("dark".equals(setting_map_theme)) {
            gmap.setMapStyle(MapStyleOptions.loadRawResourceStyle(context, R.raw.map_style_dark));
        } else { // è in modalità automatica
            if(getThemeMode(context) == 1) { // modalità notte attiva (metto il tema scuro)
                gmap.setMapStyle(MapStyleOptions.loadRawResourceStyle(context, R.raw.map_style_dark));
            } else {
                gmap.setMapStyle(MapStyleOptions.loadRawResourceStyle(context, R.raw.map_style_light));
            }
        }

        String setting_map_type = sp.getString("setting_map_type", null);
        if("roads".equals(setting_map_type)) {
            gmap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        } else if("satellite".equals(setting_map_type)) {
            gmap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        } else {
            gmap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        }
    }

    static Marker compassSelectedMarker;
    public static void setCompassSelectedMarker(Marker m) {
        compassSelectedMarker = m;
    }
    public static Marker getCompassSelectedMarker() {
        return compassSelectedMarker;
    }

    static Marker editorSelectedMarker;
    public static Marker getEditorSelectedMarker() {
        return editorSelectedMarker;
    }
    public static void setEditorSelectedMarker(Marker editorSelectedMarker) {
        Utils.editorSelectedMarker = editorSelectedMarker;
    }

    /**
     * Chiude la tastiera.
     * @param a l'activity attuale
     */
    public static void closeKeyboard(Activity a) {
        View view = a.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager)a.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    /**
     * Restituisce la rotazione del dispositivo.
     * @param context contesto
     * @return un intero che definisce la rotazione, usare Surface.ROTATION_x per i tipi di orientamento standard
     */
    public static int getRotation(Context context) {
        return ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getRotation();
    }
}
