package it.unipi.di.pantani.trashfinder.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import java.util.Set;

@Entity(tableName = "marker_table")
@TypeConverters(ListConverter.class)
public class POIMarker {
    @PrimaryKey(autoGenerate = true)
    protected int id;

    public enum MarkerType {
        // vari tipi di cestini
        trashbin_indifferenziato, // red
        trashbin_plastica, // azure
        trashbin_alluminio, // blue
        trashbin_carta, // green
        trashbin_vetro, // cyan
        trashbin_organico, // orange
        trashbin_farmaci, // violet
        trashbin_pile, // yellow
        trashbin_olio, // rose
        trashbin_vestiti, // green
        // isola ecologica
        recyclingdepot // magenta
    }
    @NonNull
    public Set<MarkerType> types;

    private final double latitude;

    private final double longitude;

    private final String notes;

    public POIMarker(@NonNull Set<MarkerType> types, double latitude, double longitude, String notes) {
        this.types = types;
        this.latitude = latitude;
        this.longitude = longitude;
        this.notes = notes;
    }

    public int getId() {
        return id;
    }

    @NonNull
    public Set<MarkerType> getType() {
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
}
