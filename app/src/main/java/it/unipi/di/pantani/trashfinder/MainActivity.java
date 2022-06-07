/*
 * Copyright (c) 2021/2022
 * Leonardo Pantani - 598896
 * University of Pisa - Department of Computer Science
 */

package it.unipi.di.pantani.trashfinder;

import static it.unipi.di.pantani.trashfinder.Utils.IMPORT_SIZE;
import static it.unipi.di.pantani.trashfinder.Utils.REQUIRE_PERMISSION_CODE_INTRO;
import static it.unipi.di.pantani.trashfinder.Utils.amIOnCellular;
import static it.unipi.di.pantani.trashfinder.Utils.checkPerms;
import static it.unipi.di.pantani.trashfinder.Utils.setCurrentUserAccount;
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
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.preference.PreferenceManager;
import androidx.viewpager.widget.ViewPager;

import com.bumptech.glide.Glide;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.navigation.NavigationView;

import java.text.DecimalFormat;

import it.unipi.di.pantani.trashfinder.databinding.ActivityMainBinding;
import it.unipi.di.pantani.trashfinder.feedback.FeedbackActivity;
import it.unipi.di.pantani.trashfinder.intro.SliderAdapter;
import it.unipi.di.pantani.trashfinder.services.DownloadService;
import it.unipi.di.pantani.trashfinder.settings.SettingsActivity;

public class MainActivity extends AppCompatActivity implements NavController.OnDestinationChangedListener {
    private SharedPreferences mSharedPrefs;
    private ActivityMainBinding mBinding;
    private AppBarConfiguration mAppBarConfiguration;

    private NavigationView mNavigationView;
    private NavController mNavController;
    private DrawerLayout mDrawer;

    public GoogleSignInClient mGoogleSignInClient;

    public boolean mAskImportLater = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        super.onCreate(savedInstanceState);

        if(!mSharedPrefs.getBoolean("finished_import", false)) {
            if(!amIOnCellular(this)) {
                importTrashBins(false);
            } else {
                mAskImportLater = true;
            }
        }

