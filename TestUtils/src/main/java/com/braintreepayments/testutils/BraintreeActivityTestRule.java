package com.braintreepayments.testutils;

import android.app.Activity;
import android.support.test.espresso.intent.Intents;
import android.support.test.rule.ActivityTestRule;

import static com.braintreepayments.testutils.SharedPreferencesHelper.getSharedPreferences;

public class BraintreeActivityTestRule<T extends Activity> extends ActivityTestRule<T> {

    public BraintreeActivityTestRule(Class<T> activityClass) {
        super(activityClass);
    }

    public BraintreeActivityTestRule(Class<T> activityClass, boolean initialTouchMode,
            boolean launchActivity) {
        super(activityClass, initialTouchMode, launchActivity);
    }

    @Override
    protected void beforeActivityLaunched() {
        super.beforeActivityLaunched();
        getSharedPreferences().edit().clear().commit();
    }

    @Override
    protected void afterActivityLaunched() {
        Intents.init();
        super.afterActivityLaunched();
    }

    @Override
    protected void afterActivityFinished() {
        super.afterActivityFinished();
        Intents.release();
        getSharedPreferences().edit().clear().commit();
    }
}
