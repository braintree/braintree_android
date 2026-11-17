package com.braintreepayments.api.core

import com.braintreepayments.api.core.Configuration.Companion.fromJson
import com.braintreepayments.api.paypal.PayPalRecurringBillingPlanType
import com.braintreepayments.api.sharedutils.Time
import com.braintreepayments.api.testutils.Fixtures
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
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

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class AnalyticsClientUnitTest {

    private val analyticsApi: AnalyticsApi = mockk(relaxed = true)
    private val analyticsEventRepository: AnalyticsEventRepository = mockk(relaxed = true)
    private val analyticsParamRepository: AnalyticsParamRepository = mockk(relaxed = true)
    private val time: Time = mockk()
    private val merchantRepository: MerchantRepository = mockk(relaxed = true)

    private val configuration: Configuration = fromJson(Fixtures.CONFIGURATION_WITH_ENVIRONMENT)
    private val otherConfiguration: Configuration = fromJson(Fixtures.CONFIGURATION_WITH_ACCESS_TOKEN)
    private val authorization = Authorization.fromString(Fixtures.TOKENIZATION_KEY)
    private val otherAuthorization = Authorization.fromString(Fixtures.PROD_TOKENIZATION_KEY)
    private val eventName = "sample-event-name"
    private val timestamp = 123L
    private val linkType = LinkType.APP_LINK
    private val fundingSource = "paypal"
    private val uiType = "compose"
    private val isBillingAgreement = true
    private val isPurchase = true
    private val billingPlanType = PayPalRecurringBillingPlanType.RECURRING.name

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
        pageType = "page-type",
        uiType = "compose"
    )

    private val expectedAnalyticsEvent = AnalyticsEvent(
        name = eventName,
        timestamp = timestamp,
        contextId = analyticsEventParams.contextId,
        linkType = linkType.stringValue,
        isVaultRequest = analyticsEventParams.isVaultRequest,
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
        fundingSource = fundingSource,
        uiType = uiType
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
        every { analyticsParamRepository.recurringBillingPlanType } returns billingPlanType
    }

    private fun createSut(
        configurationLoader: ConfigurationLoader,
        dispatcher: CoroutineDispatcher,
        coroutineScope: CoroutineScope,
    ) = AnalyticsClient(
        analyticsApi = analyticsApi,
        analyticsParamRepository = analyticsParamRepository,
        analyticsEventRepository = analyticsEventRepository,
        time = time,
        configurationLoader = configurationLoader,
        merchantRepository = merchantRepository,
        dispatcher = dispatcher,
        coroutineScope = coroutineScope,
    )

    @Test
    fun `when sendEvent is called with sendImmediately as false, event is added to the analyticsEventRepository`() {
        val configurationLoader = MockkConfigurationLoaderBuilder()
            .configuration(configuration)
            .build()
        val sut = createSut(configurationLoader, testDispatcher(), testScope())

        sut.sendEvent(
            eventName = eventName,
            authorization = authorization,
            analyticsEventParams = analyticsEventParams,
            sendImmediately = false
        )

        verify { analyticsEventRepository.addEvent(expectedAnalyticsEvent, authorization) }
        coVerify(exactly = 0) { configurationLoader.loadConfiguration(any()) }
        verify(exactly = 0) { analyticsApi.execute(any(), any(), any()) }
    }

    @Test
    fun `when sendEvent is called without an authorization, the shim uses the merchantRepository authorization`() {
        every { merchantRepository.authorization } returns authorization
        val configurationLoader = MockkConfigurationLoaderBuilder()
            .configuration(configuration)
            .build()
        val sut = createSut(configurationLoader, testDispatcher(), testScope())

        sut.sendEvent(
            eventName = eventName,
            analyticsEventParams = analyticsEventParams,
            sendImmediately = false
        )

        verify { analyticsEventRepository.addEvent(expectedAnalyticsEvent, authorization) }
    }

    @Test
    fun `when sendEvent is called with sendImmediately as true, the events api is executed`() = runTest {
        val sut = createSut(
            MockkConfigurationLoaderBuilder().configuration(configuration).build(),
            StandardTestDispatcher(testScheduler),
            TestScope(StandardTestDispatcher(testScheduler))
        )

        sut.sendEvent(
            eventName = eventName,
            authorization = authorization,
            analyticsEventParams = analyticsEventParams
        )
        advanceUntilIdle()

        verify {
            analyticsApi.execute(
                events = listOf(expectedAnalyticsEvent),
                configuration = configuration,
                authorization = authorization
            )
        }
    }

    @Test
    fun `when sendEvent is called with sendImmediately as true, configuration loads before events are flushed`() =
        runTest {
            val configurationLoader = MockkConfigurationLoaderBuilder()
                .configuration(configuration)
                .build()
            val sut = createSut(
                configurationLoader,
                StandardTestDispatcher(testScheduler),
                TestScope(StandardTestDispatcher(testScheduler))
            )

            sut.sendEvent(
                eventName = eventName,
                authorization = authorization,
                analyticsEventParams = analyticsEventParams
            )
            advanceUntilIdle()

            coVerifyOrder {
                configurationLoader.loadConfiguration(authorization)
                analyticsEventRepository.flushAndReturnEvents()
            }
        }

    @Test
    fun `when the trigger's configuration load fails, nothing is flushed or executed`() = runTest {
        val configurationLoader = MockkConfigurationLoaderBuilder()
            .configurationError(Exception("config error"))
            .build()
        val sut = createSut(
            configurationLoader,
            StandardTestDispatcher(testScheduler),
            TestScope(StandardTestDispatcher(testScheduler))
        )

        sut.sendEvent(
            eventName = eventName,
            authorization = authorization,
            analyticsEventParams = analyticsEventParams
        )
        advanceUntilIdle()

        verify(exactly = 0) { analyticsEventRepository.flushAndReturnEvents() }
        verify(exactly = 0) { analyticsApi.execute(any(), any(), any()) }
    }

    @Test
    fun `when the trigger's configuration load is cancelled, nothing is sent and a later send still works`() =
        runTest {
            val configurationLoader = mockk<ConfigurationLoader> {
                coEvery { loadConfiguration(authorization) } throws
                    kotlin.coroutines.cancellation.CancellationException("cancelled") andThen
                    ConfigurationLoaderResult.Success(configuration)
            }
            val sut = createSut(
                configurationLoader,
                StandardTestDispatcher(testScheduler),
                TestScope(StandardTestDispatcher(testScheduler))
            )

            sut.sendEvent(
                eventName = eventName,
                authorization = authorization,
                analyticsEventParams = analyticsEventParams
            )
            advanceUntilIdle()

            verify(exactly = 0) { analyticsEventRepository.flushAndReturnEvents() }
            verify(exactly = 0) { analyticsApi.execute(any(), any(), any()) }

            sut.sendEvent(
                eventName = eventName,
                authorization = authorization,
                analyticsEventParams = analyticsEventParams
            )
            advanceUntilIdle()

            verify(exactly = 1) {
                analyticsApi.execute(
                    events = listOf(expectedAnalyticsEvent),
                    configuration = configuration,
                    authorization = authorization
                )
            }
        }

    @Test
    fun `when queued events belong to the trigger's authorization, they are sent with it in one execute`() =
        runTest {
            val queuedEvent = expectedAnalyticsEvent.copy(name = "queued-event")
            every { analyticsEventRepository.flushAndReturnEvents() } returns listOf(
                AnalyticsEventBatch(authorization).apply { events.add(queuedEvent) }
            )
            val sut = createSut(
                MockkConfigurationLoaderBuilder().configuration(configuration).build(),
                StandardTestDispatcher(testScheduler),
                TestScope(StandardTestDispatcher(testScheduler))
            )

            sut.sendEvent(
                eventName = eventName,
                authorization = authorization,
                analyticsEventParams = analyticsEventParams
            )
            advanceUntilIdle()

            verify {
                analyticsApi.execute(
                    events = listOf(queuedEvent, expectedAnalyticsEvent),
                    configuration = configuration,
                    authorization = authorization
                )
            }
        }

    @Test
    fun `when queued events belong to another authorization, they are executed with their own configuration`() =
        runTest {
            val queuedEvent = expectedAnalyticsEvent.copy(name = "queued-other-event")
            every { analyticsEventRepository.flushAndReturnEvents() } returns listOf(
                AnalyticsEventBatch(otherAuthorization).apply { events.add(queuedEvent) }
            )
            val configurationLoader = mockk<ConfigurationLoader> {
                coEvery { loadConfiguration(authorization) } returns
                    ConfigurationLoaderResult.Success(configuration)
                coEvery { loadConfiguration(otherAuthorization) } returns
                    ConfigurationLoaderResult.Success(otherConfiguration)
            }
            val sut = createSut(
                configurationLoader,
                StandardTestDispatcher(testScheduler),
                TestScope(StandardTestDispatcher(testScheduler))
            )

            sut.sendEvent(
                eventName = eventName,
                authorization = authorization,
                analyticsEventParams = analyticsEventParams
            )
            advanceUntilIdle()

            verify {
                analyticsApi.execute(
                    events = listOf(expectedAnalyticsEvent),
                    configuration = configuration,
                    authorization = authorization
                )
            }
            verify {
                analyticsApi.execute(
                    events = listOf(queuedEvent),
                    configuration = otherConfiguration,
                    authorization = otherAuthorization
                )
            }
        }

    @Test
    fun `when another authorization's configuration load fails, its batch is dropped and the trigger is sent`() =
        runTest {
            every { analyticsEventRepository.flushAndReturnEvents() } returns listOf(
                AnalyticsEventBatch(otherAuthorization).apply {
                    events.add(expectedAnalyticsEvent.copy(name = "queued-other-event"))
                }
            )
            val configurationLoader = mockk<ConfigurationLoader> {
                coEvery { loadConfiguration(authorization) } returns
                    ConfigurationLoaderResult.Success(configuration)
                coEvery { loadConfiguration(otherAuthorization) } returns
                    ConfigurationLoaderResult.Failure(Exception("load failed"))
            }
            val sut = createSut(
                configurationLoader,
                StandardTestDispatcher(testScheduler),
                TestScope(StandardTestDispatcher(testScheduler))
            )

            sut.sendEvent(
                eventName = eventName,
                authorization = authorization,
                analyticsEventParams = analyticsEventParams
            )
            advanceUntilIdle()

            verify {
                analyticsApi.execute(
                    events = listOf(expectedAnalyticsEvent),
                    configuration = configuration,
                    authorization = authorization
                )
            }
            verify(exactly = 0) { analyticsApi.execute(any(), any(), otherAuthorization) }
        }

    @Test
    fun `when another authorization's configuration load throws, its batch is dropped and the trigger is sent`() =
        runTest {
            every { analyticsEventRepository.flushAndReturnEvents() } returns listOf(
                AnalyticsEventBatch(otherAuthorization).apply {
                    events.add(expectedAnalyticsEvent.copy(name = "queued-other-event"))
                }
            )
            val configurationLoader = mockk<ConfigurationLoader> {
                coEvery { loadConfiguration(authorization) } returns
                    ConfigurationLoaderResult.Success(configuration)
                coEvery { loadConfiguration(otherAuthorization) } throws RuntimeException("boom")
            }
            val sut = createSut(
                configurationLoader,
                StandardTestDispatcher(testScheduler),
                TestScope(StandardTestDispatcher(testScheduler))
            )

            sut.sendEvent(
                eventName = eventName,
                authorization = authorization,
                analyticsEventParams = analyticsEventParams
            )
            advanceUntilIdle()

            verify {
                analyticsApi.execute(
                    events = listOf(expectedAnalyticsEvent),
                    configuration = configuration,
                    authorization = authorization
                )
            }
            verify(exactly = 0) { analyticsApi.execute(any(), any(), otherAuthorization) }
        }

    @Test
    fun `when sending another authorization's batch throws, later events are still sent`() = runTest {
        every { analyticsEventRepository.flushAndReturnEvents() } returnsMany listOf(
            listOf(
                AnalyticsEventBatch(otherAuthorization).apply {
                    events.add(expectedAnalyticsEvent.copy(name = "queued-other-event"))
                }
            ),
            emptyList()
        )
        every { analyticsApi.execute(any(), any(), otherAuthorization) } throws RuntimeException("boom")
        val configurationLoader = mockk<ConfigurationLoader> {
            coEvery { loadConfiguration(authorization) } returns
                ConfigurationLoaderResult.Success(configuration)
            coEvery { loadConfiguration(otherAuthorization) } returns
                ConfigurationLoaderResult.Success(otherConfiguration)
        }
        val sut = createSut(
            configurationLoader,
            StandardTestDispatcher(testScheduler),
            TestScope(StandardTestDispatcher(testScheduler))
        )

        sut.sendEvent(eventName = eventName, authorization = authorization, analyticsEventParams = analyticsEventParams)
        advanceUntilIdle()
        sut.sendEvent(eventName = eventName, authorization = authorization, analyticsEventParams = analyticsEventParams)
        advanceUntilIdle()

        verify(exactly = 2) {
            analyticsApi.execute(listOf(expectedAnalyticsEvent), configuration, authorization)
        }
    }

    @Test
    fun `when another authorization's configuration load never completes, the trigger is still sent`() =
        runTest {
            val queuedEvent = expectedAnalyticsEvent.copy(name = "queued-event")
            every { analyticsEventRepository.flushAndReturnEvents() } returns listOf(
                AnalyticsEventBatch(authorization).apply { events.add(queuedEvent) }
            )
            val neverCompletingLoad = CompletableDeferred<ConfigurationLoaderResult>()
            val configurationLoader = mockk<ConfigurationLoader> {
                coEvery { loadConfiguration(otherAuthorization) } returns
                    ConfigurationLoaderResult.Success(otherConfiguration)
                coEvery { loadConfiguration(authorization) } coAnswers { neverCompletingLoad.await() }
            }
            val sut = createSut(
                configurationLoader,
                StandardTestDispatcher(testScheduler),
                TestScope(StandardTestDispatcher(testScheduler))
            )

            sut.sendEvent(
                eventName = eventName,
                authorization = otherAuthorization,
                analyticsEventParams = analyticsEventParams
            )
            advanceUntilIdle()

            verify {
                analyticsApi.execute(
                    events = listOf(expectedAnalyticsEvent),
                    configuration = otherConfiguration,
                    authorization = otherAuthorization
                )
            }
            verify(exactly = 0) { analyticsApi.execute(any(), any(), authorization) }

            neverCompletingLoad.complete(ConfigurationLoaderResult.Success(configuration))
            advanceUntilIdle()

            verify {
                analyticsApi.execute(
                    events = listOf(queuedEvent),
                    configuration = configuration,
                    authorization = authorization
                )
            }
        }

    @Test
    fun `when reportCrash is called, the crash event is executed and never queued`() = runTest {
        val expectedCrashEvent = AnalyticsEvent(
            name = "crash",
            timestamp = timestamp
        )
        val sut = createSut(
            MockkConfigurationLoaderBuilder().configuration(configuration).build(),
            StandardTestDispatcher(testScheduler),
            TestScope(StandardTestDispatcher(testScheduler))
        )

        sut.reportCrash(authorization)
        advanceUntilIdle()

        verify {
            analyticsApi.execute(
                events = listOf(expectedCrashEvent),
                configuration = configuration,
                authorization = authorization
            )
        }
        verify(exactly = 0) { analyticsEventRepository.addEvent(any(), any()) }
    }

    private fun testDispatcher(): CoroutineDispatcher = StandardTestDispatcher()

    private fun testScope(): CoroutineScope = CoroutineScope(testDispatcher())
}
