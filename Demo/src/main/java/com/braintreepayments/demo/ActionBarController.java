package com.braintreepayments.demo;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

public class ActionBarController {

    public void updateTitle(Fragment fragment) {
        FragmentActivity activity = fragment.getActivity();
        if (activity instanceof AppCompatActivity) {
            updateTitle((AppCompatActivity) activity);
        }
    }

    public void updateTitle(AppCompatActivity activity) {
        ActionBar actionBar = activity.getSupportActionBar();
        if (actionBar != null) {
            String environment = Settings.getEnvironment(activity);
            String authType = Settings.getAuthorizationType(activity);
            actionBar.setTitle(String.format("%s / %s", environment, authType));
        }
    }
}
