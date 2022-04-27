package it.unipi.di.pantani.trashfinder.home;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import it.unipi.di.pantani.trashfinder.compass.CompassFragment;

/**
 * A [FragmentPagerAdapter] that returns a fragment corresponding to
 * one of the sections/tabs/pages.
 */
public class SectionsPagerAdapter extends FragmentStateAdapter {
    public SectionsPagerAdapter(FragmentActivity fa) {
        super(fa);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        Fragment fragment;
        switch(position) {
            case 1: {
                fragment = new CompassFragment();
                break;
            }

            case 0:
            default: {
                fragment = new MapsFragment();
                break;
            }
        }

        return fragment;
    }

    @Override
    public int getItemCount() {
        return 2;
    }
}