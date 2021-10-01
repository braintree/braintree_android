package com.braintreepayments.api;

import static junit.framework.TestCase.assertEquals;
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

    private Context context;

    @Before
    public void beforeEach() throws GeneralSecurityException, IOException {
        context = ApplicationProvider.getApplicationContext();
        new BraintreeSharedPreferences().getSharedPreferences(context).edit().clear().apply();
    }

    @Test
    public void getSharedPreferences_returnsEncryptedSharedPreferences() throws GeneralSecurityException, IOException {
        BraintreeSharedPreferences sut = new BraintreeSharedPreferences();
        SharedPreferences sharedPreferences = sut.getSharedPreferences(ApplicationProvider.getApplicationContext());
        assertTrue(sharedPreferences instanceof EncryptedSharedPreferences);
    }

    @Test
    public void getSharedPreferences_returnsPreferencesWithBraintreeApiFilenameByDefault() throws GeneralSecurityException, IOException {
        BraintreeSharedPreferences sut = new BraintreeSharedPreferences();
        SharedPreferences sharedPreferences = sut.getSharedPreferences(context);
        sharedPreferences.edit().putBoolean("test-key-braintree-api", true).apply();
        assertTrue(sut.getSharedPreferences(context).getBoolean("test-key-braintree-api", false));
    }

    @Test
    public void getSharedPreferences_returnsPreferencesWithFilenameByFromConstructor() throws GeneralSecurityException, IOException {
        BraintreeSharedPreferences sut = new BraintreeSharedPreferences();
        SharedPreferences sharedPreferences = sut.getSharedPreferences(context);
        sharedPreferences.edit().putBoolean("test-key-custom-file", true).apply();
        assertTrue(sut.getSharedPreferences(context).getBoolean("test-key-custom-file", false));
    }

    @Test
    public void getString_returnsStringFromSharedPreferences() throws GeneralSecurityException, IOException {
        BraintreeSharedPreferences sut = new BraintreeSharedPreferences();
        SharedPreferences sharedPreferences = sut.getSharedPreferences(context);
        sharedPreferences.edit().putString("testKey", "testValue").apply();

        assertEquals("testValue", sut.getString(context, "testKey"));
    }

    @Test
    public void getString_withFilename_returnsStringFromSharedPreferences() throws GeneralSecurityException, IOException {
        BraintreeSharedPreferences sut = new BraintreeSharedPreferences();
        SharedPreferences sharedPreferences = sut.getSharedPreferences(context);
        sharedPreferences.edit().putString("testKey", "testValue").apply();

        assertEquals("testValue", sut.getString(context, "testKey"));
    }

    @Test
    public void putString_savesStringInSharedPreferences() throws GeneralSecurityException, IOException {
        BraintreeSharedPreferences sut = new BraintreeSharedPreferences();

        sut.putString(context, "testKey2", "testValue2");

        SharedPreferences sharedPreferences = sut.getSharedPreferences(context);
        assertEquals("testValue2", sharedPreferences.getString("testKey2", null));
    }

    @Test
    public void putString_withFilename_savesStringInSharedPreferences() throws GeneralSecurityException, IOException {
        BraintreeSharedPreferences sut = new BraintreeSharedPreferences();

        sut.putString(context, "testKey2", "testValue2");

        SharedPreferences sharedPreferences = sut.getSharedPreferences(context);
        assertEquals("testValue2", sharedPreferences.getString("testKey2", null));
    }

    @Test
    public void getBoolean_returnsBooleanFromSharedPreferences() throws GeneralSecurityException, IOException {
        BraintreeSharedPreferences sut = new BraintreeSharedPreferences();
        SharedPreferences sharedPreferences = sut.getSharedPreferences(context);
        sharedPreferences.edit().putBoolean("testKeyBoolean", true).apply();

        assertTrue(sut.getBoolean(context, "testKeyBoolean"));
    }

    @Test
    public void putBoolean_savesBooleanToSharedPreferences() throws GeneralSecurityException, IOException {
        BraintreeSharedPreferences sut = new BraintreeSharedPreferences();

        sut.putBoolean(context, "testKeyBoolean2", true);

        SharedPreferences sharedPreferences = sut.getSharedPreferences(context);
        assertTrue(sharedPreferences.getBoolean("testKeyBoolean2", false));
    }

    @Test
    public void containsKey_returnsIfKeyExistsInSharedPreferences() throws GeneralSecurityException, IOException {
        BraintreeSharedPreferences sut = new BraintreeSharedPreferences();

        SharedPreferences sharedPreferences = sut.getSharedPreferences(context);
        sharedPreferences.edit().putBoolean("testContainsKey", true).apply();

        assertTrue(sut.containsKey(context, "testContainsKey"));
    }

    @Test
    public void getLong_returnsLongFromSharedPreferences() throws GeneralSecurityException, IOException {
        BraintreeSharedPreferences sut = new BraintreeSharedPreferences();
        SharedPreferences sharedPreferences = sut.getSharedPreferences(context);
        sharedPreferences.edit().putLong("testKeyLong", 1L).apply();

        assertEquals(1L, sut.getLong(context, "testKeyLong"));
    }

    @Test
    public void putStringAndLong_savesToSharedPreferences() throws GeneralSecurityException, IOException {
        BraintreeSharedPreferences sut = new BraintreeSharedPreferences();

        sut.putStringAndLong(context, "testKeyString", "testValueString", "testKeyLong", 2L);

        SharedPreferences sharedPreferences = sut.getSharedPreferences(context);
        assertEquals("testValueString", sharedPreferences.getString("testKeyString", null));
        assertEquals(2L, sharedPreferences.getLong("testKeyLong", 0));
    }
}