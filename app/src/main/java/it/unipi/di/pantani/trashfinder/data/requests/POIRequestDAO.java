package it.unipi.di.pantani.trashfinder.data.requests;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

@Dao
public interface POIRequestDAO {
    @Query("SELECT * FROM requests_table WHERE id = :id")
    LiveData<POIRequest> getById(int id);

    @Insert
    void insert(POIRequest request);

    @Delete
    void delete(POIRequest request);
}
