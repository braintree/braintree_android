package com.braintreepayments.api.testutils;

import static com.braintreepayments.api.testutils.SharedPreferencesHelper.getSharedPreferences;

import android.annotation.SuppressLint;
import android.app.KeyguardManager;
import android.app.KeyguardManager.KeyguardLock;
import android.content.Context;

import androidx.appcompat.app.AppCompatActivity;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.rule.ActivityTestRule;

import com.braintreepayments.api.sharedutils.BraintreeSharedPreferences;

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

        keyguardLock = ((KeyguardManager) ApplicationProvider.getApplicationContext()
                .getSystemService(Context.KEYGUARD_SERVICE))
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
