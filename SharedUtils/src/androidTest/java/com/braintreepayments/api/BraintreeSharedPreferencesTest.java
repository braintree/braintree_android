package com.braintreepayments.api;

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertNull;
import static junit.framework.TestCase.assertTrue;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.security.GeneralSecurityException;

@RunWith(AndroidJUnit4ClassRunner.class)
public class BraintreeSharedPreferencesTest {

    @Before
    public void beforeEach() throws GeneralSecurityException, IOException {
        new BraintreeSharedPreferences().getSharedPreferences(ApplicationProvider.getApplicationContext()).edit().clear().apply();
    }

    @Test
    public void getSharedPreferences_returnsEncryptedSharedPreferences() throws GeneralSecurityException, IOException {
        BraintreeSharedPreferences sut = new BraintreeSharedPreferences();
        SharedPreferences sharedPreferences = sut.getSharedPreferences(ApplicationProvider.getApplicationContext());
        assertTrue(sharedPreferences instanceof EncryptedSharedPreferences);
    }

    @Test
    public void getSharedPreferences_returnsPreferencesWithBraintreeApiFileNameByDefault() throws GeneralSecurityException, IOException {
        Context context = ApplicationProvider.getApplicationContext();
        BraintreeSharedPreferences sut = new BraintreeSharedPreferences();
        SharedPreferences sharedPreferences = sut.getSharedPreferences(context);
        sharedPreferences.edit().putBoolean("test-key-braintree-api", true).apply();
        assertTrue(sut.getSharedPreferences(context, "BraintreeApi").getBoolean("test-key-braintree-api", false));
    }

    @Test
    public void getSharedPreferences_returnsPreferencesWithFileNameByFromConstructor() throws GeneralSecurityException, IOException {
        Context context = ApplicationProvider.getApplicationContext();
        BraintreeSharedPreferences sut = new BraintreeSharedPreferences();
        SharedPreferences sharedPreferences = sut.getSharedPreferences(context, "custom-file-name");
        sharedPreferences.edit().putBoolean("test-key-custom-file", true).apply();
        assertTrue(sut.getSharedPreferences(context, "custom-file-name").getBoolean("test-key-custom-file", false));
    }
}