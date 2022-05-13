package it.unipi.di.pantani.trashfinder.maps;

import android.content.Context;

import androidx.annotation.NonNull;

import com.google.android.gms.maps.GoogleMap;
import com.google.maps.android.clustering.Cluster;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.view.DefaultClusterRenderer;

import it.unipi.di.pantani.trashfinder.data.marker.MyItemOnMap;

public class CustomClusterRenderer extends DefaultClusterRenderer<MyItemOnMap> implements GoogleMap.OnCameraMoveListener {
    private final GoogleMap mMap;
    private float currentZoomLevel;
    private final float maxZoomLevel;

    public CustomClusterRenderer(Context context, GoogleMap map, ClusterManager<MyItemOnMap> clusterManager, float currentZoomLevel, float maxZoomLevel) {
        super(context, map, clusterManager);

        this.mMap = map;
        this.currentZoomLevel = currentZoomLevel;
        this.maxZoomLevel = maxZoomLevel;
    }

    @Override
    public void onCameraMove() {
        currentZoomLevel = mMap.getCameraPosition().zoom;
    }

    @Override
    protected boolean shouldRenderAsCluster(@NonNull Cluster<MyItemOnMap> cluster) {
        return super.shouldRenderAsCluster(cluster) && currentZoomLevel < maxZoomLevel;
    }
}
