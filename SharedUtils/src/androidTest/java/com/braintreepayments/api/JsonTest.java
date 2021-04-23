package com.braintreepayments.api;

import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;

@RunWith(AndroidJUnit4ClassRunner.class)
public class JsonTest {

    private JSONObject json;

    @Before
    public void setup() throws JSONException {
        json = new JSONObject("{\"key\":null}");
    }

    @Test
    public void android_optString_returnsIncorrectNullValue() {
        assertEquals("null", json.optString("key"));
        assertEquals("null", json.optString("key", null));
    }

    @Test
    public void optString_returnsCorrectNullValue() {
        assertNull(Json.optString(json, "key", null));
    }

    @Test
    public void optString_returnsFallback() {
        assertEquals("fallback", Json.optString(json, "key", "fallback"));
    }

    @Test
    public void optString_returnsValue() throws JSONException {
        json = new JSONObject("{\"key\":\"value\"}");

        assertEquals("value", Json.optString(json, "key", "value"));
    }
}
