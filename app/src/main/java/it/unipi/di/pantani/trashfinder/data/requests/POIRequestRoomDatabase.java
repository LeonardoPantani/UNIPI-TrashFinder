package it.unipi.di.pantani.trashfinder.data.requests;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 Room is a database layer on top of an SQLite database.
 Room takes care of mundane tasks that you used to handle with an SQLiteOpenHelper.
 Room uses the DAO to issue queries to its database.
 By default, to avoid poor UI performance, Room doesn't allow you to issue queries on the main thread.
 When Room queries return LiveData, the queries are automatically run asynchronously on a background thread.
 Room provides compile-time checks of SQLite statements.
 */
@Database(entities = {POIRequest.class}, version = 1, exportSchema = false)
public abstract class POIRequestRoomDatabase extends RoomDatabase {
    public abstract POIRequestDAO requestDAO();

    private static volatile POIRequestRoomDatabase INSTANCE;
    private static final int NUMBER_OF_THREADS = 4;
    static final ExecutorService databaseWriteExecutor = Executors.newFixedThreadPool(NUMBER_OF_THREADS);

    static POIRequestRoomDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (POIRequestRoomDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    POIRequestRoomDatabase.class, "request_database")
                            .addCallback(sRoomDatabaseCallback) // aggiunto per riempire il database
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    private static final RoomDatabase.Callback sRoomDatabaseCallback = new RoomDatabase.Callback() {
        @Override
        public void onCreate(@NonNull SupportSQLiteDatabase db) {
            super.onCreate(db);

            // eseguito al primo avvio
            databaseWriteExecutor.execute(() -> {
                POIRequestDAO dao = INSTANCE.requestDAO();
                dao.deleteAll();


                Executors.newSingleThreadExecutor().execute(() -> {
                    // TODO
                });
            });
        }
    };
}
