package it.unipi.di.pantani.trashfinder.data.requests;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import it.unipi.di.pantani.trashfinder.data.marker.POIMarker;

@Entity(tableName = "poirequest_table")
@TypeConverters(MarkerConverter.class)
public class POIRequest {
    @PrimaryKey(autoGenerate = true)
    private int id;

    private final POIMarker element;

    private final long date;

    private final String imageLink;

    private final String userEmail;

    /**
     * se vero:  this.element contiene l'elemento candidato per l'eliminazione
     * se falso: this.element contiene l'elemento aggiornato della richiesta
     */
    private final boolean deletion;

    public POIRequest(POIMarker element, long date, String imageLink, String userEmail, boolean deletion) {
        this.element = element;
        this.deletion = deletion;
        this.date = date;
        this.imageLink = imageLink;
        this.userEmail = userEmail;
    }

    // GETTERS E SETTERS
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public POIMarker getElement() {
        return element;
    }

    public boolean getDeletion() {
        return deletion;
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
}
