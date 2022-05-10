package it.unipi.di.pantani.trashfinder.compass;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import it.unipi.di.pantani.trashfinder.data.marker.POIMarker;
import it.unipi.di.pantani.trashfinder.data.marker.POIMarkerRepository;

public class CompassViewModel extends AndroidViewModel {
    private final POIMarkerRepository mRepository;

    public CompassViewModel(@NonNull Application application) {
        super(application);
        mRepository = new POIMarkerRepository(application);
    }

    LiveData<POIMarker> getMarkerId(int id) {
        return mRepository.getById(id);
    }
}
