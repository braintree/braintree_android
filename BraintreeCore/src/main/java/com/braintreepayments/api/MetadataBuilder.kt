package com.braintreepayments.api

import androidx.annotation.RestrictTo
import org.json.JSONException
import org.json.JSONObject

/**
 * @suppress
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class MetadataBuilder {

    private val json = JSONObject()

    init {
        try {
            json.put(PLATFORM_KEY, "android")
        } catch (ignored: JSONException) {
        }
    }

    fun source(source: String?): MetadataBuilder {
        try {
            json.put(SOURCE_KEY, source)
        } catch (ignored: JSONException) {
        }
        return this
    }

    fun integration(integration: String?): MetadataBuilder {
        try {
            json.put(INTEGRATION_KEY, integration)
        } catch (ignored: JSONException) {
        }
        return this
    }

    fun sessionId(sessionId: String?): MetadataBuilder {
        try {
            json.put(SESSION_ID_KEY, sessionId)
        } catch (ignored: JSONException) {
        }
        return this
    }

    fun version(): MetadataBuilder {
        try {
            json.put(VERSION_KEY, BuildConfig.VERSION_NAME)
        } catch (ignored: JSONException) {
        }
        return this
    }

    fun build(): JSONObject {
        return json
    }

    override fun toString(): String {
        return json.toString()
    }

    companion object {
        const val META_KEY = "_meta"
        private const val SOURCE_KEY = "source"
        private const val INTEGRATION_KEY = "integration"
        private const val SESSION_ID_KEY = "sessionId"
        private const val VERSION_KEY = "version"
        private const val PLATFORM_KEY = "platform"
    }
}
