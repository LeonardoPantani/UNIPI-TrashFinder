package it.unipi.di.pantani.trashfinder.maps;

import static it.unipi.di.pantani.trashfinder.Utils.getCompassSelectedMarker;
import static it.unipi.di.pantani.trashfinder.Utils.getEditorSelectedMarker;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Marker;
import com.google.gson.Gson;

import it.unipi.di.pantani.trashfinder.R;
import it.unipi.di.pantani.trashfinder.data.marker.POIMarker;

public class POIMarkerWindowAdapter implements GoogleMap.InfoWindowAdapter {
    private final View mWindow;
    private final int mode;

    @SuppressLint("InflateParams")
    public POIMarkerWindowAdapter(Context context, int mode) {
        super();
        this.mode = mode;
        mWindow = LayoutInflater.from(context).inflate(R.layout.marker_window, null);
    }

    private void renderInfoWindowText(Marker marker, View view) {
        /*
            la snippet contiene l'oggetto POIMarker serializzato in JSON (non una bella soluzione, ma
            non sono riuscito ad incapsularlo nel tag perché quell'attributo non è supportato dalla
            libreria del sistema di clustering.

            "m" è il marker che contiene le informazioni dei punti di interesse visualizzati sulla mappa
         */
        String markerSnippet = marker.getSnippet();
        POIMarker m = new Gson().fromJson(markerSnippet, POIMarker.class);

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

        if(mode == 0) {
            // modifica consiglio della infowindow
            if(marker.equals(getCompassSelectedMarker())) {
                infoWindowTip.setText(view.getResources().getString(R.string.infowindow_tipitemselected));
            } else {
                infoWindowTip.setText(view.getResources().getString(R.string.infowindow_tipclickcompass));
            }
        } else {
            // modifica consiglio della infowindow
            if(marker.equals(getEditorSelectedMarker())) {
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
}
