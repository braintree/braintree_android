package com.braintreepayments.api.core

import org.json.JSONException
import org.json.JSONObject

internal class DeviceMetadata(
    private val appId: String? = null,
    private val appName: String? = null,
    private val clientSDKVersion: String? = null,
    private val clientOs: String? = null,
    private val component: String? = null,
    private val deviceManufacturer: String? = null,
    private val deviceModel: String? = null,
    private val dropInSDKVersion: String? = null,
    private val environment: String? = null,
    private val eventSource: String? = null,
    private val integrationType: IntegrationType? = null,
    private val isSimulator: Boolean = false,
    private val merchantAppVersion: String? = null,
    private val merchantId: String? = null,
    private val platform: String? = null,
    private val sessionId: String? = null,
) {

    @Throws(JSONException::class)
    fun toJSON(): JSONObject {
        return JSONObject()
            .put(APP_ID_KEY, appId)
            .put(APP_NAME_KEY, appName)
            .put(CLIENT_SDK_VERSION_KEY, clientSDKVersion)
            .put(CLIENT_OS_KEY, clientOs)
            .put(COMPONENT_KEY, component)
            .put(DEVICE_MANUFACTURER_KEY, deviceManufacturer)
            .put(DEVICE_MODEL_KEY, deviceModel)
            .put(DROP_IN_SDK_VERSION, dropInSDKVersion)
            .put(EVENT_SOURCE_KEY, eventSource)
            .put(ENVIRONMENT_KEY, environment)
            .put(INTEGRATION_TYPE_KEY, integrationType?.stringValue)
            .put(IS_SIMULATOR_KEY, isSimulator)
            .put(MERCHANT_APP_VERSION_KEY, merchantAppVersion)
            .put(MERCHANT_ID_KEY, merchantId)
            .put(PLATFORM_KEY, platform)
            .put(SESSION_ID_KEY, sessionId)
    }

    companion object {
        private const val APP_ID_KEY = "app_id"
        private const val APP_NAME_KEY = "app_name"
        private const val CLIENT_SDK_VERSION_KEY = "c_sdk_ver"
        private const val CLIENT_OS_KEY = "client_os"
        private const val COMPONENT_KEY = "comp"
        private const val DEVICE_MANUFACTURER_KEY = "device_manufacturer"
        private const val DEVICE_MODEL_KEY = "mobile_device_model"
        private const val DROP_IN_SDK_VERSION = "drop_in_sdk_ver"
        private const val EVENT_SOURCE_KEY = "event_source"
        private const val ENVIRONMENT_KEY = "merchant_sdk_env"
        private const val INTEGRATION_TYPE_KEY = "api_integration_type"
        private const val IS_SIMULATOR_KEY = "is_simulator"
        private const val MERCHANT_APP_VERSION_KEY = "mapv"
        private const val MERCHANT_ID_KEY = "merchant_id"
        private const val PLATFORM_KEY = "platform"
        private const val SESSION_ID_KEY = "session_id"
    }
}
