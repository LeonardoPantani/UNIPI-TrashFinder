/*
 * Copyright (c) 2021/2022
 * Leonardo Pantani - 598896
 * University of Pisa - Department of Computer Science
 */

package it.unipi.di.pantani.trashfinder.maps;

import android.content.Context;

import androidx.annotation.NonNull;

import com.google.android.gms.maps.GoogleMap;
import com.google.maps.android.clustering.Cluster;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.view.DefaultClusterRenderer;

import it.unipi.di.pantani.trashfinder.data.marker.MyItemOnMap;

/**
 * Definendo un renderer personalizzato, è possibile creare un callback chiamato allo zoom.
 *
 * Il motivo principale di questo renderer (usato solo nel fragment "MapEditor") è fare il clustering
 * degli oggetti solo ad un determinato livello di zoom. In questo modo, anche se vengono messi dei
 * cestini troppo vicini, si è sicuri di vederli sempre separati e non uniti in un cluster.
 *
 * Ricordarsi: questo vale solo in modalità modifica.
 */
public class CustomClusterRenderer extends DefaultClusterRenderer<MyItemOnMap> implements GoogleMap.OnCameraMoveListener {
    public interface MapZoomChangeListener {
        void onNewZoom(float newZoom);
    }

    private final GoogleMap mMap;
    private float mCurrentZoomLevel;
    private final float mMaxZoomLevel;
    private MapZoomChangeListener mListener;

    public CustomClusterRenderer(Context context, GoogleMap map, ClusterManager<MyItemOnMap> clusterManager, float mCurrentZoomLevel, float mMaxZoomLevel) {
        super(context, map, clusterManager);

        this.mMap = map;
        this.mCurrentZoomLevel = mCurrentZoomLevel;
        this.mMaxZoomLevel = mMaxZoomLevel;
    }

    public void setZoomChangeListener(MapZoomChangeListener l) {
        mListener = l;
        mListener.onNewZoom(mCurrentZoomLevel);
    }

    public void unsetZoomChangeListener() {
        mListener = null;
    }

    @Override
    public void onCameraMove() {
        float newZoom = mMap.getCameraPosition().zoom;

        if(mListener != null && mCurrentZoomLevel != newZoom) {
            mListener.onNewZoom(newZoom);
        }

        mCurrentZoomLevel = newZoom;
    }

    @Override
    protected boolean shouldRenderAsCluster(@NonNull Cluster<MyItemOnMap> cluster) {
        return super.shouldRenderAsCluster(cluster) && mCurrentZoomLevel < mMaxZoomLevel;
    }
}
