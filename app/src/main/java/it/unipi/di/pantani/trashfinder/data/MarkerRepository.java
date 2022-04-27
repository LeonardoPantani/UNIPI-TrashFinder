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
public class MarkerRepository {
    private MarkerDao mMarkerDao;
    private LiveData<List<Marker>> mMarkerList;

    public MarkerRepository(Application application) {
        MarkerRoomDatabase db = MarkerRoomDatabase.getDatabase(application);
        mMarkerDao = db.markerDao();
        mMarkerList = mMarkerDao.getNearMarkers();
    }

    public LiveData<List<Marker>> getMarkerList() {
        return mMarkerList;
    }

    public LiveData<Marker> getById(int id) {
        return mMarkerDao.getById(id);
    }

    public void insert(Marker marker) {
        MarkerRoomDatabase.databaseWriteExecutor.execute(() -> mMarkerDao.insert(marker));
    }
}
