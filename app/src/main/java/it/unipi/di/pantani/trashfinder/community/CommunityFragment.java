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

import it.unipi.di.pantani.trashfinder.R;
import it.unipi.di.pantani.trashfinder.Utils;
import it.unipi.di.pantani.trashfinder.databinding.FragmentCommunityBinding;

public class CommunityFragment extends Fragment {
    private FragmentCommunityBinding binding;
    private CommunityViewModel mCommunityViewModel;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        binding = FragmentCommunityBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        binding.communityYourcontributeProposedchanges.setText(String.valueOf(0));
        binding.communityYourcontributeEvaluatedechanges.setText(String.valueOf(0));
        binding.communityGeneralstatsNumbertrashbins.setText(String.valueOf(0));
        binding.communityGeneralstatsNumberchanges.setText(String.valueOf(0));

        // applico il listener alla card "apri editor mappa"
        binding.communityCardOpenmapeditor.setOnClickListener(this::onClickOpen);

        // view model
        mCommunityViewModel = new ViewModelProvider(this).get(CommunityViewModel.class);

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
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
            Toast.makeText(this.getContext(), R.string.community_cannotcontribute_warning, Toast.LENGTH_SHORT).show();
        }
    }

    private void updateCards() {
        if(Utils.getCurrentUserAccount() != null) {
            binding.communityCardCannotcontribute.setVisibility(View.GONE);

            binding.communityCardYourcontribution.setVisibility(View.VISIBLE);
            binding.communityCardOpenmapeditor.setVisibility(View.VISIBLE);
        } else {
            binding.communityCardCannotcontribute.setVisibility(View.VISIBLE);

            binding.communityCardYourcontribution.setVisibility(View.GONE);
            binding.communityCardOpenmapeditor.setVisibility(View.GONE);
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
        // mostro dati
        mCommunityViewModel.getMarkerNumber().observe(getViewLifecycleOwner(), numberOfBins -> binding.communityGeneralstatsNumbertrashbins.setText(String.valueOf(numberOfBins)));
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
    }
}