package com.braintreepayments.demo;

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
    }
}
