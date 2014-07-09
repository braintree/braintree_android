package com.braintreepayments.api.utils;

import android.app.Activity;
import android.os.Bundle;

import com.google.android.apps.common.testing.testrunner.GoogleInstrumentationTestRunner;

public class CustomInstrumentationTestRunner extends GoogleInstrumentationTestRunner {

    private SystemAnimations systemAnimations;

    @Override
    public void onCreate(Bundle arguments) {
        super.onCreate(arguments);
        systemAnimations = new SystemAnimations(getContext());
    }

    @Override
    public void callActivityOnCreate(Activity activity, Bundle bundle) {
        systemAnimations.disableAll();
        super.callActivityOnCreate(activity, bundle);
    }

    @Override
    public void callActivityOnDestroy(Activity activity) {
        super.callActivityOnDestroy(activity);
        systemAnimations.enableAll();
    }
}
