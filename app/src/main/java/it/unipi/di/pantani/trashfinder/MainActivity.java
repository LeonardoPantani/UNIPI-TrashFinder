package it.unipi.di.pantani.trashfinder;

import static it.unipi.di.pantani.trashfinder.Utils.REQUIRE_PERMISSION_CODE_INTRO;
import static it.unipi.di.pantani.trashfinder.Utils.checkPerms;
import static it.unipi.di.pantani.trashfinder.Utils.setPreference;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavBackStackEntry;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.preference.PreferenceManager;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.navigation.NavigationView;

import it.unipi.di.pantani.trashfinder.databinding.ActivityMainBinding;
import it.unipi.di.pantani.trashfinder.feedback.FeedbackActivity;
import it.unipi.di.pantani.trashfinder.intro.SliderAdapter;
import it.unipi.di.pantani.trashfinder.settings.SettingsActivity;

public class MainActivity extends AppCompatActivity implements NavController.OnDestinationChangedListener {
    private SharedPreferences sp;
    private ActivityMainBinding binding;
    private AppBarConfiguration mAppBarConfiguration;

    private NavController navController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        sp = PreferenceManager.getDefaultSharedPreferences(this);
        super.onCreate(savedInstanceState);

        if(!sp.getBoolean("setting_show_intro_at_startup", true)) {
            startApp(null);
        } else {
            startIntro();
        }
    }

    public void startApp(View notUsed) {
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.appBarMain.toolbar);

        NavigationView navigationView = binding.navView;
        DrawerLayout drawer = binding.drawerLayout;
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_maps, R.id.nav_compass, R.id.nav_community, R.id.nav_mapeditor)
                .setOpenableLayout(drawer)
                .build();
        // ottengo il navController
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment_content_main);
        if(navHostFragment == null) { // non dovrebbe mai verificarsi!
            Log.d("ISTANZA", "navHostFragment null!");
            return;
        }
        navController = navHostFragment.getNavController();
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);
        navController.addOnDestinationChangedListener(this);

        if (!checkPerms(getBaseContext())) {
            showPermissionWarningDialog();
        }

        setPreference(this, "setting_show_intro_at_startup", false);
    }

    ViewPager viewPagerTutorial;
    public void startIntro() {
        setContentView(R.layout.activity_main_intro);

        viewPagerTutorial = findViewById(R.id.viewpager);
        SliderAdapter sliderAdapter = new SliderAdapter(this);
        viewPagerTutorial.setAdapter(sliderAdapter);
    }

    public void requirePermissionsIntro(View view) {
        if (!checkPerms(getBaseContext())) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUIRE_PERMISSION_CODE_INTRO);
        }

        if (checkPerms(getBaseContext())) {
            view.setEnabled(false);
        }
    }

    public void showPermissionWarningDialog() {
        if(sp.getBoolean("shown_permission_warning", false)) { // se è già stato mostrato una volta
            return;
        }

        AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.setTitle(getResources().getString(R.string.dialog_nolocationperm_title));
        alertDialog.setMessage(getResources().getString(R.string.dialog_nolocationperm_desc));
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, getResources().getString(R.string.dialog_nolocationperm_button_ok),
                (dialog, which) -> dialog.dismiss());
        alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, getResources().getString(R.string.dialog_nolocationperm_button_grantperms),
                (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    Uri uri = Uri.fromParts("package", getPackageName(), null);
                    intent.setData(uri);
                    startActivity(intent);
                    finish();
                });
        alertDialog.show();

        setPreference(this, "shown_permission_warning", true);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUIRE_PERMISSION_CODE_INTRO) { // tutorial
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(getApplicationContext(), getResources().getString(R.string.intro_location_permissiongranted, getResources().getString(R.string.app_name)), Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(getApplicationContext(), getResources().getString(R.string.intro_location_permissiondenied), Toast.LENGTH_LONG).show();
            }
            viewPagerTutorial.arrowScroll(View.FOCUS_RIGHT);
            findViewById(R.id.button_require_permissions).setVisibility(View.GONE);
            return;
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    // MENU
    @SuppressLint("RestrictedApi")
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        if (menu instanceof MenuBuilder) {
            ((MenuBuilder) menu).setOptionalIconsVisible(true);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        } else if(item.getItemId() == R.id.action_feedback) {
            startActivity(new Intent(this, FeedbackActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        return NavigationUI.navigateUp(navController, mAppBarConfiguration) || super.onSupportNavigateUp();
    }

    @Override
    public void onDestinationChanged(@NonNull NavController navController, @NonNull NavDestination navDestination, @Nullable Bundle bundle) {
        if(navDestination.getId() != R.id.nav_maps) {
            binding.appBarMain.fab.hide();
        }

        ActionBar ab = getSupportActionBar();
        if(ab != null)
            ab.setTitle(getResources().getString(R.string.app_name) + " - " + ab.getTitle());

        // debug
        Log.d("ISTANZA2", "> " + navDestination.getDisplayName());
        for(NavBackStackEntry a : navController.getBackQueue()) {
            Log.d("ISTANZA2", "Backstack -> " + a.getDestination().getDisplayName());
        }
    }
}