/*
 * Copyright (c) 2021/2022
 * Leonardo Pantani - 598896
 * University of Pisa - Department of Computer Science
 */

package it.unipi.di.pantani.trashfinder.community;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import java.util.Random;

import it.unipi.di.pantani.trashfinder.R;
import it.unipi.di.pantani.trashfinder.Utils;
import it.unipi.di.pantani.trashfinder.databinding.FragmentCommunityBinding;

public class CommunityFragment extends Fragment {
    private FragmentCommunityBinding mBinding;
    private CommunityViewModel mCommunityViewModel;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        mBinding = FragmentCommunityBinding.inflate(inflater, container, false);
        View root = mBinding.getRoot();

        mBinding.communityYourcontributeProposedchanges.setText(String.valueOf(0));
        mBinding.communityYourcontributeEvaluatedechanges.setText(String.valueOf(0));
        mBinding.communityGeneralstatsNumbertrashbins.setText(String.valueOf(0));
        mBinding.communityGeneralstatsNumberchanges.setText(String.valueOf(0));

        // applico il listener alla card "apri editor mappa"
        mBinding.communityCardOpenmapeditor.setOnClickListener(this::onClickOpen);

        // applico il listener al pulsante "le tue richieste"
        mBinding.communityCardYourcontributionButton.setOnClickListener(this::onClickYourChanges);

        // view model
        mCommunityViewModel = new ViewModelProvider(this).get(CommunityViewModel.class);

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mBinding = null;
    }

    /**
     * Funzione che viene chiamata appena la card "apri editor mappa" è premuta
     * @param view la view cliccata
     */
    private void onClickOpen(View view) {
        if(Utils.getCurrentUserAccount() != null) {
            Navigation.findNavController(view).popBackStack(R.id.nav_maps, false); // primo
            Navigation.findNavController(view).navigate(R.id.nav_mapeditor);
        } else {
            Toast.makeText(this.getContext(), R.string.community_cannotcontribute_warning, Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Funzione che viene chiamata appena viene premuto il tasto "tue modifiche"
     * @param view la view cliccata
     */
    private void onClickYourChanges(View view) {
        if(Utils.getCurrentUserAccount() != null) {
            Navigation.findNavController(view).navigate(R.id.nav_requests);
        } else {
            Toast.makeText(this.getContext(), R.string.community_yourcontribute_warning, Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Aggiorna i dati delle cards. E' chiamata dalla onStart (prima che vengano mostrati i contenuti)
     * e dalla onResume(), chiamata quando si effettua il login dalla barra laterale.
     */
    private void updateCards() {
        // mostro dati
        mCommunityViewModel.getMarkerNumber().observe(getViewLifecycleOwner(), numberOfBins -> {
            mBinding.communityGeneralstatsNumbertrashbins.setText(String.valueOf(numberOfBins));
            // scelgo un numero casuale perché non ho un database che mi restituisce quante modifiche sono state fatte in totale
            mBinding.communityGeneralstatsNumberchanges.setText(String.valueOf(new Random().nextInt(100000) + numberOfBins));
        });

        if(Utils.getCurrentUserAccount() != null) {
            mBinding.communityCardCannotcontribute.setVisibility(View.GONE);

            mBinding.communityCardYourcontribution.setVisibility(View.VISIBLE);
            mBinding.communityCardOpenmapeditor.setVisibility(View.VISIBLE);

            if(Utils.getCurrentUserAccount() != null)
                mCommunityViewModel.getUserRequestNumber(Utils.getCurrentUserAccount().getEmail()).observe(getViewLifecycleOwner(), numberOfOwnRequests -> mBinding.communityYourcontributeProposedchanges.setText(String.valueOf(numberOfOwnRequests)));
        } else {
            mBinding.communityCardCannotcontribute.setVisibility(View.VISIBLE);

            mBinding.communityCardYourcontribution.setVisibility(View.GONE);
            mBinding.communityCardOpenmapeditor.setVisibility(View.GONE);
        }
    }

    // --------- METODI ATTACH, DETACH, RESUME, PAUSE ---------

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        Log.d("ISTANZA", "community -> onAttach");
    }

    @Override
    public void onDetach() {
        super.onDetach();
        Log.d("ISTANZA", "community -> onDetach");
    }

    @Override
    public void onResume() {
        super.onResume();
        updateCards();
        Log.d("ISTANZA", "community -> onResume");
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d("ISTANZA", "community -> onPause");
    }

    @Override
    public void onStart() {
        super.onStart();
        updateCards();
        Log.d("ISTANZA", "community -> onStart");
    }
}