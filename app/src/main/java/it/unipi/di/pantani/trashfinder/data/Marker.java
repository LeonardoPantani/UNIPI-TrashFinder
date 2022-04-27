package it.unipi.di.pantani.trashfinder.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "marker_table")
public class Marker {
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
        // isola ecologica
        recyclingdepot // magenta
    }
    @NonNull
    private MarkerType type;

    private double latitude;

    private double longitude;

    private String notes;

    public Marker(@NonNull MarkerType type, double latitude, double longitude, String notes) {
        this.type = type;
        this.latitude = latitude;
        this.longitude = longitude;
        this.notes = notes;
    }

    public int getId() {
        return id;
    }

    @NonNull
    public MarkerType getType() {
        return type;
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
