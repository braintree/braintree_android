package com.braintreepayments.api.core

import org.robolectric.RobolectricTestRunner
import org.json.JSONException
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(RobolectricTestRunner::class)
class MetadataBuilderUnitTest {

    @Test
    @Throws(JSONException::class)
    fun `build assembles integration, platform, version, sessionId and source into json`() {
        val json = MetadataBuilder()
            .integration(IntegrationType.CUSTOM)
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
