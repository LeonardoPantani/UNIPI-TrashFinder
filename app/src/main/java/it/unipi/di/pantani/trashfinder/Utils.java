package it.unipi.di.pantani.trashfinder;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;

import androidx.core.app.ActivityCompat;

public class Utils {
    public static final int default_location_refresh_time = 3;
    public static final int[] TAB_TITLES = new int[]{R.string.tab_name_maps, R.string.tab_name_compass, R.string.tab_name_mapeditor};

    public static boolean checkPerms(Context context) {
        return ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }
}
