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

import it.unipi.di.pantani.trashfinder.data.marker.POIMarkerRepository;
import it.unipi.di.pantani.trashfinder.data.requests.POIRequestRepository;

public class CommunityViewModel extends AndroidViewModel {
    private final POIMarkerRepository mMarkersRepository;
    private final POIRequestRepository mRequestsRepository;

    public CommunityViewModel(@NonNull Application application) {
        super(application);
        mMarkersRepository = new POIMarkerRepository(application);
        mRequestsRepository = new POIRequestRepository(application);
    }

    LiveData<Integer> getMarkerNumber() {
        return mMarkersRepository.getNumberMarker();
    }

    LiveData<Integer> getUserRequestNumber(String userEmail) {
        return mRequestsRepository.getUserRequestNumber(userEmail);
    }
}
