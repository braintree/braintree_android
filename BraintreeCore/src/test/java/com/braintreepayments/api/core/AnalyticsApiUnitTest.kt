package com.braintreepayments.api.core

import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
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

    private val clientTokenEvent = AnalyticsEvent(
        name = "event-name",
        timestamp = 123L,
        contextId = "context-id",
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
        didSdkAttemptAppSwitch = true,
        errorDescription = "error-description",
        fundingSource = "funding-source"
    )

    private val tokenizationKeyEvent = AnalyticsEvent(
        name = "event-name",
        timestamp = 123L,
        contextId = "context-id",
        linkType = "link-type",
        isVaultRequest = false,
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
        didSdkAttemptAppSwitch = true,
        errorDescription = "error-description",
        fundingSource = "funding-source"
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
    }

    private fun createAnalyticsApi(
        testDispatcher: kotlinx.coroutines.CoroutineDispatcher? = null,
        testScope: kotlinx.coroutines.CoroutineScope? = null
    ) = AnalyticsApi(
        httpClient = httpClient,
        deviceInspector = deviceInspector,
        analyticsParamRepository = analyticsParamRepository,
        merchantRepository = merchantRepository,
        dispatcher = testDispatcher ?: kotlinx.coroutines.Dispatchers.Main,
        coroutineScope = testScope ?: kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main)
    )

    @Test
    fun `when execute is called with client token, httpClient post is invoked`() = runTest {
        val authFingerprint = "auth-fingerprint"
        every { merchantRepository.authorization } returns clientToken
        every { clientToken.bearer } returns authFingerprint

        val expectedJson = getExpectedJson(clientToken)

        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)
        sut = createAnalyticsApi(testDispatcher, testScope)

        sut.execute(listOf(clientTokenEvent), configuration)
        advanceUntilIdle()

        coVerify {
            httpClient.post(
                path = "https://api-m.paypal.com/v1/tracking/batch/events",
                data = withArg {
                    assertEquals(JSONObject(expectedJson).toString(), JSONObject(it).toString())
                },
                configuration = null,
                authorization = clientToken
            )
        }
    }

    @Test
    fun `when execute is called with tokenization key, httpClient post is invoked`() = runTest {
        val tokenizationKeyBearer = "tokenization-key-bearer"
        every { merchantRepository.authorization } returns tokenizationKey
        every { tokenizationKey.bearer } returns tokenizationKeyBearer

        val expectedJson = getExpectedJson(tokenizationKey)

        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)
        sut = createAnalyticsApi(testDispatcher, testScope)

        sut.execute(listOf(tokenizationKeyEvent), configuration)
        advanceUntilIdle()

        coVerify {
            httpClient.post(
                path = "https://api-m.paypal.com/v1/tracking/batch/events",
                data = withArg {
                    assertEquals(JSONObject(expectedJson).toString(), JSONObject(it).toString())
                },
                configuration = null,
                authorization = tokenizationKey
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
                                    "attempted_app_switch": ${tokenizationKeyEvent.didSdkAttemptAppSwitch},
                                    "page_type": "${tokenizationKeyEvent.pageType}",
                                    "merchant_enabled_app_switch": ${tokenizationKeyEvent.didEnablePayPalAppSwitch},
                                    "button_type": "${tokenizationKeyEvent.buttonType}",
                                    "end_time": ${tokenizationKeyEvent.endTime},
                                    "is_vault": ${tokenizationKeyEvent.isVaultRequest},
                                    "context_id": "${tokenizationKeyEvent.contextId}",
                                    "url": "${tokenizationKeyEvent.appSwitchUrl}",
                                    "link_type": "${tokenizationKeyEvent.linkType}",
                                    "start_time": ${tokenizationKeyEvent.startTime},
                                    "endpoint": "${tokenizationKeyEvent.endpoint}",
                                    "t": ${tokenizationKeyEvent.timestamp},
                                    "experiment": "${tokenizationKeyEvent.experiment}",
                                    "shopper_session_id": "${tokenizationKeyEvent.shopperSessionId}",
                                    "error_desc": "${tokenizationKeyEvent.errorDescription}",
                                    "funding_source": "${tokenizationKeyEvent.fundingSource}",
                                    "event_name": "${tokenizationKeyEvent.name}",
                                    "button_position": "${tokenizationKeyEvent.buttonOrder}",
                                    "context_type": "EC-TOKEN",
                                    "paypal_app_switch_url_received": ${tokenizationKeyEvent.didPayPalServerAttemptAppSwitch}
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
                                    "attempted_app_switch": ${clientTokenEvent.didSdkAttemptAppSwitch},
                                    "page_type": "${clientTokenEvent.pageType}",
                                    "merchant_enabled_app_switch": ${clientTokenEvent.didEnablePayPalAppSwitch},
                                    "button_type": "${clientTokenEvent.buttonType}",
                                    "end_time": ${clientTokenEvent.endTime},
                                    "is_vault": ${clientTokenEvent.isVaultRequest},
                                    "context_id": "${clientTokenEvent.contextId}",
                                    "url": "${clientTokenEvent.appSwitchUrl}",
                                    "link_type": "${clientTokenEvent.linkType}",
                                    "start_time": ${clientTokenEvent.startTime},
                                    "endpoint": "${clientTokenEvent.endpoint}",
                                    "t": ${clientTokenEvent.timestamp},
                                    "experiment": "${clientTokenEvent.experiment}",
                                    "shopper_session_id": "${clientTokenEvent.shopperSessionId}",
                                    "error_desc": "${clientTokenEvent.errorDescription}",
                                    "funding_source": "${clientTokenEvent.fundingSource}",
                                    "event_name": "${clientTokenEvent.name}",
                                    "button_position": "${clientTokenEvent.buttonOrder}",
                                    "context_type": "BA-TOKEN",
                                    "paypal_app_switch_url_received": ${clientTokenEvent.didPayPalServerAttemptAppSwitch}
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
