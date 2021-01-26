package com.braintreepayments.api;

import android.annotation.SuppressLint;
import android.app.KeyguardManager;
import android.app.KeyguardManager.KeyguardLock;
import android.content.Context;

import androidx.appcompat.app.AppCompatActivity;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.rule.ActivityTestRule;

import static com.braintreepayments.api.SharedPreferencesHelper.getSharedPreferences;

@SuppressWarnings("deprecation")
public class BraintreeActivityTestRule<T extends AppCompatActivity> extends ActivityTestRule<T> {

    private KeyguardLock mKeyguardLock;

    public BraintreeActivityTestRule(Class<T> activityClass) {
        super(activityClass);
        init();
    }

    public BraintreeActivityTestRule(Class<T> activityClass, boolean initialTouchMode,
            boolean launchActivity) {
        super(activityClass, initialTouchMode, launchActivity);
        init();
    }

    @SuppressWarnings("MissingPermission")
    @SuppressLint({"MissingPermission", "ApplySharedPref"})
    private void init() {
        getSharedPreferences(ApplicationProvider.getApplicationContext()).edit().clear().commit();

        mKeyguardLock = ((KeyguardManager) ApplicationProvider.getApplicationContext().getSystemService(Context.KEYGUARD_SERVICE))
                .newKeyguardLock("BraintreeActivityTestRule");
        mKeyguardLock.disableKeyguard();
    }

    @SuppressWarnings("MissingPermission")
    @SuppressLint({"MissingPermission", "ApplySharedPref"})
    @Override
    protected void afterActivityFinished() {
        super.afterActivityFinished();

        getSharedPreferences(ApplicationProvider.getApplicationContext()).edit().clear().commit();

        mKeyguardLock.reenableKeyguard();
    }
}
