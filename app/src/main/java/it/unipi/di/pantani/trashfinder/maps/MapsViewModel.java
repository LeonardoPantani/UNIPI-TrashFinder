package it.unipi.di.pantani.trashfinder.maps;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import java.util.List;

import it.unipi.di.pantani.trashfinder.data.POIMarker;
import it.unipi.di.pantani.trashfinder.data.POIMarkerRepository;

public class MapsViewModel extends AndroidViewModel {
    private final POIMarkerRepository mRepository;

    private final LiveData<List<POIMarker>> mNearMarkers;

    public MapsViewModel(@NonNull Application application) {
        super(application);
        mRepository = new POIMarkerRepository(application);
        mNearMarkers = mRepository.getMarkerList();
    }

    LiveData<List<POIMarker>> getNearMarkers() {
        return mNearMarkers;
    }
}
