package it.unipi.di.pantani.trashfinder.intro;

import static it.unipi.di.pantani.trashfinder.Utils.checkPerms;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.viewpager.widget.PagerAdapter;

import it.unipi.di.pantani.trashfinder.R;
import pl.droidsonroids.gif.GifImageView;

public class SliderAdapter extends PagerAdapter {
    final Context context;
    LayoutInflater inflater;

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
    public final int[] colorArray = {
            Color.parseColor("#4F4F4F"),
            Color.parseColor("#4F4F4F"),
            Color.parseColor("#4F4F4F"),
            Color.parseColor("#4F4F4F"),
            Color.parseColor("#4F4F4F")

            /*
            Color.parseColor("#EF5555"),
            Color.parseColor("#00AD5F"),
            Color.parseColor("#3D8CED")
            */
    };

    public SliderAdapter(Context context) {
        this.context = context;
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
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.slide, container, false);

        ConstraintLayout constraintLayout = view.findViewById(R.id.constraintLayout);
        GifImageView gifImageView = view.findViewById(R.id.slideimg);
        TextView t1_title = view.findViewById(R.id.txtTitle);
        TextView t2_desc = view.findViewById(R.id.txtDescription);
        Button button_end = view.findViewById(R.id.button_end);
        Button require_permissions = view.findViewById(R.id.button_require_permissions);
        ImageView arrow_next = view.findViewById(R.id.arrow_next);
        TextView txt_swipe = view.findViewById(R.id.txtSwipe);

        constraintLayout.setBackgroundColor(colorArray[position]);
        gifImageView.setImageResource(imagesArray[position]);
        t1_title.setText(context.getResources().getString(titleArray[position]));
        t2_desc.setText(context.getResources().getString(descArray[position]));

        if(position == titleArray.length-2) {
            require_permissions.setVisibility(View.VISIBLE);
            if(checkPerms(context)) {
                require_permissions.setEnabled(false);
            }
        }

        if(position == titleArray.length-1) {
            button_end.setVisibility(View.VISIBLE);
            button_end.setText(context.getString(R.string.start_using_app, context.getResources().getString(R.string.app_name)));
            arrow_next.setVisibility(View.INVISIBLE);
            txt_swipe.setVisibility(View.INVISIBLE);
        }

        container.addView(view);
        return view;
    }
}
