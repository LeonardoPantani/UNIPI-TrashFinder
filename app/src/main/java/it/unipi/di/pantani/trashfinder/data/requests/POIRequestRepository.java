package it.unipi.di.pantani.trashfinder.data.requests;

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
public class POIRequestRepository {
    private final POIRequestDAO mPOIRequestDAO;

    public POIRequestRepository(Application application) {
        POIRequestRoomDatabase db = POIRequestRoomDatabase.getDatabase(application);
        mPOIRequestDAO = db.requestDAO();
    }

    public LiveData<POIRequest> getById(int id) {
        return mPOIRequestDAO.getById(id);
    }

    public void insert(POIRequest request) {
        POIRequestRoomDatabase.databaseWriteExecutor.execute(() -> mPOIRequestDAO.insert(request));
    }

    public void delete(POIRequest request) {
        POIRequestRoomDatabase.databaseWriteExecutor.execute(() -> mPOIRequestDAO.delete(request));
    }

    public void deleteAll() {
        POIRequestRoomDatabase.databaseWriteExecutor.execute(mPOIRequestDAO::deleteAll);
    }

    public LiveData<List<POIRequest>> getRequests(String userEmail, int total, int startIndex) {
        return mPOIRequestDAO.getRequests(userEmail, total, startIndex);
    }

    public LiveData<Integer> getUserRequestNumber(String userEmail) {
        return mPOIRequestDAO.getUserRequestNumber(userEmail);
    }

    public LiveData<Integer> getRequestNumber() {
        return mPOIRequestDAO.getRequestNumber();
    }
}
