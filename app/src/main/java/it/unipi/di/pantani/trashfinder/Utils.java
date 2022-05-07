package it.unipi.di.pantani.trashfinder;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import androidx.core.app.ActivityCompat;
import androidx.preference.PreferenceManager;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.gson.Gson;

import it.unipi.di.pantani.trashfinder.data.POIMarker;

public abstract class Utils {
    public static final int default_location_refresh_time = 3; // in seconds
    public static final String SAD_EMOJI = "{{{(>_<)}}}";
    public static final String FEEDBACK_MAIL = "l.pantani5@studenti.unipi.it";
    // codice pisa: 3600042527 | codice italia: 3600365331 | codice toscana: 3600041977
    public static final String OSM_IMPORT_STRING = "https://www.overpass-api.de/api/interpreter?data=[out:json];area(id:3600042527)-%3E.searchArea;(node[%22amenity%22=%22waste_basket%22](area.searchArea);node[%22amenity%22=%22waste_disposal%22](area.searchArea);node[%22amenity%22=%22recycling%22](area.searchArea););out%20body;%3E;out%20skel%20qt;";

    public static final double DEFAULT_LOCATION_LAT = 41.902782;
    public static final double DEFAULT_LOCATION_LON = 12.496366;

    public static final int DEFAULT_ZOOM = 5;
    public static final int LOCATION_ZOOM = 18;
    public static final int MARKER_ZOOM = 19;

    public static final int REQUIRE_PERMISSION_CODE_INTRO = 1;

    public static boolean checkPerms(Context context) {
        return ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    public static void triggerRebirth(Context context) {
        PackageManager packageManager = context.getPackageManager();
        Intent intent = packageManager.getLaunchIntentForPackage(context.getPackageName());
        ComponentName componentName = intent.getComponent();
        Intent mainIntent = Intent.makeRestartActivityTask(componentName);
        context.startActivity(mainIntent);
        Runtime.getRuntime().exit(0);
    }

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

    public static void setPreference(Context context, String preferenceName, boolean preferenceValue) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putBoolean(preferenceName, preferenceValue);
        editor.apply();
    }

    public static void setPreference(Context context, String preferenceName, String preferenceValue) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putString(preferenceName, preferenceValue);
        editor.apply();
    }

    // MAPPA
    public static void pointLocation(Context context, GoogleMap gmap) {
        if(checkPerms(context)) {
            LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            @SuppressLint("MissingPermission") Location location = locationManager.getLastKnownLocation(locationManager.getBestProvider(new Criteria(), false));
            if (location != null) {
                CameraPosition cameraPosition = new CameraPosition.Builder()
                        .target(new LatLng(location.getLatitude(), location.getLongitude()))
                        .zoom(LOCATION_ZOOM)
                        .build();
                gmap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
            } else {
                CameraPosition cameraPosition = new CameraPosition.Builder()
                        .target(new LatLng(DEFAULT_LOCATION_LAT, DEFAULT_LOCATION_LON))
                        .zoom(DEFAULT_ZOOM)
                        .build();
                gmap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
            }
        } else {
            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(new LatLng(DEFAULT_LOCATION_LAT, DEFAULT_LOCATION_LON))
                    .zoom(DEFAULT_ZOOM)
                    .build();
            gmap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        }
    }

    public static void updateMapStyleByPreference(Context context, SharedPreferences sp, GoogleMap gmap) {
        if(gmap == null) return; // appena avviata l'app, questo metodo è chiamato dalla "onResume" con mMap non valido quindi ignoro

        String setting_map_theme = sp.getString("setting_map_theme", null);

        if ("classic".equals(setting_map_theme)) {
            gmap.setMapStyle(MapStyleOptions.loadRawResourceStyle(context, R.raw.map_style_classic));
        } else if ("light".equals(setting_map_theme)) {
            gmap.setMapStyle(MapStyleOptions.loadRawResourceStyle(context, R.raw.map_style_light));
        } else if ("dark".equals(setting_map_theme)) {
            gmap.setMapStyle(MapStyleOptions.loadRawResourceStyle(context, R.raw.map_style_dark));
        } else { // è in modalità auto
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

    // MARCATORE
    public static String getTitleFromMarker(Context context, POIMarker marker) {
        //Log.d("ISTANZA", "tipi: " + marker.getType().toString());
        if(marker.getType().contains(POIMarker.MarkerType.recyclingdepot)) {
            return context.getString(R.string.markertype_recyclingdepot);
        } else {
            return context.getString(R.string.markertype_generic);
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

    public static POIMarker getPOIMarkerByMarker(Marker marker) {
        return new Gson().fromJson(marker.getSnippet(), POIMarker.class);
    }

    // CHIUDI TASTIERA
    public static void closeKeyboard(Activity a) {
        // Check if no view has focus:
        View view = a.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager)a.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
}
