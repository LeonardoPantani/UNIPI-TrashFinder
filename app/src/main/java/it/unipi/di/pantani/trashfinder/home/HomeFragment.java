package it.unipi.di.pantani.trashfinder.home;

import static it.unipi.di.pantani.trashfinder.Utils.TAB_TITLES;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import trashfinder.R;
import trashfinder.databinding.FragmentHomeBinding;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private Context context;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
    }

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        SectionsPagerAdapter sectionsPagerAdapter = new SectionsPagerAdapter(this.getActivity());
        ViewPager2 viewPager = root.findViewById(R.id.view_pager);
        viewPager.setAdapter(sectionsPagerAdapter);
        TabLayout tabs = root.findViewById(R.id.tabs);
        viewPager.setUserInputEnabled(false);

        new TabLayoutMediator(tabs, viewPager,
                (tab, position) -> tab.setText(TAB_TITLES[position])
        ).attach();

        if(getArguments() != null) {
            if(getArguments().getBoolean("directCompass")) {
                // per evitare warning
                TabLayout.Tab toSelect = tabs.getTabAt(0);
                if(toSelect != null)
                    toSelect.select();
            }
        }

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}