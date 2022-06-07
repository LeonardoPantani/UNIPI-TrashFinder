/*
 * Copyright (c) 2021/2022
 * Leonardo Pantani - 598896
 * University of Pisa - Department of Computer Science
 */

package it.unipi.di.pantani.trashfinder.data.marker;

import android.app.Application;

import androidx.lifecycle.LiveData;

import java.util.List;
import java.util.Set;

/**
 * A Repository class abstracts access to multiple data sources.
 * The Repository is not part of the Architecture Components libraries, but is a suggested best practice for code separation and architecture.
 * A Repository class provides a clean API for data access to the rest of the application.

 * A Repository manages queries and allows you to use multiple backends.
 * In the most common example, the Repository implements the logic for deciding whether to fetch data from a network or use results cached in a local database.
 */
public class POIMarkerRepository {
    private final POIMarkerDAO mPOIMarkerDAO;
    private final LiveData<List<POIMarker>> mMarkerList;
    private final LiveData<Integer> mMarkersNumber;

    public POIMarkerRepository(Application application) {
        POIMarkerRoomDatabase db = POIMarkerRoomDatabase.getDatabase(application);
        mPOIMarkerDAO = db.markerDao();
        mMarkerList = mPOIMarkerDAO.getMarkers();
        mMarkersNumber = mPOIMarkerDAO.getMarkerNumber();
    }

    public LiveData<List<POIMarker>> getMarkerList() {
        return mMarkerList;
    }

    public LiveData<POIMarker> getById(int id) {
        return mPOIMarkerDAO.getById(id);
    }

    public void insert(POIMarker marker) {
        POIMarkerRoomDatabase.databaseWriteExecutor.execute(() -> mPOIMarkerDAO.insert(marker));
    }

    public void delete(POIMarker marker) {
        POIMarkerRoomDatabase.databaseWriteExecutor.execute(() -> mPOIMarkerDAO.delete(marker));
    }

    public void update(int id, Set<POIMarker.MarkerType> newTypes, double newLatitude, double newLongitude, String newNotes) {
        POIMarkerRoomDatabase.databaseWriteExecutor.execute(() -> mPOIMarkerDAO.update(id, POIMarkerConverter.markersToString(newTypes), newLatitude, newLongitude, newNotes));
    }

    public LiveData<Integer> getNumberMarker() {
        return mMarkersNumber;
    }
}
