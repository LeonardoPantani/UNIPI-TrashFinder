/*
 * Copyright (c) 2021/2022
 * Leonardo Pantani - 598896
 * University of Pisa - Department of Computer Science
 */

package it.unipi.di.pantani.trashfinder.data.requests;

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
public interface POIRequestDAO {
    @Query("SELECT * FROM poirequest_table WHERE id = :id")
    LiveData<POIRequest> getById(int id);

    @Insert
    void insert(POIRequest request);

    @Delete
    void delete(POIRequest request);

    @Query("DELETE FROM poirequest_table")
    void deleteAll();

    @Query("SELECT * FROM poirequest_table WHERE userEmail = :userEmail ORDER BY date DESC LIMIT :total OFFSET :startIndex")
    LiveData<List<POIRequest>> getRequests(String userEmail, int total, int startIndex);

    @Query("SELECT COUNT(*) FROM poirequest_table WHERE userEmail = :userEmail")
    LiveData<Integer> getUserRequestNumber(String userEmail);

    @Query("SELECT COUNT(*) FROM poirequest_table")
    LiveData<Integer> getRequestNumber();
}
