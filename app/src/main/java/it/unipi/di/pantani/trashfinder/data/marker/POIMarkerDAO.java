package it.unipi.di.pantani.trashfinder.data.marker;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

/**
 * A DAO (data access object) validates your SQL at compile-time and associates it with a method.
 * In your Room DAO, you use handy annotations, like @Insert, to represent the most common database operations!
 * Room uses the DAO to create a clean API for your code.
 */
@Dao
public interface POIMarkerDAO {
    @Query("SELECT * FROM marker_table WHERE id = :id")
    LiveData<POIMarker> getById(int id);

    @Insert
    void insert(POIMarker marker);

    @Delete
    void delete(POIMarker marker);

    @Query("DELETE FROM marker_table")
    void deleteAll();

    @Query("SELECT * FROM marker_table")
    LiveData<List<POIMarker>> getMarkers();

    @Query("SELECT COUNT(*) FROM marker_table")
    LiveData<Integer> getMarkerNumber();

    @Query("UPDATE marker_table SET types = :newTypes, latitude = :newLatitude, longitude = :newLongitude, notes = :newNotes WHERE id = :id")
    void update(int id, String newTypes, double newLatitude, double newLongitude, String newNotes);
}
