package it.unipi.di.pantani.trashfinder.services;

import static it.unipi.di.pantani.trashfinder.Utils.API_IMPORT_STRING;
import static it.unipi.di.pantani.trashfinder.Utils.OSM_IMPORT_STRING;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

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
import java.util.concurrent.Executors;

import it.unipi.di.pantani.trashfinder.R;
import it.unipi.di.pantani.trashfinder.Utils;
import it.unipi.di.pantani.trashfinder.data.marker.POIMarker;
import it.unipi.di.pantani.trashfinder.data.marker.POIMarkerDAO;
import it.unipi.di.pantani.trashfinder.data.marker.POIMarkerRoomDatabase;

public class DownloadService extends Service {
    private Context mContext;

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = this;
        downloadFile();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_NOT_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void downloadFile() {
        POIMarkerRoomDatabase db = POIMarkerRoomDatabase.getDatabase(mContext);
        POIMarkerDAO dao = db.markerDao();

        /*
            Eseguo un task asincrono per popolare inizialmente il database con dati ottenuti
            facendo una richiesta alle API di OpenStreetMap. Se il collegamento all'API
            esterna non va a buon fine carico (pochi) dati di prova.
         */
        Executors.newSingleThreadExecutor().execute(() -> {
            createNotificationChannel();

            dao.deleteAll();
            Log.d("ISTANZA", "DB> Inizio procedura di importazione dei dati sui cestini...");

            long time = System.currentTimeMillis();
            int amount = 0;

            String buffer;
            sendNotificationStart("Drive");
            buffer = downloadData(API_IMPORT_STRING);
            if (buffer == null) {
                sendNotificationStart("OSM");
                buffer = downloadData(OSM_IMPORT_STRING);
            }
            if (buffer == null) {
                sendNotificationManual();
                amount = manualAdd(dao);
                Log.d("ISTANZA", "DB> Dati salvati e lista dei " + amount + " POI pronta (" + (System.currentTimeMillis() - time) + "ms).");
                return;
            }

            try {
                JSONObject response = new JSONObject(buffer);
                JSONArray responseArr = response.getJSONArray("elements");

                for (int i = 0; i < responseArr.length(); i++) {
                    String elementStr = responseArr.getString(i);
                    JSONObject elementObj = new JSONObject(elementStr);

                    Set<POIMarker.MarkerType> types = new HashSet<>();
                    if (elementObj.getJSONObject("tags").get("amenity").equals("recycling")) {
                        for (Iterator<String> it = elementObj.getJSONObject("tags").keys(); it.hasNext(); ) {
                            String type = it.next();
                            switch (type) {
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
                    } else if (elementObj.getJSONObject("tags").get("amenity").equals("waste_transfer_station")) {
                        types.add(POIMarker.MarkerType.recyclingdepot);
                    } else {
                        types.add(POIMarker.MarkerType.trashbin_indifferenziato);
                    }
                    dao.insert(new POIMarker(types, elementObj.getDouble("lat"), elementObj.getDouble("lon"), ""));
                    amount++;
                }
                Utils.setPreference(mContext,"finished_import", true);
                sendNotificationCompleted();
            } catch (JSONException e) { // errore json
                e.printStackTrace();
                amount = 0;
                amount = manualAdd(dao);
                sendNotificationManual();
            } finally {
                Log.d("ISTANZA", "DB> Dati salvati e lista dei " + amount + " POI pronta (" + (System.currentTimeMillis() - time) + "ms).");
            }
        });
    }

    private String downloadData(final String inputUrl) {
        String ret = null;

        HttpURLConnection connection = null;
        BufferedReader reader = null;

        try {
            Log.d("ISTANZA", "DB> URL recupero: '" + inputUrl + "'.");
            URL url = new URL(API_IMPORT_STRING);
            connection = (HttpURLConnection) url.openConnection();
            HttpURLConnection.setFollowRedirects(false);
            connection.setConnectTimeout(5 * 1000);
            connection.connect();
            Log.d("ISTANZA", "DB> Connesso a '" + inputUrl + "'.");

            InputStream stream = connection.getInputStream();

            reader = new BufferedReader(new InputStreamReader(stream));

            StringBuilder buffer = new StringBuilder();
            String line;

            Log.d("ISTANZA", "DB> Dati recuperati da '" + inputUrl + "'. Elaborazione...");
            long time = System.currentTimeMillis();
            while ((line = reader.readLine()) != null) {
                buffer.append(line).append("\n");
            }

            return buffer.toString();
        } catch (IOException e) { // errore input output (connessione probabilmente)
            Log.d("ISTANZA", "DB> Errore durante il recupero dei dati da '" + inputUrl + "'.");
            e.printStackTrace();
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

        sendNotificationFail();
        return null;
    }

    @SuppressWarnings("SameReturnValue")
    private int manualAdd(POIMarkerDAO dao) {
        dao.deleteAll();

        dao.insert(new POIMarker(Set.of(POIMarker.MarkerType.recyclingdepot), 43.72363829574406, 10.417132187214595, "Centro GEOFOR"));
        dao.insert(new POIMarker(Set.of(POIMarker.MarkerType.trashbin_indifferenziato, POIMarker.MarkerType.trashbin_plastica, POIMarker.MarkerType.trashbin_carta), 43.721768389743055, 10.408047122841685, ""));
        dao.insert(new POIMarker(Set.of(POIMarker.MarkerType.trashbin_indifferenziato), 43.72276671037211, 10.436552726467875, ""));

        Utils.setPreference(mContext,"finished_import", true);

        return 3;
    }

    // NOTIFICHE
    /**
     * Creazione notification channel. E' mostrato anche nelle impostazioni dell'app nella sezione "notifiche".
     */
    private void createNotificationChannel() {
        CharSequence name = getString(R.string.notificationchannel_downloads_title);
        String description = getString(R.string.notificationchannel_downloads_desc);
        int importance = NotificationManager.IMPORTANCE_LOW;
        NotificationChannel channel = new NotificationChannel("downloadservice", name, importance);
        channel.setDescription(description);
        // registra il notification channel. Non si potrà più cambiare la priorità.
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);
    }

    /**
     * Mostra una notifica di download con il nome della fonte.
     * @param inputSrc nome della fonte del download (due al momento: drive & osm)
     */
    private void sendNotificationStart(String inputSrc) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "downloadservice")
                .setSmallIcon(R.drawable.ic_baseline_downloading_24)
                .setContentTitle(getResources().getString(R.string.notification_downloadstarted_title))
                .setContentText(getResources().getString(R.string.notification_downloadstarted_desc, inputSrc))
                .setPriority(NotificationCompat.PRIORITY_LOW);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.notify(1, builder.build());
    }

