/*
 * Copyright (c) 2021/2022
 * Leonardo Pantani - 598896
 * University of Pisa - Department of Computer Science
 */

package it.unipi.di.pantani.trashfinder.data.requests;

import androidx.room.TypeConverter;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;

import it.unipi.di.pantani.trashfinder.data.marker.POIMarker;

/**
 * Classe per tramutare una lista di requests in stringa e viceversa. Serve al DAO per poter
 * salvare alcuni dati particolari nel database SQLite.
 */
public class POIRequestConverter {
    @TypeConverter
    public static POIMarker stringToPOIMarker(String json) {
        Gson gson = new Gson();
        Type type = new TypeToken<POIMarker>() {}.getType();
        return gson.fromJson(json, type);
    }

    @TypeConverter
    public static String poiMarkerToString(POIMarker m) {
        Gson gson = new Gson();
        Type type = new TypeToken<POIMarker>() {}.getType();
        return gson.toJson(m, type);
    }
}
