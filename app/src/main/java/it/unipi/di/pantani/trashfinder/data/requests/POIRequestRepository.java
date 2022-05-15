package it.unipi.di.pantani.trashfinder.data.requests;

import android.app.Application;
import android.database.Cursor;

import androidx.lifecycle.LiveData;

/**
 * A Repository class abstracts access to multiple data sources.
 * The Repository is not part of the Architecture Components libraries, but is a suggested best practice for code separation and architecture.
 * A Repository class provides a clean API for data access to the rest of the application.

 * A Repository manages queries and allows you to use multiple backends.
 * In the most common example, the Repository implements the logic for deciding whether to fetch data from a network or use results cached in a local database.
 */
public class POIRequestRepository {
    private final POIRequestDAO mPOIRequestDAO;
    private final Cursor mRequestCursor;

    public POIRequestRepository(Application application) {
        POIRequestRoomDatabase db = POIRequestRoomDatabase.getDatabase(application);
        mPOIRequestDAO = db.requestDAO();
        mRequestCursor = mPOIRequestDAO.getRequests();
    }

    LiveData<POIRequest> getById(int id) {
        return mPOIRequestDAO.getById(id);
    }

    void insert(POIRequest request) {
        POIRequestRoomDatabase.databaseWriteExecutor.execute(() -> mPOIRequestDAO.insert(request));
    }

    void delete(POIRequest request) {
        POIRequestRoomDatabase.databaseWriteExecutor.execute(() -> mPOIRequestDAO.delete(request));
    }

    void deleteAll() {
        POIRequestRoomDatabase.databaseWriteExecutor.execute(mPOIRequestDAO::deleteAll);
    }

    Cursor getRequests() {
        return mRequestCursor;
    }
}
