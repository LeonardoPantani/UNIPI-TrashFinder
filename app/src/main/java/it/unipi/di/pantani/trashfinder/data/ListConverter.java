package it.unipi.di.pantani.trashfinder.data;

import androidx.room.TypeConverter;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.Set;

public class ListConverter {
    @TypeConverter
    public static Set<POIMarker.MarkerType> stringToMarkers(String json) {
        Gson gson = new Gson();
        Type type = new TypeToken<Set<POIMarker.MarkerType>>() {}.getType();
        return gson.fromJson(json, type);
    }

    @TypeConverter
    public static String markersToString(Set<POIMarker.MarkerType> list) {
        Gson gson = new Gson();
        Type type = new TypeToken<Set<POIMarker.MarkerType>>() {}.getType();
        return gson.toJson(list, type);
    }
}