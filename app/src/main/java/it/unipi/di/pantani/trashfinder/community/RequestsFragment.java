/*
 * Copyright (c) 2021/2022
 * Leonardo Pantani - 598896
 * University of Pisa - Department of Computer Science
 */

package it.unipi.di.pantani.trashfinder.community;

import static it.unipi.di.pantani.trashfinder.Utils.NUMBER_REQUESTS_LIST;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import it.unipi.di.pantani.trashfinder.Utils;
import it.unipi.di.pantani.trashfinder.databinding.FragmentRequestsBinding;

/**
 * Frammento che mostra una lista di oggetti di tipo POIRequest.
 */
public class RequestsFragment extends Fragment {
    private FragmentRequestsBinding mBinding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        mBinding = FragmentRequestsBinding.inflate(inflater, container, false);
        View root = mBinding.getRoot();

        Context context = root.getContext();
        RecyclerView recyclerView = mBinding.requestsRv;
        RequestsRVAdapter adapter = new RequestsRVAdapter(context, recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        recyclerView.setAdapter(adapter);

        RequestsViewModel requestsViewModel = new ViewModelProvider(this).get(RequestsViewModel.class);

        // mostro dati
        requestsViewModel.getRequests(Utils.getCurrentUserAccount().getEmail(), NUMBER_REQUESTS_LIST, 0).observe(getViewLifecycleOwner(), requestsList -> {
            if(requestsList != null && requestsList.size() != 0) {
                mBinding.requestsRv.setVisibility(View.VISIBLE);
                mBinding.requestsNothing.setVisibility(View.GONE);

                adapter.updateList(requestsList);
            }
        });

        return root;
    }
}