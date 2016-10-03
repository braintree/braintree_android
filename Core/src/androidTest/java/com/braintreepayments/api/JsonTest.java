package com.braintreepayments.api;

import android.support.test.runner.AndroidJUnit4;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static junit.framework.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
public class JsonTest {

    private JSONObject mJson;

    @Before
    public void setup() throws JSONException {
        mJson = new JSONObject("{\"key\":null}");
    }

    @Test
    public void android_optString_returnsIncorrectNullValue() {
        assertEquals("null", mJson.optString("key"));
        assertEquals("null", mJson.optString("key", null));
    }

    @Test
    public void optString_returnsCorrectNullValue() {
        assertEquals(null, Json.optString(mJson, "key", null));
    }

    @Test
    public void optString_returnsFallback() {
        assertEquals("fallback", Json.optString(mJson, "key", "fallback"));
    }

    @Test
    public void optString_returnsValue() throws JSONException {
        mJson = new JSONObject("{\"key\":\"value\"}");

        assertEquals("value", Json.optString(mJson, "key", "value"));
    }
}
