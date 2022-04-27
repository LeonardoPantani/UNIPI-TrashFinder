package it.unipi.di.pantani.trashfinder.home;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import java.util.List;

import it.unipi.di.pantani.trashfinder.data.Marker;
import it.unipi.di.pantani.trashfinder.data.MarkerRepository;

public class MapsViewModel extends AndroidViewModel {
    private MarkerRepository mRepository;

    private final LiveData<List<Marker>> mNearMarkers;

    public MapsViewModel(@NonNull Application application) {
        super(application);
        mRepository = new MarkerRepository(application);
        mNearMarkers = mRepository.getMarkerList();
    }

    LiveData<List<Marker>> getNearMarkers() {
        return mNearMarkers;
    }
}
