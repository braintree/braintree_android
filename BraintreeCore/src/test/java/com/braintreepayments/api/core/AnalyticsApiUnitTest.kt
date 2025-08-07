package com.braintreepayments.api.core

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.json.JSONObject
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class AnalyticsApiUnitTest {

    private val httpClient = mockk<BraintreeHttpClient>(relaxed = true)
    private val deviceInspector = mockk<DeviceInspector>(relaxed = true)
    private val analyticsParamRepository = mockk<AnalyticsParamRepository>(relaxed = true)
    private val merchantRepository = mockk<MerchantRepository>(relaxed = true)

    private lateinit var sut: AnalyticsApi

    private val configuration = mockk<Configuration>()
    private val tokenizationKey = mockk<TokenizationKey>(relaxed = true)
    private val clientToken = mockk<ClientToken>(relaxed = true)

    private val integrationType = IntegrationType.CUSTOM
    private val deviceMetadata = DeviceMetadata(
        appId = "app-id",
        appName = "app-name",
        clientSDKVersion = "client-sdk-version",
        clientOs = "client-os",
        component = "component",
        deviceManufacturer = "device-manufacturer",
        deviceModel = "device-model",
        dropInSDKVersion = "drop-in-sdk-version",
        environment = "environment",
        eventSource = "event-source",
        integrationType = integrationType,
        isSimulator = false,
        merchantAppVersion = "merchant-app-version",
        merchantId = "merchant-id",
        platform = "platform",
        sessionId = "session-id"
    )

    private val event = AnalyticsEvent(
        name = "event-name",
        timestamp = 123L,
        payPalContextId = "paypal-context-id",
        linkType = "link-type",
        isVaultRequest = true,
        startTime = 123,
        endTime = 234,
        endpoint = "endpoint",
        experiment = "experiment",
        appSwitchUrl = "app-switch-url",
        shopperSessionId = "shopper-session-id",
        buttonType = "button-type",
        buttonOrder = "button-order",
        pageType = "page-type",
        didEnablePayPalAppSwitch = true,
        didPayPalServerAttemptAppSwitch = true,
        errorDescription = "error-description",
        contextType = "EC-TOKEN"
    )

    @Before
    fun setUp() {
        every {
            deviceInspector.getDeviceMetadata(
                context = merchantRepository.applicationContext,
                configuration = configuration,
                sessionId = analyticsParamRepository.sessionId,
                integration = integrationType
            )
        } returns deviceMetadata
        every { deviceInspector.isVenmoInstalled(merchantRepository.applicationContext) } returns false
        every { deviceInspector.isPayPalInstalled() } returns false
        every { merchantRepository.integrationType } returns integrationType

        sut = AnalyticsApi(
            httpClient = httpClient,
            deviceInspector = deviceInspector,
            analyticsParamRepository = analyticsParamRepository,
            merchantRepository = merchantRepository
        )
    }

    @Test
    fun `when execute is called with client token, httpClient post is invoked`() {
        val authFingerprint = "auth-fingerprint"
        every { merchantRepository.authorization } returns clientToken
        every { clientToken.bearer } returns authFingerprint

        val expectedJson = getExpectedJson(clientToken)

        sut.execute(listOf(event), configuration)

        verify {
            httpClient.post(
                path = "https://api-m.paypal.com/v1/tracking/batch/events",
                data = withArg {
                    assertEquals(JSONObject(expectedJson).toString(), JSONObject(it).toString())
                },
                configuration = null,
                authorization = clientToken,
                callback = null
            )
        }
    }

    @Test
    fun `when execute is called with tokenization key, httpClient post is invoked`() {
        val tokenizationKeyBearer = "tokenization-key-bearer"
        every { merchantRepository.authorization } returns tokenizationKey
        every { tokenizationKey.bearer } returns tokenizationKeyBearer

        val expectedJson = getExpectedJson(tokenizationKey)

        sut.execute(listOf(event), configuration)

        verify {
            httpClient.post(
                path = "https://api-m.paypal.com/v1/tracking/batch/events",
                data = withArg {
                    assertEquals(JSONObject(expectedJson).toString(), JSONObject(it).toString())
                },
                configuration = null,
                authorization = tokenizationKey,
                callback = null
            )
        }
    }

    @Suppress("LongMethod")
    private fun getExpectedJson(authorization: Authorization): String {
        return when (authorization) {
            is TokenizationKey -> {
                """
                {
                    "events": [
                        {
                            "batch_params": {
                                "mobile_device_model": "${deviceMetadata.deviceModel}",
                                "comp": "${deviceMetadata.component}",
                                "merchant_sdk_env": "${deviceMetadata.environment}",
                                "venmo_installed": false,
                                "tokenization_key": "${authorization.bearer}",
                                "mapv": "${deviceMetadata.merchantAppVersion}",
                                "session_id": "${deviceMetadata.sessionId}",
                                "c_sdk_ver": "${deviceMetadata.clientSDKVersion}",
                                "event_source": "${deviceMetadata.eventSource}",
                                "merchant_id": "${deviceMetadata.merchantId}",
                                "drop_in_sdk_ver": "${deviceMetadata.dropInSDKVersion}",
                                "paypal_installed": false,
                                "device_manufacturer": "${deviceMetadata.deviceManufacturer}",
                                "is_simulator": ${deviceMetadata.isSimulator},
                                "platform": "${deviceMetadata.platform}",
                                "api_integration_type": "${integrationType.stringValue}",
                                "space_key": "SKDUYK",
                                "product_name": "BT_DCC",
                                "app_name": "${deviceMetadata.appName}",
                                "client_os": "${deviceMetadata.clientOs}",
                                "app_id": "${deviceMetadata.appId}"
                            },
                            "event_params": [
                                {
                                    "tenant_name": "Braintree",
                                    "attempted_app_switch": ${event.didPayPalServerAttemptAppSwitch},
                                    "page_type": "${event.pageType}",
                                    "merchant_enabled_app_switch": ${event.didEnablePayPalAppSwitch},
                                    "button_type": "${event.buttonType}",
                                    "end_time": ${event.endTime},
                                    "is_vault": ${event.isVaultRequest},
                                    "paypal_context_id": "${event.payPalContextId}",
                                    "url": "${event.appSwitchUrl}",
                                    "link_type": "${event.linkType}",
                                    "start_time": ${event.startTime},
                                    "endpoint": "${event.endpoint}",
                                    "t": ${event.timestamp},
                                    "experiment": "${event.experiment}",
                                    "shopper_session_id": "${event.shopperSessionId}",
                                    "error_desc": "${event.errorDescription}",
                                    "event_name": "${event.name}",
                                    "button_position": "${event.buttonOrder}",
                                    "context_type": "EC-TOKEN"
                                }
                            ]
                        }
                    ]
                }
                """
            }

            is ClientToken -> {
                """
                {
                    "events": [
                        {
                            "batch_params": {
                                "mobile_device_model": "${deviceMetadata.deviceModel}",
                                "comp": "${deviceMetadata.component}",
                                "merchant_sdk_env": "${deviceMetadata.environment}",
                                "venmo_installed": false,
                                "mapv": "${deviceMetadata.merchantAppVersion}",
                                "session_id": "${deviceMetadata.sessionId}",
                                "c_sdk_ver": "${deviceMetadata.clientSDKVersion}",
                                "event_source": "${deviceMetadata.eventSource}",
                                "merchant_id": "${deviceMetadata.merchantId}",
                                "drop_in_sdk_ver": "${deviceMetadata.dropInSDKVersion}",
                                "paypal_installed": false,
                                "device_manufacturer": "${deviceMetadata.deviceManufacturer}",
                                "is_simulator": ${deviceMetadata.isSimulator},
                                "api_integration_type": "${integrationType.stringValue}",
                                "platform": "${deviceMetadata.platform}",
                                "space_key": "SKDUYK",
                                "product_name": "BT_DCC",
                                "authorization_fingerprint": "${authorization.bearer}",
                                "app_name": "${deviceMetadata.appName}",
                                "client_os": "${deviceMetadata.clientOs}",
                                "app_id": "${deviceMetadata.appId}"
                            },
                            "event_params": [
                                {
                                    "tenant_name": "Braintree",
                                    "attempted_app_switch": ${event.didPayPalServerAttemptAppSwitch},
                                    "page_type": "${event.pageType}",
                                    "merchant_enabled_app_switch": ${event.didEnablePayPalAppSwitch},
                                    "button_type": "${event.buttonType}",
                                    "end_time": ${event.endTime},
                                    "is_vault": ${event.isVaultRequest},
                                    "paypal_context_id": "${event.payPalContextId}",
                                    "url": "${event.appSwitchUrl}",
                                    "link_type": "${event.linkType}",
                                    "start_time": ${event.startTime},
                                    "endpoint": "${event.endpoint}",
                                    "t": ${event.timestamp},
                                    "experiment": "${event.experiment}",
                                    "shopper_session_id": "${event.shopperSessionId}",
                                    "error_desc": "${event.errorDescription}",
                                    "event_name": "${event.name}",
                                    "button_position": "${event.buttonOrder}",
                                    "context_type": "BA-TOKEN"
                                }
                            ]
                        }
                    ]
                }
                """
            }

            else -> throw IllegalArgumentException("invalid Authorization")
        }
    }
}
