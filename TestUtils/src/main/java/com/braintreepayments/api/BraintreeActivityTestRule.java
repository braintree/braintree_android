package com.braintreepayments.api;

import static com.braintreepayments.api.SharedPreferencesHelper.getSharedPreferences;

import android.annotation.SuppressLint;
import android.app.KeyguardManager;
import android.app.KeyguardManager.KeyguardLock;
import android.content.Context;

import androidx.appcompat.app.AppCompatActivity;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.rule.ActivityTestRule;

@SuppressWarnings("deprecation")
public class BraintreeActivityTestRule<T extends AppCompatActivity> extends ActivityTestRule<T> {

    private KeyguardLock keyguardLock;

    public BraintreeActivityTestRule(Class<T> activityClass) {
        super(activityClass);
        init();
    }

    @SuppressWarnings("MissingPermission")
    @SuppressLint({"MissingPermission", "ApplySharedPref"})
    private void init() {
        Context context = ApplicationProvider.getApplicationContext();
        getSharedPreferences(context).edit().clear().commit();
        BraintreeSharedPreferences.getInstance(context).clearSharedPreferences();

        keyguardLock = ((KeyguardManager) ApplicationProvider.getApplicationContext().getSystemService(Context.KEYGUARD_SERVICE))
                .newKeyguardLock("BraintreeActivityTestRule");
        keyguardLock.disableKeyguard();
    }

    @SuppressWarnings("MissingPermission")
    @SuppressLint({"MissingPermission", "ApplySharedPref"})
    @Override
    protected void afterActivityFinished() {
        super.afterActivityFinished();

        Context context = ApplicationProvider.getApplicationContext();
        getSharedPreferences(context).edit().clear().commit();
        BraintreeSharedPreferences.getInstance(context).clearSharedPreferences();

        keyguardLock.reenableKeyguard();
    }
}
