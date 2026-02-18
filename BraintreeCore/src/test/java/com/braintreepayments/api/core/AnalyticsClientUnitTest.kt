package com.braintreepayments.api.core

import com.braintreepayments.api.core.Configuration.Companion.fromJson
import com.braintreepayments.api.paypal.PayPalRecurringBillingPlanType
import com.braintreepayments.api.sharedutils.Time
import com.braintreepayments.api.testutils.Fixtures
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException
import java.security.GeneralSecurityException

@RunWith(RobolectricTestRunner::class)
class AnalyticsClientUnitTest {

    private val analyticsApi: AnalyticsApi = mockk(relaxed = true)
    private val analyticsEventRepository: AnalyticsEventRepository = mockk(relaxed = true)
    private val analyticsParamRepository: AnalyticsParamRepository = mockk(relaxed = true)
    private val time: Time = mockk()
    private lateinit var configurationLoader: ConfigurationLoader

    private val configuration: Configuration = fromJson(Fixtures.CONFIGURATION_WITH_ENVIRONMENT)
    private val eventName = "sample-event-name"
    private val timestamp = 123L
    private val linkType = LinkType.APP_LINK
    private val fundingSource = "paypal"
    private val isBillingAgreement = true
    private val isPurchase = true
    private val billingPlanType = PayPalRecurringBillingPlanType.RECURRING.name

    private lateinit var sut: AnalyticsClient

    private val analyticsEventParams = AnalyticsEventParams(
        contextId = "sample-paypal-context-id",
        isVaultRequest = true,
        startTime = 789,
        endTime = 987,
        endpoint = "fake-endpoint",
        experiment = "fake-experiment",
        appSwitchUrl = "app-switch-url",
        shopperSessionId = "shopper-session-id",
        buttonType = "button-type",
        buttonOrder = "button-order",
        pageType = "page-type"
    )

    private val expectedAnalyticsEvent = AnalyticsEvent(
        name = eventName,
        timestamp = timestamp,
        contextId = analyticsEventParams.contextId,
        linkType = linkType.stringValue,
        isVaultRequest = analyticsEventParams.isVaultRequest,
        isPurchaseFlow = isPurchase,
        recurringBillingPlanType = billingPlanType,
        shouldRequestBillingAgreement = isBillingAgreement,
        startTime = analyticsEventParams.startTime,
        endTime = analyticsEventParams.endTime,
        endpoint = analyticsEventParams.endpoint,
        experiment = analyticsEventParams.experiment,
        appSwitchUrl = analyticsEventParams.appSwitchUrl,
        shopperSessionId = analyticsEventParams.shopperSessionId,
        buttonType = analyticsEventParams.buttonType,
        buttonOrder = analyticsEventParams.buttonOrder,
        pageType = analyticsEventParams.pageType,
        didEnablePayPalAppSwitch = true,
        didPayPalServerAttemptAppSwitch = true,
        didSdkAttemptAppSwitch = true,
        fundingSource = fundingSource
    )

    @Before
    @Throws(InvalidArgumentException::class, GeneralSecurityException::class, IOException::class)
    fun beforeEach() {
        every { time.currentTime } returns timestamp
        every { analyticsParamRepository.linkType } returns linkType
        every { analyticsParamRepository.didEnablePayPalAppSwitch } returns true
        every { analyticsParamRepository.didPayPalServerAttemptAppSwitch } returns true
        every { analyticsParamRepository.didSdkAttemptAppSwitch } returns true
        every { analyticsParamRepository.fundingSource } returns fundingSource
        every { analyticsParamRepository.shouldRequestBillingAgreement } returns isBillingAgreement
        every { analyticsParamRepository.isPurchaseFlow } returns isPurchase
        every { analyticsParamRepository.recurringBillingPlanType } returns billingPlanType

        configurationLoader = MockkConfigurationLoaderBuilder()
            .configuration(configuration)
            .build()

        sut = AnalyticsClient(
            analyticsApi = analyticsApi,
            analyticsParamRepository = analyticsParamRepository,
            analyticsEventRepository = analyticsEventRepository,
            time = time,
            configurationLoader = configurationLoader,
        )
    }

    @Test
    fun `when sendEvent is called with sendImmediately as false, event is added to the analyticsEventRepository`() {
        sut.sendEvent(
            eventName = eventName,
            analyticsEventParams = analyticsEventParams,
            sendImmediately = false
        )

        verify { analyticsEventRepository.addEvent(expectedAnalyticsEvent) }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `when sendEvent is called with sendImmediately as true, the events api is executed`() = runTest {
            val testDispatcher = StandardTestDispatcher(testScheduler)
            val testScope = TestScope(testDispatcher)
            val sut = AnalyticsClient(
                analyticsApi = analyticsApi,
                analyticsParamRepository = analyticsParamRepository,
                analyticsEventRepository = analyticsEventRepository,
                time = time,
                configurationLoader = configurationLoader,
                dispatcher = testDispatcher,
                coroutineScope = testScope
            )

            sut.sendEvent(
                eventName = eventName,
                analyticsEventParams = analyticsEventParams,
                sendImmediately = true
            )
            advanceUntilIdle()

            verify {
                analyticsApi.execute(
                    events = listOf(expectedAnalyticsEvent),
                    configuration = configuration
                )
            }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `when sendEvent is called with sendImmediately as true, all events in the analyticsEventRepository are sent`() =
    runTest {

        every { analyticsEventRepository.flushAndReturnEvents() } returns listOf(expectedAnalyticsEvent)

        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)
        val sut = AnalyticsClient(
            analyticsApi = analyticsApi,
            analyticsParamRepository = analyticsParamRepository,
            analyticsEventRepository = analyticsEventRepository,
            time = time,
            configurationLoader = configurationLoader,
            dispatcher = testDispatcher,
            coroutineScope = testScope
        )

        sut.sendEvent(
            eventName = "initial-event",
            analyticsEventParams = analyticsEventParams,
            sendImmediately = true
        )
        advanceUntilIdle()

        val initialEvent = expectedAnalyticsEvent.copy(name = "initial-event")

        verify {
            analyticsApi.execute(
                events = listOf(expectedAnalyticsEvent, initialEvent),
                configuration = configuration
            )
        }
    }

    @Test
    fun `when reportCrash is called, the events api is executed`() {
        val expectedCrashEvent = AnalyticsEvent(
            name = "crash",
            timestamp = timestamp
        )

        sut.reportCrash(configuration)

        verify {
            analyticsApi.execute(
                events = listOf(expectedCrashEvent),
                configuration = configuration
            )
        }
    }
}
