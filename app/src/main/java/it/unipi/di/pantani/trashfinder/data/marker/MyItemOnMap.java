/*
 * Copyright (c) 2021/2022
 * Leonardo Pantani - 598896
 * University of Pisa - Department of Computer Science
 */

package it.unipi.di.pantani.trashfinder.data.marker;

import androidx.annotation.NonNull;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterItem;

/**
 * Rappresenta un elemento sulla mappa. E' questo l'elemento su cui lavora la libreria GoogleMapsUtils
 * che implementa il sistema di clustering. Purtroppo non permette il salvataggio di tag, a differenza
 * della versione standard del MarkerItem di Google Maps. Per questo motivo, alcuni dati extra sono
 * salvati nello snippet del marker come stringa json.
 */
public class MyItemOnMap implements ClusterItem {
    private final LatLng position;
    private final String title;
    private final String snippet;

    public MyItemOnMap(double lat, double lng, String title, String snippet) {
        position = new LatLng(lat, lng);
        this.title = title;
        this.snippet = snippet;
    }

    @NonNull
    @Override
    public LatLng getPosition() {
        return position;
    }

    public String getTitle() {
        return title;
    }

    public String getSnippet() {
        return snippet;
    }
}
