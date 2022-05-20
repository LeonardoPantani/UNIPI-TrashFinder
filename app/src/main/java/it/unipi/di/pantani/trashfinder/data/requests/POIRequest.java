package it.unipi.di.pantani.trashfinder.data.requests;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import it.unipi.di.pantani.trashfinder.data.marker.POIMarker;

@Entity(tableName = "poirequest_table")
@TypeConverters(MarkerConverter.class)
public class POIRequest {
    @PrimaryKey(autoGenerate = true)
    private int id;

    @NonNull
    private final POIMarker element;

    private final long date;

    private final String imageLink;

    private final String userEmail;

    public POIRequest(@NonNull POIMarker element, long date, String imageLink, String userEmail) {
        this.element = element;
        this.date = date;
        this.imageLink = imageLink;
        this.userEmail = userEmail;
    }

    // GETTERS E SETTERS
    public int getId() {
        return id;
    }

    @NonNull
    public POIMarker getElement() {
        return element;
    }

    public long getDate() {
        return date;
    }

    public String getImageLink() {
        return imageLink;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setId(int id) {
        this.id = id;
    }
}
