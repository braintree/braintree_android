package com.braintreepayments.api.card

import com.braintreepayments.api.core.AnalyticsEventParams
import com.braintreepayments.api.core.AnalyticsParamRepository
import com.braintreepayments.api.core.ApiClient
import com.braintreepayments.api.core.BraintreeException
import com.braintreepayments.api.core.Configuration
import com.braintreepayments.api.testutils.Fixtures
import com.braintreepayments.api.testutils.MockkApiClientBuilder
import com.braintreepayments.api.testutils.MockkBraintreeClientBuilder
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class CardClientUnitTest {
    private val card: Card = Card()
    private val cardTokenizeCallback: CardTokenizeCallback = mockk(relaxed = true)

    private var apiClient: ApiClient = mockk(relaxed = true)
    private val analyticsParamRepository: AnalyticsParamRepository = mockk(relaxed = true)

    private val graphQLEnabledConfig: Configuration =
        Configuration.fromJson(Fixtures.CONFIGURATION_WITH_GRAPHQL)
    private val graphQLDisabledConfig: Configuration =
        Configuration.fromJson(Fixtures.CONFIGURATION_WITHOUT_ACCESS_TOKEN)

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var testScope: TestScope

    @Before
    fun beforeEach() {
        testScope = TestScope(testDispatcher)
    }

    @Test
    fun tokenize_resetsSessionId() = runTest(testDispatcher) {
        val braintreeClient = MockkBraintreeClientBuilder().build()
        apiClient = MockkApiClientBuilder().build()

        val sut = CardClient(
            braintreeClient,
            apiClient,
            analyticsParamRepository,
            testDispatcher,
            testScope
        )
        sut.tokenize(card, cardTokenizeCallback)
        advanceUntilIdle()

        verify { analyticsParamRepository.reset() }
    }

    @Test
    fun tokenize_sendsTokenizeStartedAnalytics() = runTest(testDispatcher) {
        val braintreeClient = MockkBraintreeClientBuilder().build()
        apiClient = MockkApiClientBuilder().build()

        val sut = CardClient(
            braintreeClient,
            apiClient,
            analyticsParamRepository,
            testDispatcher,
            testScope
        )
        sut.tokenize(card, cardTokenizeCallback)
        advanceUntilIdle()

        verify { braintreeClient.sendAnalyticsEvent(CardAnalytics.CARD_TOKENIZE_STARTED, any(), true) }
    }

    @Test
    fun tokenize_whenGraphQLEnabled_setsSessionIdOnCardBeforeTokenizing() = runTest(testDispatcher) {
        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationSuccess(graphQLEnabledConfig)
            .build()
        every { analyticsParamRepository.sessionId } returns "session-id"

        val sut = CardClient(
            braintreeClient,
            apiClient,
            analyticsParamRepository,
            testDispatcher,
            testScope
        )

        val card = spyk(Card())
        sut.tokenize(card, cardTokenizeCallback)
        advanceUntilIdle()

        coVerifyOrder {
            card.sessionId = "session-id"
            apiClient.tokenizeGraphQL(any())
        }
    }

    @Test
    fun tokenize_whenGraphQLEnabled_tokenizesWithGraphQL() = runTest(testDispatcher) {
        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationSuccess(graphQLEnabledConfig)
            .build()

        apiClient = MockkApiClientBuilder()
            .tokenizeGraphQLSuccess(JSONObject(Fixtures.GRAPHQL_RESPONSE_CREDIT_CARD))
            .build()

        val sut = CardClient(
            braintreeClient,
            apiClient,
            analyticsParamRepository,
            testDispatcher,
            testScope
        )

        sut.tokenize(card, cardTokenizeCallback)
        advanceUntilIdle()

        val captor = slot<CardResult>()
        verify { cardTokenizeCallback.onCardResult(capture(captor)) }

        val result = captor.captured
        assertTrue(result is CardResult.Success)
        val cardNonce = result.nonce
        assertEquals("3744a73e-b1ab-0dbd-85f0-c12a0a4bd3d1", cardNonce.string)
    }

    @Test
    fun tokenize_whenGraphQLDisabled_tokenizesWithREST() = runTest(testDispatcher) {
        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationSuccess(graphQLDisabledConfig)
            .build()

        apiClient = MockkApiClientBuilder()
            .tokenizeRESTSuccess(JSONObject(Fixtures.PAYMENT_METHODS_RESPONSE_VISA_CREDIT_CARD))
            .build()

        val sut = CardClient(
            braintreeClient,
            apiClient,
            analyticsParamRepository,
            testDispatcher,
            testScope
        )

        sut.tokenize(card, cardTokenizeCallback)
        advanceUntilIdle()

        val captor = slot<CardResult>()
        verify { cardTokenizeCallback.onCardResult(capture(captor)) }

        val result = captor.captured
        assertTrue(result is CardResult.Success)
        val cardNonce = result.nonce
        assertEquals("123456-12345-12345-a-adfa", cardNonce.string)
    }

    @Test
    fun tokenize_whenGraphQLEnabled_sendsAnalyticsEventOnSuccess() = runTest(testDispatcher) {
        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationSuccess(graphQLEnabledConfig)
            .build()

        apiClient = MockkApiClientBuilder()
            .tokenizeGraphQLSuccess(JSONObject(Fixtures.GRAPHQL_RESPONSE_CREDIT_CARD))
            .build()

        val sut = CardClient(
            braintreeClient,
            apiClient,
            analyticsParamRepository,
            testDispatcher,
            testScope
        )
        sut.tokenize(card, cardTokenizeCallback)
        advanceUntilIdle()

        verify { braintreeClient.sendAnalyticsEvent(CardAnalytics.CARD_TOKENIZE_SUCCEEDED, any(), true) }
    }

    @Test
    fun tokenize_whenGraphQLDisabled_sendsAnalyticsEventOnSuccess() = runTest(testDispatcher) {
        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationSuccess(graphQLDisabledConfig)
            .build()

        apiClient = MockkApiClientBuilder()
            .tokenizeRESTSuccess(JSONObject(Fixtures.PAYMENT_METHODS_RESPONSE_VISA_CREDIT_CARD))
            .build()

        val sut = CardClient(
            braintreeClient,
            apiClient,
            analyticsParamRepository,
            testDispatcher,
            testScope
        )
        sut.tokenize(card, cardTokenizeCallback)
        advanceUntilIdle()

        verify { braintreeClient.sendAnalyticsEvent(CardAnalytics.CARD_TOKENIZE_SUCCEEDED, any(), true) }
    }

    @Test
    fun tokenize_whenGraphQLEnabled_callsListenerWithErrorOnFailure() = runTest(testDispatcher) {
        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationSuccess(graphQLEnabledConfig)
            .build()

        val error = IOException()
        apiClient = MockkApiClientBuilder()
            .tokenizeGraphQLError(error)
            .build()

        val sut = CardClient(
            braintreeClient,
            apiClient,
            analyticsParamRepository,
            testDispatcher,
            testScope
        )
        sut.tokenize(card, cardTokenizeCallback)
        advanceUntilIdle()

        val captor = slot<CardResult>()
        verify { cardTokenizeCallback.onCardResult(capture(captor)) }

        val result = captor.captured
        assertTrue(result is CardResult.Failure)
        val actualError = result.error
        assertEquals(error, actualError)
    }

    @Test
    fun tokenize_whenGraphQLDisabled_callsListenerWithErrorOnFailure() = runTest(testDispatcher) {
        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationSuccess(graphQLDisabledConfig)
            .build()

        val error = IOException()
        apiClient = MockkApiClientBuilder()
            .tokenizeRESTError(error)
            .build()

        val sut = CardClient(
            braintreeClient,
            apiClient,
            analyticsParamRepository,
            testDispatcher,
            testScope
        )
        sut.tokenize(card, cardTokenizeCallback)
        advanceUntilIdle()

        val captor = slot<CardResult>()
        verify { cardTokenizeCallback.onCardResult(capture(captor)) }

        val result = captor.captured
        assertTrue(result is CardResult.Failure)
        val actualError = result.error
        assertEquals(error, actualError)
    }

    @Test
    fun tokenize_whenGraphQLEnabled_sendsAnalyticsEventOnFailure() = runTest(testDispatcher) {
        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationSuccess(graphQLEnabledConfig)
            .build()

        val error = IOException()
        apiClient = MockkApiClientBuilder()
            .tokenizeGraphQLError(error)
            .build()

        val sut = CardClient(
            braintreeClient,
            apiClient,
            analyticsParamRepository,
            testDispatcher,
            testScope
        )
        sut.tokenize(card, cardTokenizeCallback)
        advanceUntilIdle()

        verify { braintreeClient.sendAnalyticsEvent(CardAnalytics.CARD_TOKENIZE_FAILED, any(), true) }
    }

    @Test
    fun tokenize_whenGraphQLDisabled_sendsAnalyticsEventOnFailure() = runTest(testDispatcher) {
        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationSuccess(graphQLDisabledConfig)
            .build()

        val error = IOException()
        apiClient = MockkApiClientBuilder()
            .tokenizeRESTError(error)
            .build()

        val sut = CardClient(
            braintreeClient,
            apiClient,
            analyticsParamRepository,
            testDispatcher,
            testScope
        )
        sut.tokenize(card, cardTokenizeCallback)
        advanceUntilIdle()

        verify { braintreeClient.sendAnalyticsEvent(CardAnalytics.CARD_TOKENIZE_FAILED, any(), true) }
    }

    @Test
    fun tokenize_propagatesConfigurationFetchError() = runTest(testDispatcher) {
        val configError = Exception("Configuration error.")
        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationError(configError)
            .build()

        val sut = CardClient(
            braintreeClient,
            apiClient,
            analyticsParamRepository,
            testDispatcher,
            testScope
        )
        sut.tokenize(card, cardTokenizeCallback)
        advanceUntilIdle()

        val captor = slot<CardResult>()
        verify { cardTokenizeCallback.onCardResult(capture(captor)) }

        val result = captor.captured
        assertTrue(result is CardResult.Failure)
        val actualError = result.error
        assertEquals(configError, actualError)
    }

    @Test
    fun tokenizeException_analyticsEventIsSentWithErrorDescription() = runTest(testDispatcher) {
        val errorDescription = "Configuration error."
        val configError = Exception(errorDescription)
        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationError(configError)
            .build()

        val sut = CardClient(
            braintreeClient,
            apiClient,
            analyticsParamRepository,
            testDispatcher,
            testScope
        )
        sut.tokenize(card, cardTokenizeCallback)
        advanceUntilIdle()

        val errorParams = AnalyticsEventParams(
            errorDescription = errorDescription
        )
        verify { braintreeClient.sendAnalyticsEvent(CardAnalytics.CARD_TOKENIZE_FAILED, errorParams, true) }
    }

    @Test
    fun tokenize_whenResponseHasErrorsArray_callsListenerWithErrorOnFailure() = runTest(testDispatcher) {
        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationSuccess(graphQLEnabledConfig)
            .build()

        val responseWithErrors = JSONObject().apply {
            put("errors", JSONArray().apply {
                put(JSONObject().apply {
                    put("message", "Invalid card number")
                })
            })
        }

        apiClient = MockkApiClientBuilder()
            .tokenizeGraphQLSuccess(responseWithErrors)
            .build()

        val sut = CardClient(
            braintreeClient,
            apiClient,
            analyticsParamRepository,
            testDispatcher,
            testScope
        )
        sut.tokenize(card, cardTokenizeCallback)
        advanceUntilIdle()

        val captor = slot<CardResult>()
        verify { cardTokenizeCallback.onCardResult(capture(captor)) }

        val result = captor.captured
        assertTrue(result is CardResult.Failure)
        assertTrue(result.error is BraintreeException)
    }

    @Test
    fun tokenize_whenResponseHasEmptyErrorsArray_proceedsWithNormalProcessing() = runTest(testDispatcher) {
        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationSuccess(graphQLEnabledConfig)
            .build()

        val responseWithEmptyErrors = JSONObject(Fixtures.GRAPHQL_RESPONSE_CREDIT_CARD).apply {
            put("errors", JSONArray()) // Empty errors array
        }

        apiClient = MockkApiClientBuilder()
            .tokenizeGraphQLSuccess(responseWithEmptyErrors)
            .build()

        val sut = CardClient(
            braintreeClient,
            apiClient,
            analyticsParamRepository,
            testDispatcher,
            testScope
        )
        sut.tokenize(card, cardTokenizeCallback)
        advanceUntilIdle()

        val captor = slot<CardResult>()
        verify { cardTokenizeCallback.onCardResult(capture(captor)) }

        val result = captor.captured
        assertTrue(result is CardResult.Success)
    }
}
