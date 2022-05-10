package it.unipi.di.pantani.trashfinder.data.requests;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

import it.unipi.di.pantani.trashfinder.data.loggedusers.LoggedUser;
import it.unipi.di.pantani.trashfinder.data.marker.POIMarker;

@Entity(tableName = "requests_table",
        foreignKeys = {@ForeignKey(entity = LoggedUser.class,
        parentColumns = "id",
        childColumns = "userId",
        onDelete = ForeignKey.CASCADE)
})
public class POIRequest {
    @PrimaryKey(autoGenerate = true)
    private int id;

    @NonNull
    private POIMarker element;

    private long date;

    private String imageLink;

    private int userId;

    public POIRequest(@NonNull POIMarker element, long date, String imageLink, int userId) {
        this.element = element;
        this.date = date;
        this.imageLink = imageLink;
        this.userId = userId;
    }
}
