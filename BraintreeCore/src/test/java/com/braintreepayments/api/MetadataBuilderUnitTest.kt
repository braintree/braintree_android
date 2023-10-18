package com.braintreepayments.api

import org.robolectric.RobolectricTestRunner
import org.json.JSONException
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(RobolectricTestRunner::class)
class MetadataBuilderUnitTest {

    @Test
    @Throws(JSONException::class)
    fun build_correctlyBuildsMetadata() {
        val json = MetadataBuilder()
            .integration("custom")
            .version()
            .sessionId("session-id")
            .source("form")
            .build()
        assertEquals("custom", json.getString("integration"))
        assertEquals("android", json.getString("platform"))
        assertEquals(BuildConfig.VERSION_NAME, json.getString("version"))
        assertEquals("session-id", json.getString("sessionId"))
        assertEquals("form", json.getString("source"))
    }
}
