package it.unipi.di.pantani.trashfinder;

import static it.unipi.di.pantani.trashfinder.Utils.getCompassSelectedMarker;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Marker;

import trashfinder.R;

public class POIMarkerWindowAdapter implements GoogleMap.InfoWindowAdapter {
    private final View mWindow;

    @SuppressLint("InflateParams")
    public POIMarkerWindowAdapter(Context context) {
        super();
        mWindow = LayoutInflater.from(context).inflate(R.layout.marker_window, null);
    }

    private void renderInfoWindowText(Marker marker, View view) {
        // dati del marker
        String markerTitle = marker.getTitle();
        String markerSnippet = marker.getSnippet();

        // textview della infowindow
        TextView infoWindowTitle = view.findViewById(R.id.infowindow_title);
        TextView infoWindowSnippet = view.findViewById(R.id.infowindow_snippet);
        TextView infoWindowTip = view.findViewById(R.id.infowindow_selectedlement);


        // modifica titolo della infowindow
        if(markerTitle != null && !markerTitle.equals("")) {
            infoWindowTitle.setText(markerTitle.substring(markerTitle.indexOf(' ')+1));
            ((TextView)view.findViewById(R.id.infowindow_id)).setText(markerTitle.substring(0, markerTitle.indexOf(' ')));
        }

        // modifica descrizione della infowindow
        if(markerSnippet != null && !markerSnippet.equals("")) {
            view.findViewById(R.id.infowindow_section_notes).setVisibility(View.VISIBLE);
            infoWindowSnippet.setText(markerSnippet);
        }

        // modifica consiglio della infowindow
        if(marker.equals(getCompassSelectedMarker())) {
            infoWindowTip.setText(view.getResources().getString(R.string.infowindow_tipitemselected));
            infoWindowTip.setTextColor(Color.GREEN);
        } else {
            infoWindowTip.setText(view.getResources().getString(R.string.infowindow_tipclickcompass));
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
