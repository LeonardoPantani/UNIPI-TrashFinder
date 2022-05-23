package it.unipi.di.pantani.trashfinder.settings;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
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
import androidx.preference.SwitchPreferenceCompat;

import com.google.android.material.snackbar.Snackbar;

import it.unipi.di.pantani.trashfinder.BuildConfig;
import it.unipi.di.pantani.trashfinder.R;
import it.unipi.di.pantani.trashfinder.Utils;

public class SettingsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        Context context = SettingsActivity.this;
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
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        SwitchPreferenceCompat setting_show_intro_at_startup;
        Preference settingstatic_osm;
        Preference settingstatic_version;

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

            setting_show_intro_at_startup = findPreference("setting_show_intro_at_startup");
            if(setting_show_intro_at_startup != null)
                setting_show_intro_at_startup.setOnPreferenceChangeListener(this::onShowIntroChange);

            settingstatic_osm = findPreference("settingstatic_osm");
            if(settingstatic_osm != null)
                settingstatic_osm.setOnPreferenceClickListener(this::onOSMCreditsClick);

            settingstatic_version = findPreference("settingstatic_version");
            if(settingstatic_version != null)
                settingstatic_version.setSummary(getResources().getString(R.string.version, getResources().getString(R.string.app_name), BuildConfig.VERSION_NAME));

            setting_map_type = findPreference("setting_map_type");
            setting_map_theme = findPreference("setting_map_theme");

            if(!setting_map_type.getValue().equals("roads")) {
                setting_map_theme.setEnabled(false);
            }

            setting_map_type.setOnPreferenceChangeListener(this::onMapTypeChange);
        }

        @SuppressWarnings("SameReturnValue")
        private boolean onOSMCreditsClick(Preference preference) {
            Uri uri = Uri.parse(Utils.OSM_WEBSITE);
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            return true;
        }

        @SuppressWarnings("SameReturnValue")
        public boolean onMapTypeChange(Preference preference, Object newValue) {
            setting_map_theme.setEnabled(newValue.equals("roads"));
            return true;
        }

        public boolean onShowIntroChange(Preference preference, Object newValue) {
            if(newValue.equals(true)) {
                View currentView = getView();
                if(currentView == null) return false;

                Snackbar mySnackbar = Snackbar.make(currentView, getResources().getString(R.string.show_intro_at_startup_tiprestart_title, getResources().getString(R.string.app_name)), Snackbar.LENGTH_SHORT);
                mySnackbar.setAction(R.string.show_intro_at_startup_tiprestart_button, view -> Utils.triggerRebirth(currentView.getContext()));
                mySnackbar.show();
            }

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
}
