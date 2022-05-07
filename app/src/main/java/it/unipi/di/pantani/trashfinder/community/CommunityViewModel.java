package it.unipi.di.pantani.trashfinder.community;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import it.unipi.di.pantani.trashfinder.data.POIMarkerRepository;

public class CommunityViewModel extends AndroidViewModel {
    private final POIMarkerRepository mRepository;

    public CommunityViewModel(@NonNull Application application) {
        super(application);
        mRepository = new POIMarkerRepository(application);
    }

    LiveData<Integer> getMarkerNumber() {
        return mRepository.getNumberMarker();
    }
}
