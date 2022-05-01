package it.unipi.di.pantani.trashfinder.community;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import trashfinder.R;
import trashfinder.databinding.FragmentCommunityBinding;

public class CommunityFragment extends Fragment {
    private FragmentCommunityBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        binding = FragmentCommunityBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        binding.communityYourcontributeProposedchanges.setText(String.valueOf(0));
        binding.communityYourcontributeEvaluatedechanges.setText(String.valueOf(0));
        binding.communityGeneralstatsNumbertrashbins.setText(String.valueOf(0));
        binding.communityGeneralstatsNumberchanges.setText(String.valueOf(0));

        // applico il listener alla card "apri editor mappa"
        binding.communiyCardOpenmapeditor.setOnClickListener(this::onClickOpen);

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
    public void onClickOpen(View view) {
        Navigation.findNavController(view).popBackStack(R.id.nav_maps, false); // primo
        Navigation.findNavController(view).navigate(R.id.nav_mapeditor);
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
        Log.d("ISTANZA", "community -> onResume");
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d("ISTANZA", "community -> onPause");
    }
}