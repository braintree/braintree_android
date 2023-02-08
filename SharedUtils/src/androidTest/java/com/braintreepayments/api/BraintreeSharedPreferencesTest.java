package com.braintreepayments.api;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertFalse;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4ClassRunner.class)
public class BraintreeSharedPreferencesTest {

    private SharedPreferences workingSharedPreferences;

    @Before
    public void beforeEach() {
        Context context = ApplicationProvider.getApplicationContext();
        workingSharedPreferences =
            context.getSharedPreferences("testfilename", Context.MODE_PRIVATE);
    }

    @After
    public void afterEach() {
        workingSharedPreferences.edit().clear().apply();
    }

    @Test
    public void getString_returnsFallbackStringByDefault() {
        BraintreeSharedPreferences sut = new BraintreeSharedPreferences(workingSharedPreferences);
        assertEquals("fallbackValue", sut.getString("stringKey", "fallbackValue"));
    }

    @Test
    public void putString_storesStringInSharedPreferences() {
        BraintreeSharedPreferences sut = new BraintreeSharedPreferences(workingSharedPreferences);
        sut.putString("stringKey", "stringValue");

        assertEquals("stringValue", sut.getString("stringKey", ""));
    }

    @Test
    public void getBoolean_returnsFalseByDefault() {
        BraintreeSharedPreferences sut = new BraintreeSharedPreferences(workingSharedPreferences);
        assertFalse(sut.getBoolean("booleanKey"));
    }

    @Test
    public void putBoolean_storesBooleanInSharedPreferences() {
        BraintreeSharedPreferences sut = new BraintreeSharedPreferences(workingSharedPreferences);
        sut.putBoolean("booleanKey", true);

        assertTrue(sut.getBoolean("booleanKey"));
    }

    @Test
    public void containsKey_whenKeyExists_returnsTrue() {
        BraintreeSharedPreferences sut = new BraintreeSharedPreferences(workingSharedPreferences);
        sut.putBoolean("booleanKey", true);

        assertTrue(sut.containsKey("booleanKey"));
    }

    @Test
    public void containsKey_whenKeyDoesNotExist_returnsTrue() {
        BraintreeSharedPreferences sut = new BraintreeSharedPreferences(workingSharedPreferences);
        assertFalse(sut.containsKey("booleanKey"));
    }

    @Test
    public void putStringAndLong_storesStringInSharedPreferences() {
        BraintreeSharedPreferences sut = new BraintreeSharedPreferences(workingSharedPreferences);
        sut.putStringAndLong("stringKey", "stringValue", "longKey", 123L);

        assertEquals("stringValue", sut.getString("stringKey", null));
        assertEquals(123L, sut.getLong("longKey"));
    }

    @Test
    public void getLong_returnsZeroByDefault() {
        BraintreeSharedPreferences sut = new BraintreeSharedPreferences(workingSharedPreferences);
        assertEquals(0L, sut.getLong("longKey"));
    }

    @Test
    public void clearSharedPreferences_clearsSharedPreferences() {
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
}