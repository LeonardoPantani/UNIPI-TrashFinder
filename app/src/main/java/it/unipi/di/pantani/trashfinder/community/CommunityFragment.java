package it.unipi.di.pantani.trashfinder.community;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import it.unipi.di.pantani.trashfinder.mapeditor.MapEditorFragment;
import trashfinder.R;
import trashfinder.databinding.FragmentCommunityBinding;

public class CommunityFragment extends Fragment {

    private FragmentCommunityBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentCommunityBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        TextView proposedchanges = root.findViewById(R.id.community_yourcontribute_proposedchanges);
        TextView evaluatedchanges = root.findViewById(R.id.community_yourcontribute_evaluatedchanges);
        TextView numbertrashbins = root.findViewById(R.id.community_generalstats_numbertrashbins);
        TextView numberchanges = root.findViewById(R.id.community_generalstats_numberchanges);

        proposedchanges.setText(getResources().getString(R.string.community_yourcontribute_proposedchanges, 0));
        evaluatedchanges.setText(getResources().getString(R.string.community_yourcontribute_evaluatedchanges, 0));
        numbertrashbins.setText(getResources().getString(R.string.community_generalstats_numbertrashbins, 0));
        numberchanges.setText(getResources().getString(R.string.community_generalstats_numberchanges, 0));

        CardView cardView = root.findViewById(R.id.community_card_openmapeditor);

        cardView.setOnClickListener(v -> {
            // cambio fragment
            FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
            transaction.replace(R.id.nav_host_fragment_content_main, new MapEditorFragment());
            transaction.commit();
        });

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}