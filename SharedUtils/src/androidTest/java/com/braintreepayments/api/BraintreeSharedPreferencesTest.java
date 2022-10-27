package com.braintreepayments.api;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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

    private SharedPreferences workingSharedPreferences;

    private SharedPreferences failingSharedPreferences;
    private SharedPreferences.Editor failingSharedPreferencesEditor;

    @Before
    public void beforeEach() throws GeneralSecurityException, IOException {
        Context context = ApplicationProvider.getApplicationContext();

        MasterKey masterKey = new MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build();

        workingSharedPreferences = EncryptedSharedPreferences.create(
                context,
                "testfilename",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        );

        failingSharedPreferences = mock(SharedPreferences.class);
        when(failingSharedPreferences.getString(anyString(), anyString()))
                .thenThrow(new SecurityException("get string error"));
        when(failingSharedPreferences.getBoolean(anyString(), anyBoolean()))
                .thenThrow(new SecurityException("get boolean error"));
        when(failingSharedPreferences.contains(anyString()))
                .thenThrow(new SecurityException("get contains error"));
        when(failingSharedPreferences.getLong(anyString(), anyLong()))
                .thenThrow(new SecurityException("get long error"));

        failingSharedPreferencesEditor = mock(SharedPreferences.Editor.class);
        when(failingSharedPreferences.edit()).thenReturn(failingSharedPreferencesEditor);

        when(failingSharedPreferences.edit().putString(anyString(), anyString()))
                .thenThrow(new SecurityException("put string error"));
        when(failingSharedPreferences.edit().putBoolean(anyString(), anyBoolean()))
                .thenThrow(new SecurityException("put boolean error"));
        when(failingSharedPreferences.edit().putLong(anyString(), anyLong()))
                .thenThrow(new SecurityException("put long error"));
        when(failingSharedPreferences.edit().clear())
                .thenThrow(new SecurityException("clear error"));
    }

    @After
    public void afterEach() {
        workingSharedPreferences.edit().clear().apply();
    }

    @Test
    public void getString_returnsFallbackStringByDefault() throws UnexpectedException {
        BraintreeSharedPreferences sut = new BraintreeSharedPreferences(workingSharedPreferences);
        assertEquals("fallbackValue", sut.getString("stringKey", "fallbackValue"));
    }

    @Test(expected = UnexpectedException.class)
    public void getString_whenSharedPreferencesFails_throwsError() throws UnexpectedException {
        BraintreeSharedPreferences sut = new BraintreeSharedPreferences(failingSharedPreferences);
        sut.getString("stringKey", "fallbackValue");
    }

    @Test(expected = UnexpectedException.class)
    public void getString_whenSharedPreferencesNotAccessible_throwsError() throws UnexpectedException {
        BraintreeSharedPreferences sut = new BraintreeSharedPreferences(new UnexpectedException("message"));
        sut.getString("stringKey", "fallbackValue");
    }

    @Test
    public void putString_storesStringInSharedPreferences() throws UnexpectedException {
        BraintreeSharedPreferences sut = new BraintreeSharedPreferences(workingSharedPreferences);
        sut.putString("stringKey", "stringValue");

        assertEquals("stringValue", sut.getString("stringKey", ""));
    }

    @Test(expected = UnexpectedException.class)
    public void putString_whenSharedPreferencesFails_throwsError() throws UnexpectedException {
        BraintreeSharedPreferences sut = new BraintreeSharedPreferences(failingSharedPreferences);
        sut.putString("stringKey", "stringValue");
    }

    @Test(expected = UnexpectedException.class)
    public void putString_whenSharedPreferencesNotAccessible_throwsError() throws UnexpectedException {
        BraintreeSharedPreferences sut = new BraintreeSharedPreferences(new UnexpectedException("message"));
        sut.putString("stringKey", "stringValue");
    }

    @Test
    public void getBoolean_returnsFalseByDefault() throws UnexpectedException {
        BraintreeSharedPreferences sut = new BraintreeSharedPreferences(workingSharedPreferences);
        assertFalse(sut.getBoolean("booleanKey"));
    }

    @Test(expected = UnexpectedException.class)
    public void getBoolean_whenSharedPreferencesFails_throwsError() throws UnexpectedException {
        BraintreeSharedPreferences sut = new BraintreeSharedPreferences(failingSharedPreferences);
        sut.getBoolean("booleanKey");
    }

    @Test(expected = UnexpectedException.class)
    public void getBoolean_whenSharedPreferencesNotAccessible_throwsError() throws UnexpectedException {
        BraintreeSharedPreferences sut = new BraintreeSharedPreferences(new UnexpectedException("message"));
        sut.getBoolean("booleanKey");
    }

    @Test
    public void putBoolean_storesBooleanInSharedPreferences() throws UnexpectedException {
        BraintreeSharedPreferences sut = new BraintreeSharedPreferences(workingSharedPreferences);
        sut.putBoolean("booleanKey", true);

        assertTrue(sut.getBoolean("booleanKey"));
    }

    @Test(expected = UnexpectedException.class)
    public void putBoolean_whenSharedPreferencesFails_throwsError() throws UnexpectedException {
        BraintreeSharedPreferences sut = new BraintreeSharedPreferences(failingSharedPreferences);
        sut.putBoolean("booleanKey", true);
    }

    @Test(expected = UnexpectedException.class)
    public void putBoolean_whenSharedPreferencesNotAccessible_throwsError() throws UnexpectedException {
        BraintreeSharedPreferences sut = new BraintreeSharedPreferences(new UnexpectedException("message"));
        sut.putBoolean("booleanKey", true);
    }

    @Test
    public void containsKey_whenKeyExists_returnsTrue() throws UnexpectedException {
        BraintreeSharedPreferences sut = new BraintreeSharedPreferences(workingSharedPreferences);
        sut.putBoolean("booleanKey", true);

        assertTrue(sut.containsKey("booleanKey"));
    }

    @Test
    public void containsKey_whenKeyDoesNotExist_returnsTrue() throws UnexpectedException {
        BraintreeSharedPreferences sut = new BraintreeSharedPreferences(workingSharedPreferences);
        assertFalse(sut.containsKey("booleanKey"));
    }

    @Test(expected = UnexpectedException.class)
    public void containsKey_whenSharedPreferencesNotAccessible_throwsError() throws UnexpectedException {
        BraintreeSharedPreferences sut = new BraintreeSharedPreferences(new UnexpectedException("message"));
        sut.containsKey("booleanKey");
    }

    @Test(expected = UnexpectedException.class)
    public void containsKey_whenSharedPreferencesFails_throwsError() throws UnexpectedException {
        BraintreeSharedPreferences sut = new BraintreeSharedPreferences(failingSharedPreferences);
        sut.containsKey("booleanKey");
    }

    @Test
    public void putStringAndLong_storesStringInSharedPreferences() throws UnexpectedException {
        BraintreeSharedPreferences sut = new BraintreeSharedPreferences(workingSharedPreferences);
        sut.putStringAndLong("stringKey", "stringValue", "longKey", 123L);

        assertEquals("stringValue", sut.getString("stringKey", null));
        assertEquals(123L, sut.getLong("longKey"));
    }

    @Test(expected = UnexpectedException.class)
    public void putStringAndLong_whenSharedPreferencesFails_throwsError() throws UnexpectedException {
        BraintreeSharedPreferences sut = new BraintreeSharedPreferences(failingSharedPreferences);
        sut.putStringAndLong("stringKey", "stringValue", "longKey", 123L);
    }

    @Test(expected = UnexpectedException.class)
    public void putStringAndLong_whenSharedPreferencesNotAccessible_throwsError() throws UnexpectedException {
        BraintreeSharedPreferences sut = new BraintreeSharedPreferences(new UnexpectedException("message"));
        sut.putStringAndLong("stringKey", "stringValue", "longKey", 123L);
    }

    @Test
    public void getLong_returnsZeroByDefault() throws UnexpectedException {
        BraintreeSharedPreferences sut = new BraintreeSharedPreferences(workingSharedPreferences);
        assertEquals(0L, sut.getLong("longKey"));
    }

    @Test(expected = UnexpectedException.class)
    public void getLong_whenSharedPreferencesFails_throwsError() throws UnexpectedException {
        BraintreeSharedPreferences sut = new BraintreeSharedPreferences(failingSharedPreferences);
        sut.getLong("longKey");
    }

    @Test(expected = UnexpectedException.class)
    public void getLong_whenSharedPreferencesNotAccessible_throwsError() throws UnexpectedException {
        BraintreeSharedPreferences sut = new BraintreeSharedPreferences(new UnexpectedException("message"));
        sut.getLong("longKey");
    }

    @Test
    public void clearSharedPreferences_clearsSharedPreferences() throws UnexpectedException {
        BraintreeSharedPreferences sut = new BraintreeSharedPreferences(workingSharedPreferences);

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
    public void clearSharedPreferences_whenSharedPreferencesFails_throwsError() throws UnexpectedException {
        BraintreeSharedPreferences sut = new BraintreeSharedPreferences(failingSharedPreferences);
        sut.clearSharedPreferences();
    }

    @Test(expected = UnexpectedException.class)
    public void clearSharedPreferences_whenSharedPreferencesNotAccessible_throwsError() throws UnexpectedException {
        BraintreeSharedPreferences sut = new BraintreeSharedPreferences(new UnexpectedException("message"));
        sut.clearSharedPreferences();
    }
}