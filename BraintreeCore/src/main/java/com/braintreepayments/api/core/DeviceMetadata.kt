package com.braintreepayments.api.core

internal data class DeviceMetadata(
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
    val integrationType: IntegrationType? = null,
    val isSimulator: Boolean = false,
    val merchantAppVersion: String? = null,
    val merchantId: String? = null,
    val platform: String? = null,
    val sessionId: String? = null,
)
