package com.braintreepayments.api

internal data class DeviceMetadata internal constructor(
    val appId: String? = null,
    val appName: String? = null,
    val clientSDKVersion: String? = null,
    val clientOs: String? = null,
    val component: String? = null,
    val deviceManufacturer: String? = null,
    val deviceModel: String? = null,
    val dropInSDKVersion: String? = null,
    val environment: String? = null,
    val eventSource: String? = null,
    val integrationType: String? = null,
    val isSimulator: Boolean = false,
    val merchantAppVersion: String? = null,
    val merchantId: String? = null,
    val platform: String? = null,
    val sessionId: String? = null,
)
