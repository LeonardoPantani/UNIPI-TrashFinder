package it.unipi.di.pantani.trashfinder.data.requests;

import androidx.annotation.NonNull;
import androidx.room.PrimaryKey;

import it.unipi.di.pantani.trashfinder.data.marker.POIMarker;

public class POIRequest {
    @PrimaryKey(autoGenerate = true)
    private int id;

    @NonNull
    private final POIMarker element;

    private final long date;

    private final String imageLink;

    private final int userId;

    public POIRequest(@NonNull POIMarker element, long date, String imageLink, int userId) {
        this.element = element;
        this.date = date;
        this.imageLink = imageLink;
        this.userId = userId;
    }
}
