/*
 * Copyright (c) 2021/2022
 * Leonardo Pantani - 598896
 * University of Pisa - Department of Computer Science
 */

package it.unipi.di.pantani.trashfinder.intro;

import static it.unipi.di.pantani.trashfinder.Utils.checkPerms;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.viewpager.widget.PagerAdapter;

import it.unipi.di.pantani.trashfinder.R;
import it.unipi.di.pantani.trashfinder.Utils;
import it.unipi.di.pantani.trashfinder.databinding.SlideLayoutBinding;

public class SliderAdapter extends PagerAdapter {
    private final Context mContext;

    public final int[] imagesArray = {
            R.drawable.map_animation,
            R.drawable.chronometer_animation,
            R.drawable.verify_animation,
            R.drawable.marker_icon,
            R.drawable.done_icon
    };
    public final int[] titleArray = {
            R.string.intro_title_1,
            R.string.intro_title_2,
            R.string.intro_title_3,
            R.string.intro_title_4,
            R.string.intro_title_5
    };
    public final int[] descArray = {
            R.string.intro_desc_1,
            R.string.intro_desc_2,
            R.string.intro_desc_3,
            R.string.intro_desc_4,
            R.string.intro_desc_5
    };

    public SliderAdapter(Context mContext) {
        this.mContext = mContext;
    }

    @Override
    public int getCount() {
        return titleArray.length;
    }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
        return (view == object);
    }

    @Override
    public void destroyItem(ViewGroup container, int position, @NonNull Object object) {
        container.removeView((View) object);
    }

    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup container, int position) {
        LayoutInflater li = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        it.unipi.di.pantani.trashfinder.databinding.SlideLayoutBinding binding = SlideLayoutBinding.inflate(li, container, false);
        View root = binding.getRoot();

        if(Utils.getThemeMode(mContext) == 1) { // modalità notte attiva
            binding.constraintLayout.setBackgroundColor(mContext.getResources().getColor(R.color.darkgray, mContext.getTheme()));
        }
        binding.slideimg.setImageResource(imagesArray[position]);
        binding.txtTitle.setText(mContext.getResources().getString(titleArray[position]));
        binding.txtDescription.setText(mContext.getResources().getString(descArray[position]));

        if(position == titleArray.length-2) {
            binding.buttonRequirePermissions.setVisibility(View.VISIBLE);
            if(checkPerms(mContext)) {
                binding.buttonRequirePermissions.setEnabled(false);
            }
        }

        if(position == titleArray.length-1) {
            binding.buttonEnd.setVisibility(View.VISIBLE);
            binding.txtDescription.setText(mContext.getResources().getString(descArray[position], mContext.getResources().getString(R.string.app_name)));
            binding.buttonEnd.setText(mContext.getString(R.string.start_using_app, mContext.getResources().getString(R.string.app_name)));
            binding.arrowNext.setVisibility(View.INVISIBLE);
            binding.txtSwipe.setVisibility(View.INVISIBLE);
        }

        container.addView(root);
        return root;
    }
}
