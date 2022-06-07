/*
 * Copyright (c) 2021/2022
 * Leonardo Pantani - 598896
 * University of Pisa - Department of Computer Science
 */

package it.unipi.di.pantani.trashfinder.maps;

import static it.unipi.di.pantani.trashfinder.Utils.getCompassSelectedMarker;
import static it.unipi.di.pantani.trashfinder.Utils.getEditorSelectedMarker;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Marker;
import com.google.gson.Gson;

import java.util.Objects;

import it.unipi.di.pantani.trashfinder.R;
import it.unipi.di.pantani.trashfinder.data.marker.POIMarker;

public class POIMarkerWindowAdapter implements GoogleMap.InfoWindowAdapter {
    private final View mWindow;
    private final int mMode;
    private final Gson mGson;

    public POIMarkerWindowAdapter(Context context, int mMode) {
        super();
        this.mMode = mMode;
        mGson = new Gson();
        mWindow = View.inflate(context, R.layout.marker_window, null);
    }

    private void renderInfoWindowText(Marker marker, View view) {
        /*
            la snippet contiene l'oggetto POIMarker serializzato in JSON (non una bella soluzione, ma
            non sono riuscito ad incapsularlo nel tag perché quell'attributo non è supportato dalla
            libreria del sistema di clustering.

            "m" è il marker che contiene le informazioni dei punti di interesse visualizzati sulla mappa
         */
        String markerSnippet = marker.getSnippet();
        POIMarker m = mGson.fromJson(markerSnippet, POIMarker.class);
        if(m == null) return;

        // textview della infowindow
        TextView infoWindowTitle = view.findViewById(R.id.infowindow_title);
        TextView infoWindowId = view.findViewById(R.id.infowindow_id);
        TextView infoWindowTypes = view.findViewById(R.id.infowindow_types);
        TextView infoWindowSnippet = view.findViewById(R.id.infowindow_snippet);
        TextView infoWindowTip = view.findViewById(R.id.infowindow_selectedlement);

        infoWindowTitle.setText(marker.getTitle()); // l'unica parte che non cambia
        infoWindowId.setText(view.getResources().getString(R.string.infowindow_id, m.getId())); // id
        // tipi cestino
        StringBuilder content = new StringBuilder();
        for(POIMarker.MarkerType t : m.getTypes()) {
            if(t != POIMarker.MarkerType.recyclingdepot)
                content.append(POIMarker.getMarkerTypeName(view.getContext(), t)).append("\n");
        }
        if(content.length() != 0) {
            content.delete(content.length()-1, content.length());
            infoWindowTypes.setText(content.toString());
            infoWindowTypes.setVisibility(View.VISIBLE);
        } else {
            infoWindowTypes.setVisibility(View.GONE);
        }

        if(m.getNotes() != null && !m.getNotes().equals("")) {
            view.findViewById(R.id.infowindow_section_notes).setVisibility(View.VISIBLE);
            infoWindowSnippet.setText(m.getNotes()); // note
        } else {
            view.findViewById(R.id.infowindow_section_notes).setVisibility(View.GONE);
        }

        if(mMode == 0) { // sono in visualizzazione
            // modifica consiglio della infowindow
            if(areMarkersEqual(marker, getCompassSelectedMarker())) {
                infoWindowTip.setText(view.getResources().getString(R.string.infowindow_tipitemselected));
            } else {
                infoWindowTip.setText(view.getResources().getString(R.string.infowindow_tipclickcompass));
            }
        } else { // sono in editor
            // modifica consiglio della infowindow
            if(areMarkersEqual(marker, getEditorSelectedMarker())) {
                infoWindowTip.setText(view.getResources().getString(R.string.infowindow_tipitemselected));
            } else {
                infoWindowTip.setText(view.getResources().getString(R.string.infowindow_tipedit));
            }
        }
    }

    @Nullable
    @Override
    public View getInfoContents(@NonNull Marker marker) {
        renderInfoWindowText(marker, mWindow);
        return mWindow;
    }

    @Nullable
    @Override
    public View getInfoWindow(@NonNull Marker marker) {
        renderInfoWindowText(marker, mWindow);
        return mWindow;
    }

    public static boolean areMarkersEqual(Marker a, Marker b) {
        if(a != null && b != null) {
            return Objects.equals(a.getSnippet(), b.getSnippet());
        } else {
            return false;
        }
    }
}
