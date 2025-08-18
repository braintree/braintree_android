package com.braintreepayments.api.sharedutils

import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import org.json.JSONException
import org.json.JSONObject
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull

@RunWith(AndroidJUnit4ClassRunner::class)
class JsonTest {

    private lateinit var json: JSONObject

    @Before
    @Throws(JSONException::class)
    fun setup() {
        json = JSONObject("{\"key\":null}")
    }

    @Test
    fun android_optString_returnsIncorrectNullValue() {
        assertEquals("null", json.optString("key"))
    }

    @Test
    fun optString_returnsCorrectNullValue() {
        assertNull(Json.optString(json, "key", null))
    }

    @Test
    fun optString_returnsFallback() {
        assertEquals("fallback", Json.optString(json, "key", "fallback"))
    }

    @Test
    @Throws(JSONException::class)
    fun optString_returnsValue() {
        json = JSONObject("{\"key\":\"value\"}")

        assertEquals("value", Json.optString(json, "key", "value"))
    }
}
