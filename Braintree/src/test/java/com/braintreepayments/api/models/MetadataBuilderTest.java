package com.braintreepayments.api.models;

import com.braintreepayments.api.BuildConfig;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;

import static junit.framework.Assert.assertEquals;

@RunWith(RobolectricGradleTestRunner.class)
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
