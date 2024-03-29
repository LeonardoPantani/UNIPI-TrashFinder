/*
 * Copyright (c) 2021/2022
 * Leonardo Pantani - 598896
 * University of Pisa - Department of Computer Science
 */

package it.unipi.di.pantani.trashfinder.data.marker;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import com.google.android.gms.maps.model.Marker;
import com.google.gson.Gson;

import java.io.Serializable;
import java.util.Set;

import it.unipi.di.pantani.trashfinder.R;

@Entity(tableName = "marker_table")
@TypeConverters(POIMarkerConverter.class)
public class POIMarker implements Serializable {
    @PrimaryKey(autoGenerate = true)
    private int id;

    public enum MarkerType {
        // vari tipi di cestini
        trashbin_indifferenziato,
        trashbin_plastica,
        trashbin_alluminio,
        trashbin_carta,
        trashbin_vetro,
        trashbin_organico,
        trashbin_farmaci,
        trashbin_pile,
        trashbin_olio,
        trashbin_vestiti,
        // isola ecologica
        recyclingdepot
    }

    public static String getMarkerTypeName(Context context, MarkerType t) {
        String ret = "";
        switch(t) {
            case trashbin_indifferenziato:
                ret += context.getString(R.string.markertype_undifferentiated);
                break;
            case trashbin_plastica:
                ret += context.getString(R.string.markertype_plastic);
                break;
            case trashbin_alluminio:
                ret += context.getString(R.string.markertype_aluminium);
                break;
            case trashbin_carta:
                ret += context.getString(R.string.markertype_paper);
                break;
            case trashbin_vetro:
                ret += context.getString(R.string.markertype_glass);
                break;
            case trashbin_organico:
                ret += context.getString(R.string.markertype_organic);
                break;
            case trashbin_farmaci:
                ret += context.getString(R.string.markertype_drugs);
                break;
            case trashbin_pile:
                ret += context.getString(R.string.markertype_batteries);
                break;
            case trashbin_olio:
                ret += context.getString(R.string.markertype_oil);
                break;
            case trashbin_vestiti:
                ret += context.getString(R.string.markertype_clothes);
                break;
            case recyclingdepot:
                ret += context.getString(R.string.markertype_recyclingdepot);
                break;
        }

        return ret;
    }

    @NonNull
    private final Set<MarkerType> types;

    private final double latitude;

    private final double longitude;

    private final String notes;

    public POIMarker(@NonNull Set<MarkerType> types, double latitude, double longitude, String notes) {
        this.types = types;
        this.latitude = latitude;
        this.longitude = longitude;
        this.notes = notes;
    }

    // GETTERS E SETTERS
    public int getId() {
        return id;
    }

    @NonNull
    public Set<MarkerType> getTypes() {
        return types;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public String getNotes() {
        return notes;
    }

    public void setId(int id) {
        this.id = id;
    }

    // METODI STATICI DI UTILITA' RELATIVI AI MARCATORI
    public static String getTitleFromMarker(Context context, POIMarker marker) {
        if(marker.getTypes().contains(POIMarker.MarkerType.recyclingdepot)) {
            return context.getString(R.string.markertype_recyclingdepot);
        } else {
            return context.getString(R.string.markertype_generic);
        }
    }

    public static POIMarker getPOIMarkerByMarker(Marker marker) {
        return new Gson().fromJson(marker.getSnippet(), POIMarker.class);
    }
}
