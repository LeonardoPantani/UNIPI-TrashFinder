package it.unipi.di.pantani.trashfinder.community;

import static it.unipi.di.pantani.trashfinder.Utils.updateMapStyleByPreference;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

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
            Activity mainactivity = getActivity();
            DrawerLayout navDrawer;
            if(mainactivity != null) {
                navDrawer = mainactivity.findViewById(R.id.drawer_layout);
                if(!navDrawer.isDrawerOpen(GravityCompat.START))
                    navDrawer.openDrawer(GravityCompat.START);
            }
        });

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

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