    /**
     * Mostra una notifica di download fallito.
     */
    private void sendNotificationFail() {
        cancelNotification(1);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "downloadservice")
                .setSmallIcon(R.drawable.ic_baseline_running_with_errors_24)
                .setContentTitle(getResources().getString(R.string.notification_downloadfailed_title))
                .setContentText(getResources().getString(R.string.notification_downloadfailed_desc))
                .setPriority(NotificationCompat.PRIORITY_LOW);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.notify(2, builder.build());
    }

    /**
     * Mostra una notifica di download completato.
     */
    private void sendNotificationCompleted() {
        cancelNotification(1);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "downloadservice")
                .setSmallIcon(R.drawable.ic_baseline_celebration_24)
                .setContentTitle(getResources().getString(R.string.notification_downloadcompleted_title))
                .setContentText(getResources().getString(R.string.notification_downloadcompleted_desc))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.notify(3, builder.build());
    }

    /**
     * Mostra una notifica di download fallito. Questa notifica avvisa l'utente che non si è riusciti
     * a scaricare i dati sui cestini da nessuna fonte e che quindi si utilizzerà dei dati di default.
     */
    private void sendNotificationManual() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "downloadservice")
                .setSmallIcon(R.drawable.ic_baseline_info_24)
                .setContentTitle(getResources().getString(R.string.notification_downloadmanual_title))
                .setContentText(getResources().getString(R.string.notification_downloadmanual_desc))
                .setPriority(NotificationCompat.PRIORITY_LOW);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.notify(4, builder.build());
    }

    /**
     * Elimina una notifica.
     * @param notifyId id della notifica da eliminare
     */
    public void cancelNotification(int notifyId) {
        NotificationManager nMgr = (NotificationManager) mContext.getSystemService(NOTIFICATION_SERVICE);
        nMgr.cancel(notifyId);
    }
}