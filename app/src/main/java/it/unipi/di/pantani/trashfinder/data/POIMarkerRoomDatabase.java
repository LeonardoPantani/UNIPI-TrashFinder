package it.unipi.di.pantani.trashfinder.data;

import static it.unipi.di.pantani.trashfinder.Utils.OSM_IMPORT_STRING;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import java.util.List;
import java.util.concurrent.ExecutionException;
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
@Database(entities = {POIMarker.class}, version = 1, exportSchema = false)
public abstract class POIMarkerRoomDatabase extends RoomDatabase {
    public abstract POIMarkerDAO markerDao();

    private static POIMarkerRoomDatabase INSTANCE;
    private static final int NUMBER_OF_THREADS = 4;
    static final ExecutorService databaseWriteExecutor = Executors.newFixedThreadPool(NUMBER_OF_THREADS);

    static POIMarkerRoomDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (POIMarkerRoomDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            POIMarkerRoomDatabase.class, "marker_database")
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
                POIMarkerDAO dao = INSTANCE.markerDao();
                dao.deleteAll();

                AsyncTask<String,String, List<POIMarker>> a = new MarkerImport().execute(OSM_IMPORT_STRING);
                try {
                    List<POIMarker> l = a.get();
                    Log.d("ISTANZA", "applicazione dati al database...");
                    for(POIMarker p : l) {
                        dao.insert(p);
                    }
                } catch (ExecutionException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
        }
    };
}
