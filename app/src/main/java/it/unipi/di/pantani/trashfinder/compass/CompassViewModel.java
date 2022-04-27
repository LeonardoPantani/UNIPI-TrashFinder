package it.unipi.di.pantani.trashfinder.compass;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import it.unipi.di.pantani.trashfinder.data.Marker;
import it.unipi.di.pantani.trashfinder.data.MarkerRepository;

public class CompassViewModel extends AndroidViewModel {
    private MarkerRepository mRepository;

    public CompassViewModel(@NonNull Application application) {
        super(application);
        mRepository = new MarkerRepository(application);
    }

    LiveData<Marker> getMarkerId(int id) {
        return mRepository.getById(id);
    }
}
