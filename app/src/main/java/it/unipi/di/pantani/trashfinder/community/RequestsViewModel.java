/*
 * Copyright (c) 2021/2022
 * Leonardo Pantani - 598896
 * University of Pisa - Department of Computer Science
 */

package it.unipi.di.pantani.trashfinder.community;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import java.util.List;

import it.unipi.di.pantani.trashfinder.data.requests.POIRequest;
import it.unipi.di.pantani.trashfinder.data.requests.POIRequestRepository;

public class RequestsViewModel extends AndroidViewModel {
    private final POIRequestRepository mRepository;
    
    public RequestsViewModel(@NonNull Application application) {
        super(application);
        mRepository = new POIRequestRepository(application);
    }

    public LiveData<List<POIRequest>> getRequests(String userEmail, int total, int startIndex) {
        return mRepository.getRequests(userEmail, total, startIndex);
    }
}
