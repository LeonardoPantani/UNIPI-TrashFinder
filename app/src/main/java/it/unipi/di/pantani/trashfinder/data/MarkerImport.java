package it.unipi.di.pantani.trashfinder.data;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class MarkerImport extends AsyncTask<String, String, List<POIMarker>> {
    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    protected List<POIMarker> doInBackground(String... params) {
        HttpURLConnection connection = null;
        BufferedReader reader = null;

        List<POIMarker> res = new ArrayList<>();
        try {
            URL url = new URL(params[0]);
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
                                case "recyling:green_waste": {
                                    types.add(POIMarker.MarkerType.trashbin_organico);
                                    break;
                                }
                                case "recycling:waste":
                                default: {  // decidere se tenerlo così o no
                                    types.add(POIMarker.MarkerType.trashbin_indifferenziato);
                                    break;
                                }
                            }
                        }
                    } else {
                        types.add(POIMarker.MarkerType.trashbin_indifferenziato);
                    }

                    res.add(new POIMarker(types, elementObj.getDouble("lat"), elementObj.getDouble("lon"), ""));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

            Log.d("ISTANZA", "DB> Dati salvati e lista dei POI pronta (" + (System.currentTimeMillis()-time) + "ms).");
            return res;


        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
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
        return null;
    }

    @Override
    protected void onPostExecute(List<POIMarker> result) {
        super.onPostExecute(result);
    }
}