package com.braintreepayments.api

internal class DeviceMetadata internal constructor(
    var appId: String? = null,
    var appName: String? = null,
    var clientSDKVersion: String? = null,
    var clientOS: String? = null,
    var component: String? = null,
    var deviceManufacturer: String? = null,
    var deviceModel: String? = null,
    var dropInSDKVersion: String? = null,
    var eventSource: String? = null,
    var isSimulator: Boolean = false,
    var merchantAppVersion: String? = null,
    var platform: String? = null,
)
