package it.unipi.di.pantani.trashfinder.data;

import android.app.Application;

import androidx.lifecycle.LiveData;

import java.util.List;

/**
 * A Repository class abstracts access to multiple data sources.
 * The Repository is not part of the Architecture Components libraries, but is a suggested best practice for code separation and architecture.
 * A Repository class provides a clean API for data access to the rest of the application.

 * A Repository manages queries and allows you to use multiple backends.
 * In the most common example, the Repository implements the logic for deciding whether to fetch data from a network or use results cached in a local database.
 */
public class POIMarkerRepository {
    private POIMarkerDAO mPOIMarkerDAO;
    private LiveData<List<POIMarker>> mMarkerList;

    public POIMarkerRepository(Application application) {
        POIMarkerRoomDatabase db = POIMarkerRoomDatabase.getDatabase(application);
        mPOIMarkerDAO = db.markerDao();
        mMarkerList = mPOIMarkerDAO.getNearMarkers();
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
}
