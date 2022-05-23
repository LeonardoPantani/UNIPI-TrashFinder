package it.unipi.di.pantani.trashfinder.maps;

import android.content.Context;

import androidx.annotation.NonNull;

import com.google.android.gms.maps.GoogleMap;
import com.google.maps.android.clustering.Cluster;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.view.DefaultClusterRenderer;

import it.unipi.di.pantani.trashfinder.data.marker.MyItemOnMap;

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
