package com.braintreepayments.api;

import com.braintreepayments.api.models.BuildConfig;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static junit.framework.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
public class MetadataBuilderTest {

    @Test
    public void build_correctlyBuildsMetadata() throws JSONException {
        JSONObject json = new MetadataBuilder()
                .integration("custom")
                .version()
                .sessionId("session-id")
                .source("form")
                .build();

        assertEquals("custom", json.getString("integration"));
        assertEquals("android", json.getString("platform"));
        assertEquals(BuildConfig.VERSION_NAME, json.getString("version"));
        assertEquals("session-id", json.getString("sessionId"));
        assertEquals("form", json.getString("source"));
    }
}
