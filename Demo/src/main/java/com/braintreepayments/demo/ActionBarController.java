package com.braintreepayments.demo;

import android.view.View;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

public class ActionBarController {

    public void updateTitle(Fragment fragment) {
        Toolbar toolbar = fragment.getView().findViewById(R.id.toolbar);
        if (toolbar != null) {
            FragmentActivity activity = fragment.requireActivity();
            String environment = Settings.getEnvironment(activity);
            String authType = Settings.getAuthorizationType(activity);
            toolbar.setTitle(String.format("%s / %s", environment, authType));
        }
//        FragmentActivity activity = fragment.getActivity();
//        if (activity instanceof AppCompatActivity) {
//            updateTitle((AppCompatActivity) activity);
//        }
    }

    public void updateTitle(AppCompatActivity activity) {
//        ActionBar actionBar = activity.getSupportActionBar();
//        if (actionBar != null) {
//            String environment = Settings.getEnvironment(activity);
//            String authType = Settings.getAuthorizationType(activity);
//            actionBar.setTitle(String.format("%s / %s", environment, authType));
//        }
    }
}
