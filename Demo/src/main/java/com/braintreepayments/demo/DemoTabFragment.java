package com.braintreepayments.demo;

import android.app.ActionBar;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

public class DemoTabFragment extends Fragment {

    private ActionBarController actionBarController = new ActionBarController();

    public DemoTabFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_demo_tab, container, false);

        // setup action bar
        Toolbar toolbar = view.findViewById(R.id.toolbar);
        NavController navController = getNavController();

        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(navController.getGraph()).build();
        NavigationUI.setupWithNavController(toolbar, navController, appBarConfiguration);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        actionBarController.updateTitle(this);
    }

    private NavController getNavController() {
        Fragment navHostFragment =
            getChildFragmentManager().findFragmentById(R.id.nav_host_fragment);
        return ((NavHostFragment) navHostFragment).getNavController();
    }
}