        if(!mSharedPrefs.getBoolean("setting_show_intro_at_startup", true)) {
            startApp(null);
        } else {
            startIntro();
        }
    }

    public void startApp(View notUsed) {
        mBinding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());

        setSupportActionBar(mBinding.appBarMain.toolbar);

        mNavigationView = mBinding.navView;
        mDrawer = mBinding.drawerLayout;
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_maps, R.id.nav_compass, R.id.nav_community, R.id.nav_mapeditor)
                .setOpenableLayout(mDrawer)
                .build();
        // ottengo il navController
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment_content_main);
        if(navHostFragment == null) { // non dovrebbe mai verificarsi!
            Log.d("ISTANZA", "navHostFragment null!");
            return;
        }
        mNavController = navHostFragment.getNavController();
        NavigationUI.setupActionBarWithNavController(this, mNavController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(mNavigationView, mNavController);
        mNavController.addOnDestinationChangedListener(this);

        mBinding.navView.getHeaderView(0).setOnClickListener(this::onClickNavHeader);

        if (!checkPerms(getBaseContext())) {
            showPermissionWarningDialog();
        }

        if(mAskImportLater) {
            showDownloadDialog();
        }

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken("516037535700-889rh075g8r1koeqavliifpivsbmnns8.apps.googleusercontent.com")
                .requestEmail()
                .build();
        // Build a GoogleSignInClient with the options specified by gso.
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        initializeAccount();

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
        if (!checkPerms(view.getContext())) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUIRE_PERMISSION_CODE_INTRO);
        }
    }

    public void showPermissionWarningDialog() {
        if(mSharedPrefs.getBoolean("shown_permission_warning", false)) { // se è già stato mostrato una volta
            return;
        }

        AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.setTitle(getResources().getString(R.string.dialog_nolocationperm_title));
        alertDialog.setMessage(getResources().getString(R.string.dialog_nolocationperm_desc, getResources().getString(R.string.app_name)));
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, getResources().getString(R.string.button_ok),
                (dialog, which) -> dialog.dismiss());
        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, getResources().getString(R.string.dialog_nolocationperm_button_grantperms),
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

    public void showDownloadDialog() {
        if(amIOnCellular(this)) {
            DecimalFormat df = new DecimalFormat("#0.0");
            String mb = df.format(IMPORT_SIZE);

            AlertDialog alertDialog = new AlertDialog.Builder(this).create();
            alertDialog.setTitle(getResources().getString(R.string.dialog_importoncellular_title));
            alertDialog.setMessage(getResources().getString(R.string.dialog_importoncellular_desc, getResources().getString(R.string.app_name), mb));
            alertDialog.setCanceledOnTouchOutside(false);
            alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, getResources().getString(R.string.button_no),
                    (dialog, which) -> {
                        dialog.dismiss();
                        importTrashBins(true);
                        Toast.makeText(this, getResources().getString(R.string.import_nodownload), Toast.LENGTH_LONG).show();
                    });
            alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, getResources().getString(R.string.button_ok),
                    (dialog, which) -> {
                        importTrashBins(false);
                        Toast.makeText(this, getResources().getString(R.string.import_download_starting), Toast.LENGTH_LONG).show();
                    });
            alertDialog.show();
        } else {
            importTrashBins(false);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUIRE_PERMISSION_CODE_INTRO) { // tutorial
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, getResources().getString(R.string.intro_location_permissiongranted, getResources().getString(R.string.app_name)), Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, getResources().getString(R.string.intro_location_permissiondenied), Toast.LENGTH_LONG).show();
            }
            viewPagerTutorial.arrowScroll(View.FOCUS_RIGHT);
            findViewById(R.id.button_require_permissions).setVisibility(View.GONE);
            return;
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @SuppressLint("RestrictedApi")
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // MENU
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
        return NavigationUI.navigateUp(mNavController, mAppBarConfiguration) || super.onSupportNavigateUp();
    }

    @Override
    public void onDestinationChanged(@NonNull NavController navController, @NonNull NavDestination navDestination, @Nullable Bundle bundle) {
        if(navDestination.getId() != R.id.nav_maps) {
            mBinding.appBarMain.fab.hide();
        }

        ActionBar ab = getSupportActionBar();
        if(ab != null)
            ab.setTitle(getResources().getString(R.string.app_name) + " - " + ab.getTitle());
    }

    /**
     * Eseguito al click sulla parte alta del navigation drawer.
     * @param view view dell'header
     */
    public void onClickNavHeader(View view) {
        mDrawer.close();
        handleAuthentication();
    }

    public void handleAuthentication() {
        if(GoogleSignIn.getLastSignedInAccount(this) != null) {
            AlertDialog alertDialog = new AlertDialog.Builder(this).create();
            alertDialog.setTitle(getResources().getString(R.string.auth_logout_title));
            alertDialog.setMessage(getResources().getString(R.string.auth_logout_desc));
            alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, getResources().getString(R.string.button_cancel),
                    (dialog, which) -> dialog.dismiss());
            alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, getResources().getString(R.string.button_ok),
                    (dialog, which) -> {
                        mGoogleSignInClient.signOut();
                        setTextNavDrawer(false, null, null);
                        setCurrentUserAccount(null);
                        Toast.makeText(this,R.string.auth_logout_successful, Toast.LENGTH_SHORT).show();
                        /*
                         questo risolve il problema in cui si faccia il logout da una sezione
                         inaccessibile se non loggati, o diversa rispetto alla versione non loggata.
                         */
                        mNavController.popBackStack(R.id.nav_maps, false);
                    });
            alertDialog.show();
        } else {
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            someActivityResultLauncher.launch(signInIntent);
        }
    }

    // versione complicata della registerForResult
    public ActivityResultLauncher<Intent> someActivityResultLauncher = registerForActivityResult(
        new ActivityResultContracts.StartActivityForResult(),
            result -> {
                Intent data = result.getData();
                Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
                handleSignInResult(task);
            });

    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            setCurrentUserAccount(account);
            // utente collegato, aggiorno UI
            setTextNavDrawer(true, account.getDisplayName(), account.getPhotoUrl());
            Toast.makeText(this, getResources().getString(R.string.auth_login_successful, account.getDisplayName()), Toast.LENGTH_SHORT).show();
        } catch (ApiException e) {
            // errore (l'utente ha annullato il login)
            Log.d("ISTANZA", "signInResult:failed code=" + e.getStatusCode());
        }
    }

    private void setTextNavDrawer(boolean logged, String displayName, Uri image) {
        if(mBinding == null) return;

        View navHeader = mBinding.navView.getHeaderView(0);

        if(logged) {
            ((TextView)navHeader.findViewById(R.id.nav_header_title)).setText(displayName);
            ((TextView)navHeader.findViewById(R.id.nav_header_subtitle)).setText(getResources().getString(R.string.nav_header_subtitle_logged));

            mNavigationView.getMenu().findItem(R.id.nav_mapeditor).setEnabled(true);
        } else {
            ((TextView)navHeader.findViewById(R.id.nav_header_title)).setText(R.string.nav_header_title);
            ((TextView)navHeader.findViewById(R.id.nav_header_subtitle)).setText(R.string.nav_header_subtitle);
            ((ImageView) navHeader.findViewById(R.id.nav_header_image)).setImageResource(R.mipmap.ic_appicon_round);

            mNavigationView.getMenu().findItem(R.id.nav_mapeditor).setEnabled(false);
        }

        // mostro l'immagine del profilo dell'utente connesso, altrimenti ne mostro una di default
        Glide.with(this)
                .load(image)
                .error(R.drawable.ic_baseline_running_with_errors_24)
                .placeholder(R.drawable.ic_baseline_downloading_24)
                .fallback(R.mipmap.ic_appicon) // se "image" è null metto questo
                .fitCenter()
                .into(((ImageView) navHeader.findViewById(R.id.nav_header_image)));
    }

    private void initializeAccount() {
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        if(account != null) {
            setTextNavDrawer(true, account.getDisplayName(), account.getPhotoUrl());
            setCurrentUserAccount(account);
        } else {
            setTextNavDrawer(false, null, null);
            setCurrentUserAccount(null);
        }
    }

    @SuppressWarnings("SameParameterValue")
    private void importTrashBins(boolean forceManual) {
        Intent intent = new Intent(MainActivity.this, DownloadService.class);
        if(forceManual) {
            intent.putExtra("forceManual", true);
        }
        startService(intent);
    }

    /**
     * Chiudo il drawer laterale quando si preme il tasto indietro.
     */
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        mDrawer.close();
    }
}