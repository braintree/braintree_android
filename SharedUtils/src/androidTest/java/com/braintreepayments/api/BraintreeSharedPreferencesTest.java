package com.braintreepayments.api;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertFalse;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.security.GeneralSecurityException;

@RunWith(AndroidJUnit4ClassRunner.class)
public class BraintreeSharedPreferencesTest {

    private Context context;
    private SharedPreferences sharedPreferences;

    @Before
    public void beforeEach() throws GeneralSecurityException, IOException {
        context = ApplicationProvider.getApplicationContext();

        MasterKey masterKey = new MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build();

        sharedPreferences = EncryptedSharedPreferences.create(
                context,
                "testfilename",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        );
    }

    @After
    public void afterEach() {
        sharedPreferences.edit().clear().apply();
    }

    @Test
    public void getString_returnsFallbackStringByDefault() throws UnexpectedException {
        BraintreeSharedPreferences sut = new BraintreeSharedPreferences(sharedPreferences);
        assertEquals("fallbackValue", sut.getString("stringKey", "fallbackValue"));
    }

    @Test(expected = UnexpectedException.class)
    public void getString_whenSharedPreferencesNotAccessible_throwsError() throws UnexpectedException {
        BraintreeSharedPreferences sut = new BraintreeSharedPreferences(new UnexpectedException("message"));
        sut.getString("stringKey", "fallbackValue");
    }

    @Test
    public void putString_storesStringInSharedPreferences() throws UnexpectedException {
        BraintreeSharedPreferences sut = new BraintreeSharedPreferences(sharedPreferences);
        sut.putString("stringKey", "stringValue");

        assertEquals("stringValue", sut.getString("stringKey", ""));
    }

    @Test(expected = UnexpectedException.class)
    public void putString_whenSharedPreferencesNotAccessible_throwsError() throws UnexpectedException {
        BraintreeSharedPreferences sut = new BraintreeSharedPreferences(new UnexpectedException("message"));
        sut.putString("stringKey", "stringValue");
    }

    @Test
    public void getBoolean_returnsFalseByDefault() throws UnexpectedException {
        BraintreeSharedPreferences sut = new BraintreeSharedPreferences(sharedPreferences);
        assertFalse(sut.getBoolean("booleanKey"));
    }

    @Test(expected = UnexpectedException.class)
    public void getBoolean_whenSharedPreferencesNotAccessible_throwsError() throws UnexpectedException {
        BraintreeSharedPreferences sut = new BraintreeSharedPreferences(new UnexpectedException("message"));
        sut.getBoolean("booleanKey");
    }

    @Test
    public void putBoolean_storesBooleanInSharedPreferences() throws UnexpectedException {
        BraintreeSharedPreferences sut = new BraintreeSharedPreferences(sharedPreferences);
        sut.putBoolean("booleanKey", true);

        assertTrue(sut.getBoolean("booleanKey"));
    }

    @Test(expected = UnexpectedException.class)
    public void putBoolean_whenSharedPreferencesNotAccessible_throwsError() throws UnexpectedException {
        BraintreeSharedPreferences sut = new BraintreeSharedPreferences(new UnexpectedException("message"));
        sut.putBoolean("booleanKey", true);
    }

    @Test
    public void containsKey_whenKeyExists_returnsTrue() throws UnexpectedException {
        BraintreeSharedPreferences sut = new BraintreeSharedPreferences(sharedPreferences);
        sut.putBoolean("booleanKey", true);

        assertTrue(sut.containsKey("booleanKey"));
    }

    @Test
    public void containsKey_whenKeyDoesNotExist_returnsTrue() throws UnexpectedException {
        BraintreeSharedPreferences sut = new BraintreeSharedPreferences(sharedPreferences);
        assertFalse(sut.containsKey("booleanKey"));
    }

    @Test(expected = UnexpectedException.class)
    public void containsKey_whenSharedPreferencesNotAccessible_throwsError() throws UnexpectedException {
        BraintreeSharedPreferences sut = new BraintreeSharedPreferences(new UnexpectedException("message"));
        sut.containsKey("booleanKey");
    }

    @Test
    public void putStringAndLong_storesStringInSharedPreferences() throws UnexpectedException {
        BraintreeSharedPreferences sut = new BraintreeSharedPreferences(sharedPreferences);
        sut.putStringAndLong("stringKey", "stringValue", "longKey", 123L);

        assertEquals("stringValue", sut.getString("stringKey", null));
        assertEquals(123L, sut.getLong("longKey"));
    }

    @Test(expected = UnexpectedException.class)
    public void putStringAndLong_whenSharedPreferencesNotAccessible_throwsError() throws UnexpectedException {
        BraintreeSharedPreferences sut = new BraintreeSharedPreferences(new UnexpectedException("message"));
        sut.putStringAndLong("stringKey", "stringValue", "longKey", 123L);
    }

    @Test
    public void getLong_returnsZeroByDefault() throws UnexpectedException {
        BraintreeSharedPreferences sut = new BraintreeSharedPreferences(sharedPreferences);
        assertEquals(0L, sut.getLong("longKey"));
    }

    @Test(expected = UnexpectedException.class)
    public void getLong_whenSharedPreferencesNotAccessible_throwsError() throws UnexpectedException {
        BraintreeSharedPreferences sut = new BraintreeSharedPreferences(new UnexpectedException("message"));
        sut.getLong("longKey");
    }

    @Test
    public void clearSharedPreferences_clearsSharedPreferences() throws UnexpectedException {
        BraintreeSharedPreferences sut = new BraintreeSharedPreferences(sharedPreferences);

        sut.putString("stringKey", "stringValue");
        sut.putBoolean("booleanKey", true);
        sut.putStringAndLong("stringKey2", "stringValue2", "longKey", 123L);
        sut.clearSharedPreferences();

        assertFalse(sut.containsKey("stringKey"));
        assertFalse(sut.containsKey("booleanKey"));
        assertFalse(sut.containsKey("stringKey2"));
        assertFalse(sut.containsKey("longKey"));
    }

    @Test(expected = UnexpectedException.class)
    public void clearSharedPreferences_whenSharedPreferencesNotAccessible_throwsError() throws UnexpectedException {
        BraintreeSharedPreferences sut = new BraintreeSharedPreferences(new UnexpectedException("message"));
        sut.clearSharedPreferences();
    }
}