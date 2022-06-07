/*
 * Copyright (c) 2021/2022
 * Leonardo Pantani - 598896
 * University of Pisa - Department of Computer Science
 */

package it.unipi.di.pantani.trashfinder.data.marker;

import androidx.room.TypeConverter;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.Set;

/**
 * Classe per tramutare una lista di marker in stringa e viceversa. Serve al DAO per poter
 * salvare alcuni dati particolari nel database SQLite.
 */
public class POIMarkerConverter {
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