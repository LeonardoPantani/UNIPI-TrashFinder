package it.unipi.di.pantani.trashfinder.data;

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
@Database(entities = {Marker.class}, version = 1, exportSchema = false)
public abstract class MarkerRoomDatabase extends RoomDatabase {
    public abstract MarkerDao markerDao();

    private static volatile MarkerRoomDatabase INSTANCE;
    private static final int NUMBER_OF_THREADS = 4;
    static final ExecutorService databaseWriteExecutor =
            Executors.newFixedThreadPool(NUMBER_OF_THREADS);

    static MarkerRoomDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (MarkerRoomDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            MarkerRoomDatabase.class, "marker_database")
                            .addCallback(sRoomDatabaseCallback) // aggiunto per riempire il database
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    private static RoomDatabase.Callback sRoomDatabaseCallback = new RoomDatabase.Callback() {
        @Override
        public void onCreate(@NonNull SupportSQLiteDatabase db) {
            super.onCreate(db);

            // eseguito al primo avvio
            databaseWriteExecutor.execute(() -> {
                MarkerDao dao = INSTANCE.markerDao();
                dao.deleteAll();

                Marker test = new Marker(Marker.MarkerType.trashbin_organico, 43.72837379284841, 10.3999767226582, "Casa fede");
                dao.insert(test);
                test = new Marker(Marker.MarkerType.recyclingdepot, 43.7238295805434, 10.417441278744134, "Centro GEOFOR La Fontina");
                dao.insert(test);
                test = new Marker(Marker.MarkerType.trashbin_olio, 43.72803527270394, 10.40780072366369, "Casa abu");
                dao.insert(test);
                test = new Marker(Marker.MarkerType.trashbin_carta, 43.69250697637072, 10.480910501919539, "");
                dao.insert(test);
            });
        }
    };
}
