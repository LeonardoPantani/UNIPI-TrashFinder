/*
 * Copyright (c) 2021/2022
 * Leonardo Pantani - 598896
 * University of Pisa - Department of Computer Science
 */

package it.unipi.di.pantani.trashfinder.maps;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import it.unipi.di.pantani.trashfinder.data.requests.POIRequest;
import it.unipi.di.pantani.trashfinder.data.requests.POIRequestRepository;

public class MapEditorViewModel extends AndroidViewModel {
    private final POIRequestRepository mRepository;

    public MapEditorViewModel(@NonNull Application application) {
        super(application);
        mRepository = new POIRequestRepository(application);
    }

    public void insert(POIRequest request) {
        mRepository.insert(request);
    }

    public void delete(POIRequest request) {
        mRepository.delete(request);
    }

    public void deleteAll() {
        mRepository.deleteAll();
    }

    public LiveData<POIRequest> getById(int id) {
        return mRepository.getById(id);
    }
}