package com.braintreepayments.api.core

import com.braintreepayments.api.core.Configuration.Companion.fromJson
import com.braintreepayments.api.sharedutils.Time
import com.braintreepayments.api.testutils.Fixtures
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
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

    private lateinit var sut: AnalyticsClient

    private val analyticsEventParams = AnalyticsEventParams(
        payPalContextId = "sample-paypal-context-id",
        isVaultRequest = true,
        startTime = 789,
        endTime = 987,
        endpoint = "fake-endpoint",
        experiment = "fake-experiment",
        appSwitchUrl = "app-switch-url",
        shopperSessionId = "shopper-session-id",
        buttonType = "button-type",
        buttonOrder = "button-order",
        pageType = "page-type",
    )

    private val expectedAnalyticsEvent = AnalyticsEvent(
        name = eventName,
        timestamp = timestamp,
        payPalContextId = analyticsEventParams.payPalContextId,
        linkType = linkType.stringValue,
        isVaultRequest = analyticsEventParams.isVaultRequest,
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
    )

    @Before
    @Throws(InvalidArgumentException::class, GeneralSecurityException::class, IOException::class)
    fun beforeEach() {
        every { time.currentTime } returns timestamp
        every { analyticsParamRepository.linkType } returns linkType
        every { analyticsParamRepository.didEnablePayPalAppSwitch } returns true
        every { analyticsParamRepository.didPayPalServerAttemptAppSwitch } returns true

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

    @Test
    fun `when sendEvent is called with sendImmediately as true, the events api is executed`() {
        sut.sendEvent(
            eventName = eventName,
            analyticsEventParams = analyticsEventParams,
            sendImmediately = true
        )

        verify {
            analyticsApi.execute(
                events = listOf(expectedAnalyticsEvent),
                configuration = configuration
            )
        }
    }

    @Test
    fun `when sendEvent is called with sendImmediately as true, all events in the analyticsEventRepository are sent`() {
        every { analyticsEventRepository.flushAndReturnEvents() } returns listOf(expectedAnalyticsEvent)

        sut.sendEvent(
            eventName = "initial-event",
            analyticsEventParams = analyticsEventParams,
            sendImmediately = true
        )

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
