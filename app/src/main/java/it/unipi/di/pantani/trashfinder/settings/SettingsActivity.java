package it.unipi.di.pantani.trashfinder.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.google.android.material.snackbar.Snackbar;

import it.unipi.di.pantani.trashfinder.R;
import it.unipi.di.pantani.trashfinder.Utils;

public class SettingsActivity extends AppCompatActivity {
    Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        context = SettingsActivity.this;
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings, new SettingsFragment())
                    .commit();
        }
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        PreferenceManager.getDefaultSharedPreferences(context).registerOnSharedPreferenceChangeListener(this::onSharedPreferenceChanged);
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        ListPreference setting_map_type;
        ListPreference setting_map_theme;

        @NonNull
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            return super.onCreateView(inflater, container, savedInstanceState);
        }

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);

            setting_map_type = findPreference("setting_map_type");
            setting_map_theme = findPreference("setting_map_theme");

            if(!setting_map_type.getValue().equals("roads")) {
                setting_map_theme.setEnabled(false);
            }

            setting_map_type.setOnPreferenceChangeListener(this::onMapTypeChange);
        }

        public boolean onMapTypeChange(Preference preference, Object newValue) {
            setting_map_theme.setEnabled(newValue.equals("roads"));
            return true;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Respond to the action bar's Up/Home button
        if (item.getItemId() == android.R.id.home) {
            super.onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);

    }

    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if(key.equals("setting_show_intro_at_startup") && sharedPreferences.getBoolean("setting_show_intro_at_startup", true)) {
            Snackbar mySnackbar = Snackbar.make(findViewById(R.id.settings), getResources().getString(R.string.show_intro_at_startup_tiprestart_title, getResources().getString(R.string.app_name)), Snackbar.LENGTH_SHORT);
            mySnackbar.setAction(R.string.show_intro_at_startup_tiprestart_button, view -> Utils.triggerRebirth(context));
            mySnackbar.show();
        }
    }
}
