package com.braintreepayments.demo;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.navigation.NavController;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class DemoActivity extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback, ActionBar.OnNavigationListener {

    private AppBarConfiguration appBarConfiguration;
    private ActionBarController actionBarController = new ActionBarController();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_demo);

        ViewPager2 viewPager = findViewById(R.id.view_pager);
        viewPager.setAdapter(new DemoTabViewAdapter(this));

        TabLayout tabLayout = findViewById(R.id.tab_layout);
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {

            String tabText = null;
            DemoTab demoTab = DemoTab.from(position);
            switch (demoTab) {
                case FEATURES:
                    tabText = "Features";
                    break;
                case ENVIRONMENT:
                    tabText = "Environment";
                    break;
                case SETTINGS:
                    tabText = "Settings";
                    break;
            }
            tab.setText(tabText);

        }).attach();
        setupActionBar();
    }

    @Override
    protected void onResume() {
        super.onResume();
        actionBarController.updateTitle(this);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }

    private NavController getNavController() {
        return null;
//        Fragment navHostFragment = getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
//        return NavHostFragment.findNavController(navHostFragment);
    }

    private void setupActionBar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        NavController navController = getNavController();
        if (navController != null) {
            appBarConfiguration = new AppBarConfiguration.Builder(navController.getGraph()).build();
            NavigationUI.setupWithNavController(toolbar, navController, appBarConfiguration);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = getNavController();
        if (navController == null) {
            return super.onSupportNavigateUp();
        }
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        NavController navController = getNavController();

        if (navController != null) {
            int itemId = item.getItemId();

            if (itemId == R.id.change_authorization || itemId == R.id.change_environment) {
                navController.navigate(R.id.action_goto_change_auth_environment);
                return false;
            }

            if (itemId == R.id.request_settings) {
                navController.navigate(R.id.open_settings_fragment);
                return true;
            }
        }

        return false;
    }

    public boolean onNavigationItemSelected(int itemPosition, long itemId) {
        String env = getResources().getStringArray(R.array.environments)[itemPosition];
        if (!Settings.getEnvironment(this).equals(env)) {
            Settings.setEnvironment(this, env);
        }
        return true;
    }
}