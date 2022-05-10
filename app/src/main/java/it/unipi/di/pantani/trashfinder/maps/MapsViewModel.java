package it.unipi.di.pantani.trashfinder.maps;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import java.util.List;
import java.util.Set;

import it.unipi.di.pantani.trashfinder.data.marker.POIMarker;
import it.unipi.di.pantani.trashfinder.data.marker.POIMarkerRepository;

public class MapsViewModel extends AndroidViewModel {
    private final POIMarkerRepository mRepository;
    private final LiveData<List<POIMarker>> mNearMarkers;

    public MapsViewModel(@NonNull Application application) {
        super(application);
        mRepository = new POIMarkerRepository(application);
        mNearMarkers = mRepository.getMarkerList();
    }

    public LiveData<List<POIMarker>> getNearMarkers() {
        return mNearMarkers;
    }

    public void delete(POIMarker marker) {
        mRepository.delete(marker);
    }

    public void update(int id, Set<POIMarker.MarkerType> newTypes, double newLatitude, double newLongitude, String newNotes) {
        mRepository.update(id, newTypes, newLatitude, newLongitude, newNotes);
    }

    public void insert(POIMarker marker) {
        mRepository.insert(marker);
    }
}
