package it.unipi.di.pantani.trashfinder.data.requests;

import android.database.Cursor;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

@Dao
public interface POIRequestDAO {
    @Query("SELECT * FROM poirequest_table WHERE id = :id")
    LiveData<POIRequest> getById(int id);

    @Insert
    void insert(POIRequest request);

    @Delete
    void delete(POIRequest request);

    @Query("DELETE FROM poirequest_table")
    void deleteAll();

    @Query("SELECT * FROM poirequest_table")
    Cursor getRequests();
}
