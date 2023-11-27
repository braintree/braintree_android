package com.braintreepayments.api

import org.json.JSONException
import org.json.JSONObject

internal class DeviceMetadata internal constructor(
    private var appId: String? = null,
    private var appName: String? = null,
    private var clientSDKVersion: String? = null,
    private var clientOs: String? = null,
    private var deviceManufacturer: String? = null,
    private var deviceModel: String? = null,
    private var environment: String? = null,
    private var integrationType: String? = null,
    private var isSimulator: Boolean = false,
    private var merchantAppVersion: String? = null,
    private var merchantId: String? = null,
    private var platform: String? = null,
    private var sessionId: String? = null,
) {

    @Throws(JSONException::class)
    fun toJSON(): JSONObject {
        return JSONObject()
            .put(APP_ID_KEY, appId)
            .put(APP_NAME_KEY, appName)
             // AUTH FING TODO
            .put(CLIENT_SDK_VERSION_KEY, clientSDKVersion)
            .put(CLIENT_OS_KEY, clientOs)
            .put(COMPONENT_KEY, "braintreeclientsdk")
            .put(DEVICE_MANUFACTURER_KEY, deviceManufacturer)
            .put(DEVICE_MODEL_KEY, deviceModel)
            .put(EVENT_SOURCE_KEY, "mobile-native")
            .put(ENVIRONMENT_KEY, environment)
            .put(INTEGRATION_TYPE_KEY, integrationType)
            .put(IS_SIMULATOR_KEY, isSimulator)
            .put(MERCHANT_APP_VERSION_KEY, merchantAppVersion)
            .put(MERCHANT_ID_KEY, merchantId)
            .put(PLATFORM_KEY, platform)
            .put(SESSION_ID_KEY, sessionId)
            // TOKEN KEY TODO
    }

    companion object {
        private const val APP_ID_KEY = "app_id"
        private const val APP_NAME_KEY = "app_name"
        private const val AUTHORIZATION_FINGERPRINT_KEY = "auth_fingerprint"
        private const val CLIENT_SDK_VERSION_KEY = "c_sdk_ver"
        private const val CLIENT_OS_KEY = "client_os"
        private const val COMPONENT_KEY = "comp"
        private const val DEVICE_MANUFACTURER_KEY = "device_manufacturer"
        private const val DEVICE_MODEL_KEY = "deviceModel"
        private const val EVENT_SOURCE_KEY = "event_source"
        private const val ENVIRONMENT_KEY = "merchant_sdk_env"
        private const val INTEGRATION_TYPE_KEY = "api_integration_type"
        private const val IS_SIMULATOR_KEY = "is_simulator"
        private const val MERCHANT_APP_VERSION_KEY = "mapv"
        private const val MERCHANT_ID_KEY = "merchant_id"
        private const val PLATFORM_KEY = "platform"
        private const val SESSION_ID_KEY = "session_id"
        private const val TOKENIZATION_KEY = "tokenization_key"
    }
}
