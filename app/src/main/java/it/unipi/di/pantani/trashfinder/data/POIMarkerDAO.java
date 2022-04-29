package it.unipi.di.pantani.trashfinder.data;

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
    @Insert
    void insert(POIMarker marker);

    @Query("SELECT * FROM marker_table WHERE id = :id")
    LiveData<POIMarker> getById(int id);

    @Delete
    void delete(POIMarker marker);

    @Query("DELETE FROM marker_table")
    void deleteAll();

    @Query("SELECT * FROM marker_table WHERE (latitude BETWEEN latitude-1 AND latitude+1) AND (longitude BETWEEN longitude-1 AND latitude+1)")
    LiveData<List<POIMarker>> getNearMarkers();
}
