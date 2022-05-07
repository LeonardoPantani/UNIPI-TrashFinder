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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
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

                /*
                    Eseguo un task asincrono per popolare inizialmente il database con dati ottenuti
                    facendo una richiesta alle API di OpenStreetMap. Se il collegamento all'API
                    esterna non va a buon fine carico (pochi) dati di prova.
                 */
                AsyncTask.execute(() -> {
                    HttpURLConnection connection = null;
                    BufferedReader reader = null;

                    try {
                        URL url = new URL(OSM_IMPORT_STRING);
                        connection = (HttpURLConnection) url.openConnection();
                        connection.connect();

                        InputStream stream = connection.getInputStream();

                        reader = new BufferedReader(new InputStreamReader(stream));

                        StringBuilder buffer = new StringBuilder();
                        String line;

                        Log.d("ISTANZA", "DB> In attesa dei dati...");
                        long time = System.currentTimeMillis();
                        while ((line = reader.readLine()) != null) {
                            buffer.append(line).append("\n");
                        }
                        try {
                            JSONObject response = new JSONObject(buffer.toString());
                            JSONArray responseArr = response.getJSONArray("elements");

                            //for each element item in json array
                            for (int i = 0; i < responseArr.length(); i++) {
                                //convert element to string of contents
                                String elementStr = responseArr.getString(i);
                                //create individual element object from (whole) element string
                                JSONObject elementObj = new JSONObject(elementStr);

                                Set<POIMarker.MarkerType> types = new HashSet<>();
                                if(elementObj.getJSONObject("tags").get("amenity").equals("recycling")) {
                                    for (Iterator<String> it = elementObj.getJSONObject("tags").keys(); it.hasNext(); ) {
                                        String type = it.next();
                                        switch(type) {
                                            case "recycling:cans":
                                            case "recycling:aluminium_cans":
                                            case "recycling:aluminium": {
                                                types.add(POIMarker.MarkerType.trashbin_alluminio);
                                                break;
                                            }
                                            case "recycling:batteries": {
                                                types.add(POIMarker.MarkerType.trashbin_pile);
                                                break;
                                            }
                                            case "recycling:glass_bottles":
                                            case "recycling:glass": {
                                                types.add(POIMarker.MarkerType.trashbin_vetro);
                                                break;
                                            }
                                            case "recycling:beverage_cartons":
                                            case "recycling:cartons":
                                            case "recycling:paper_packaging":
                                            case "recycling:paper": {
                                                types.add(POIMarker.MarkerType.trashbin_carta);
                                                break;
                                            }
                                            case "recycling:clothes": {
                                                types.add(POIMarker.MarkerType.trashbin_vestiti);
                                                break;
                                            }
                                            case "recycling:PET":
                                            case "recycling:plastic_packaging":
                                            case "recycling:plastic_bottles":
                                            case "recycling:plastic": {
                                                types.add(POIMarker.MarkerType.trashbin_plastica);
                                                break;
                                            }
                                            case "recycling:organic":
                                            case "recycling:garden_waste":
                                            case "recycling:green_waste": {
                                                types.add(POIMarker.MarkerType.trashbin_organico);
                                                break;
                                            }
                                            case "recycling:drugs": {
                                                types.add(POIMarker.MarkerType.trashbin_farmaci);
                                                break;
                                            }
                                            case "recycling:waste_oil":
                                            case "recycling:engine_oil":
                                            case "recycling:cooking_oil":
                                            case "recycling:oil": {
                                                types.add(POIMarker.MarkerType.trashbin_olio);
                                                break;
                                            }
                                            case "recycling:waste":
                                            default: {
                                                types.add(POIMarker.MarkerType.trashbin_indifferenziato);
                                                break;
                                            }
                                        }
                                    }
                                } else if(elementObj.getJSONObject("tags").get("amenity").equals("waste_transfer_station")) {
                                    types.add(POIMarker.MarkerType.recyclingdepot);
                                } else {
                                    types.add(POIMarker.MarkerType.trashbin_indifferenziato);
                                }
                                dao.insert(new POIMarker(types, elementObj.getDouble("lat"), elementObj.getDouble("lon"), ""));
                            }
                        } catch (JSONException e) { // errore json
                            e.printStackTrace();
                            manualAdd();
                        }

                        Log.d("ISTANZA", "DB> Dati salvati e lista dei POI pronta (" + (System.currentTimeMillis()-time) + "ms).");
                    } catch (IOException e) { // errore input output (connessione di solito)
                        e.printStackTrace();
                        manualAdd();
                    } finally {
                        if (connection != null) {
                            connection.disconnect();
                        }
                        try {
                            if (reader != null) {
                                reader.close();
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });
            });
        }
    };

    private static void manualAdd() {
        Set<POIMarker.MarkerType> types = new HashSet<>();

        // eseguito al primo avvio
        POIMarkerDAO dao = INSTANCE.markerDao();
        dao.deleteAll();

        types.add(POIMarker.MarkerType.recyclingdepot);
        dao.insert(new POIMarker(types, 43.72363829574406, 10.417132187214595, "Centro GEOFOR"));
    }
}
