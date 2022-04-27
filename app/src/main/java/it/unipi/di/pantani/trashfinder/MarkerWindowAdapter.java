package it.unipi.di.pantani.trashfinder;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Marker;

import trashfinder.R;

public class MarkerWindowAdapter implements GoogleMap.InfoWindowAdapter{
    private final View mWindow;
    private Context mContext;

    @SuppressLint("InflateParams")
    public MarkerWindowAdapter(Context context) {
        mContext = context;
        mWindow = LayoutInflater.from(context).inflate(R.layout.marker_window, null);
    }

    private void rendowWindowText(Marker marker, View view){

        String title = marker.getTitle();
        TextView tvTitle = view.findViewById(R.id.infowindow_title);

        if(title != null && !title.equals("")){
            tvTitle.setText(title.substring(title.indexOf(' ')+1));
            TextView a = view.findViewById(R.id.infowindow_id);
            a.setText(title.substring(0, title.indexOf(' ')));
        }

        String snippet = marker.getSnippet();
        TextView tvSnippet = view.findViewById(R.id.infowindow_snippet);

        if(snippet != null && !snippet.equals("")){
            tvSnippet.setText(snippet);
        }
    }

    @Nullable
    @Override
    public View getInfoContents(@NonNull Marker marker) {
        rendowWindowText(marker, mWindow);
        return mWindow;
    }

    @Nullable
    @Override
    public View getInfoWindow(@NonNull Marker marker) {
        rendowWindowText(marker, mWindow);
        return mWindow;
    }
}
