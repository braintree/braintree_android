package com.braintreepayments.api

import org.json.JSONException
import org.json.JSONObject

internal class DeviceMetadata internal constructor(
    private var appVersion: String? = null,
    private var deviceManufacturer: String? = null,
    private var deviceModel: String? = null,
    private var devicePersistentUUID: String? = null,
    private var dropInVersion: String? = null,
    private var integration: String? = null,
    private var isPayPalInstalled: Boolean = false,
    private var isSimulator: Boolean = false,
    private var isVenmoInstalled: Boolean = false,
    private var merchantAppId: String? = null,
    private var merchantAppName: String? = null,
    private var networkType: String? = null,
    private var platform: String? = null,
    private var platformVersion: String? = null,
    private var sdkVersion: String? = null,
    private var sessionId: String? = null,
    private var userOrientation: String? = null
) {

    @Throws(JSONException::class)
    fun toJSON(): JSONObject {
        return JSONObject()
            .put(SESSION_ID_KEY, sessionId)
            .put(INTEGRATION_TYPE_KEY, integration)
            .put(DEVICE_NETWORK_TYPE_KEY, networkType)
            .put(USER_INTERFACE_ORIENTATION_KEY, userOrientation)
            .put(MERCHANT_APP_VERSION_KEY, appVersion)
            .put(PAYPAL_INSTALLED_KEY, isPayPalInstalled)
            .put(VENMO_INSTALLED_KEY, isVenmoInstalled)
            .put(DROP_IN_VERSION_KEY, dropInVersion)
            .put(PLATFORM_KEY, platform)
            .put(PLATFORM_VERSION_KEY, platformVersion)
            .put(SDK_VERSION_KEY, sdkVersion)
            .put(MERCHANT_APP_ID_KEY, merchantAppId)
            .put(MERCHANT_APP_NAME_KEY, merchantAppName)
            .put(DEVICE_MANUFACTURER_KEY, deviceManufacturer)
            .put(DEVICE_MODEL_KEY, deviceModel)
            .put(DEVICE_APP_GENERATED_PERSISTENT_UUID_KEY, devicePersistentUUID)
            .put(IS_SIMULATOR_KEY, isSimulator)
    }

    companion object {
        private const val SESSION_ID_KEY = "sessionId"
        private const val DEVICE_NETWORK_TYPE_KEY = "deviceNetworkType"
        private const val USER_INTERFACE_ORIENTATION_KEY = "userInterfaceOrientation"
        private const val MERCHANT_APP_VERSION_KEY = "merchantAppVersion"
        private const val PAYPAL_INSTALLED_KEY = "paypalInstalled"
        private const val VENMO_INSTALLED_KEY = "venmoInstalled"
        private const val INTEGRATION_TYPE_KEY = "integrationType"
        private const val DROP_IN_VERSION_KEY = "dropinVersion"
        private const val PLATFORM_KEY = "platform"
        private const val PLATFORM_VERSION_KEY = "platformVersion"
        private const val SDK_VERSION_KEY = "sdkVersion"
        private const val MERCHANT_APP_ID_KEY = "merchantAppId"
        private const val MERCHANT_APP_NAME_KEY = "merchantAppName"
        private const val DEVICE_MANUFACTURER_KEY = "deviceManufacturer"
        private const val DEVICE_MODEL_KEY = "deviceModel"
        private const val DEVICE_APP_GENERATED_PERSISTENT_UUID_KEY =
            "deviceAppGeneratedPersistentUuid"
        private const val IS_SIMULATOR_KEY = "isSimulator"
    }
}
