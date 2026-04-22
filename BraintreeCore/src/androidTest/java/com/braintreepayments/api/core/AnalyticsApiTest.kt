package com.braintreepayments.api.core

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import com.braintreepayments.api.testutils.Fixtures
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4ClassRunner::class)
class AnalyticsApiTest {

    private lateinit var context: Context
    private lateinit var configuration: Configuration
    private lateinit var merchantRepository: MerchantRepository
    private lateinit var analyticsParamRepository: AnalyticsParamRepository
    private lateinit var sut: AnalyticsApi

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        configuration = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_ENVIRONMENT)

        SdkComponent.create(context)

        merchantRepository = MerchantRepository.instance
        merchantRepository.applicationContext = context
        merchantRepository.authorization = Authorization.fromString(Fixtures.TOKENIZATION_KEY)
        merchantRepository.integrationType = IntegrationType.CUSTOM

        analyticsParamRepository = AnalyticsParamRepository.instance
        analyticsParamRepository.reset()

        sut = AnalyticsApi(
            deviceInspector = DeviceInspector(context),
            analyticsParamRepository = analyticsParamRepository,
            merchantRepository = merchantRepository
        )
    }

    @Test(timeout = 5000)
    fun execute_withSingleEvent_doesNotCrash() {
        val event = AnalyticsEvent(
            name = "test.event.started",
            timestamp = System.currentTimeMillis()
        )

        sut.execute(listOf(event), configuration)
    }

    @Test(timeout = 5000)
    fun execute_withMultipleEvents_doesNotCrash() {
        val events = listOf(
            AnalyticsEvent(name = "test.event.started", timestamp = System.currentTimeMillis()),
            AnalyticsEvent(name = "test.event.succeeded", timestamp = System.currentTimeMillis()),
            AnalyticsEvent(name = "test.event.failed", timestamp = System.currentTimeMillis())
        )

        sut.execute(events, configuration)
    }

    @Test(timeout = 5000)
    fun execute_withFullyPopulatedEvent_doesNotCrash() {
        val event = AnalyticsEvent(
            name = "paypal.tokenize.started",
            timestamp = System.currentTimeMillis(),
            contextId = "test-context-id",
            linkType = "universal",
            isVaultRequest = true,
            shouldRequestBillingAgreement = true,
            recurringBillingPlanType = "RECURRING",
            startTime = System.currentTimeMillis() - 1000,
            endTime = System.currentTimeMillis(),
            endpoint = "/v1/paypal_hermes/setup_billing_agreement",
            experiment = "{\"test\":\"value\"}",
            appSwitchUrl = "https://example.com/switch",
            shopperSessionId = "shopper-session-123",
            buttonType = "paypal",
            buttonOrder = "1",
            pageType = "checkout",
            errorDescription = "test error",
            didEnablePayPalAppSwitch = true,
            didPayPalServerAttemptAppSwitch = true,
            didSdkAttemptAppSwitch = false,
            fundingSource = "paypal",
            uiType = "one-touch"
        )

        sut.execute(listOf(event), configuration)
    }

    @Test(timeout = 5000)
    fun execute_withNullConfiguration_doesNotCrash() {
        val event = AnalyticsEvent(
            name = "test.event.started",
            timestamp = System.currentTimeMillis()
        )

        sut.execute(listOf(event), null)
    }

    @Test(timeout = 5000)
    fun execute_withEmptyEventList_doesNotCrash() {
        sut.execute(emptyList(), configuration)
    }

    @Test(timeout = 5000)
    fun execute_withClientTokenAuthorization_doesNotCrash() {
        merchantRepository.authorization = Authorization.fromString(Fixtures.BASE64_CLIENT_TOKEN)

        val event = AnalyticsEvent(
            name = "test.event.with.client.token",
            timestamp = System.currentTimeMillis()
        )

        sut.execute(listOf(event), configuration)
    }

    @Test(timeout = 5000)
    fun execute_withAnalyticsParamsPopulated_doesNotCrash() {
        analyticsParamRepository.linkType = LinkType.APP_LINK
        analyticsParamRepository.didEnablePayPalAppSwitch = true
        analyticsParamRepository.didPayPalServerAttemptAppSwitch = false
        analyticsParamRepository.didSdkAttemptAppSwitch = true
        analyticsParamRepository.fundingSource = "venmo"
        analyticsParamRepository.shouldRequestBillingAgreement = true
        analyticsParamRepository.recurringBillingPlanType = "INSTALLMENT"

        val event = AnalyticsEvent(
            name = "test.event.with.params",
            timestamp = System.currentTimeMillis()
        )

        sut.execute(listOf(event), configuration)
    }
}