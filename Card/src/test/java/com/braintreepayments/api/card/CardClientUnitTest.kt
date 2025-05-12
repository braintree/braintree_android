package com.braintreepayments.api.card

import com.braintreepayments.api.core.AnalyticsEventParams
import com.braintreepayments.api.core.AnalyticsParamRepository
import com.braintreepayments.api.core.ApiClient
import com.braintreepayments.api.core.Configuration
import com.braintreepayments.api.testutils.Fixtures
import com.braintreepayments.api.testutils.MockkApiClientBuilder
import com.braintreepayments.api.testutils.MockkBraintreeClientBuilder
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import io.mockk.verifyOrder
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class CardClientUnitTest {
    private val card: Card = Card()
    private val cardTokenizeCallback: CardTokenizeCallback = mockk(relaxed = true)

    private var apiClient: ApiClient = mockk(relaxed = true)
    private val analyticsParamRepository: AnalyticsParamRepository = mockk(relaxed = true)

    private val graphQLEnabledConfig: Configuration = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_GRAPHQL)
    private val graphQLDisabledConfig: Configuration = Configuration.fromJson(Fixtures.CONFIGURATION_WITHOUT_ACCESS_TOKEN)

    @Test
    fun tokenize_resetsSessionId() {
        val braintreeClient = MockkBraintreeClientBuilder().build()
        apiClient = MockkApiClientBuilder().build()

        val sut = CardClient(braintreeClient, apiClient, analyticsParamRepository)
        sut.tokenize(card, cardTokenizeCallback)

        verify { analyticsParamRepository.reset() }
    }

    @Test
    fun tokenize_sendsTokenizeStartedAnalytics() {
        val braintreeClient = MockkBraintreeClientBuilder().build()
        apiClient = MockkApiClientBuilder().build()

        val sut = CardClient(braintreeClient, apiClient, analyticsParamRepository)
        sut.tokenize(card, cardTokenizeCallback)

        verify { braintreeClient.sendAnalyticsEvent(CardAnalytics.CARD_TOKENIZE_STARTED, any(), true) }
    }

    @Test
    fun tokenize_whenGraphQLEnabled_setsSessionIdOnCardBeforeTokenizing() {
        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationSuccess(graphQLEnabledConfig)
            .build()
        every { analyticsParamRepository.sessionId } returns "session-id"

        val sut = CardClient(braintreeClient, apiClient, analyticsParamRepository)

        val card = spyk(Card())
        sut.tokenize(card, cardTokenizeCallback)

        verifyOrder {
            card.sessionId = "session-id"
            apiClient.tokenizeGraphQL(any(), any())
        }
    }

    @Test
    fun tokenize_whenGraphQLEnabled_tokenizesWithGraphQL() {
        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationSuccess(graphQLEnabledConfig)
            .build()

        apiClient = MockkApiClientBuilder()
            .tokenizeGraphQLSuccess(JSONObject(Fixtures.GRAPHQL_RESPONSE_CREDIT_CARD))
            .build()

        val sut = CardClient(braintreeClient, apiClient, analyticsParamRepository)

        sut.tokenize(card, cardTokenizeCallback)

        val captor = slot<CardResult>()
        verify { cardTokenizeCallback.onCardResult(capture(captor)) }

        val result = captor.captured
        assertTrue(result is CardResult.Success)
        val cardNonce = result.nonce
        assertEquals("3744a73e-b1ab-0dbd-85f0-c12a0a4bd3d1", cardNonce.string)
    }

    @Test
    fun tokenize_whenGraphQLDisabled_tokenizesWithREST() {
        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationSuccess(graphQLDisabledConfig)
            .build()

        apiClient = MockkApiClientBuilder()
            .tokenizeRESTSuccess(JSONObject(Fixtures.PAYMENT_METHODS_RESPONSE_VISA_CREDIT_CARD))
            .build()

        val sut = CardClient(braintreeClient, apiClient, analyticsParamRepository)

        sut.tokenize(card, cardTokenizeCallback)

        val captor = slot<CardResult>()
        verify { cardTokenizeCallback.onCardResult(capture(captor)) }

        val result = captor.captured
        assertTrue(result is CardResult.Success)
        val cardNonce = result.nonce
        assertEquals("123456-12345-12345-a-adfa", cardNonce.string)
    }

    @Test
    fun tokenize_whenGraphQLEnabled_sendsAnalyticsEventOnSuccess() {
        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationSuccess(graphQLEnabledConfig)
            .build()

        apiClient = MockkApiClientBuilder()
            .tokenizeGraphQLSuccess(JSONObject(Fixtures.GRAPHQL_RESPONSE_CREDIT_CARD))
            .build()

        val sut = CardClient(braintreeClient, apiClient, analyticsParamRepository)
        sut.tokenize(card, cardTokenizeCallback)

        verify { braintreeClient.sendAnalyticsEvent(CardAnalytics.CARD_TOKENIZE_SUCCEEDED, any(), true) }
    }

    @Test
    fun tokenize_whenGraphQLDisabled_sendsAnalyticsEventOnSuccess() {
        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationSuccess(graphQLDisabledConfig)
            .build()

        apiClient = MockkApiClientBuilder()
            .tokenizeRESTSuccess(JSONObject(Fixtures.PAYMENT_METHODS_RESPONSE_VISA_CREDIT_CARD))
            .build()

        val sut = CardClient(braintreeClient, apiClient, analyticsParamRepository)
        sut.tokenize(card, cardTokenizeCallback)

        verify { braintreeClient.sendAnalyticsEvent(CardAnalytics.CARD_TOKENIZE_SUCCEEDED, any(), true) }
    }

    @Test
    fun tokenize_whenGraphQLEnabled_callsListenerWithErrorOnFailure() {
        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationSuccess(graphQLEnabledConfig)
            .build()

        val error = Exception()
        apiClient = MockkApiClientBuilder()
            .tokenizeGraphQLError(error)
            .build()

        val sut = CardClient(braintreeClient, apiClient, analyticsParamRepository)
        sut.tokenize(card, cardTokenizeCallback)

        val captor = slot<CardResult>()
        verify { cardTokenizeCallback.onCardResult(capture(captor)) }

        val result = captor.captured
        assertTrue(result is CardResult.Failure)
        val actualError = result.error
        assertEquals(error, actualError)
    }

    @Test
    fun tokenize_whenGraphQLDisabled_callsListenerWithErrorOnFailure() {
        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationSuccess(graphQLDisabledConfig)
            .build()

        val error = Exception()
        apiClient = MockkApiClientBuilder()
            .tokenizeRESTError(error)
            .build()

        val sut = CardClient(braintreeClient, apiClient, analyticsParamRepository)
        sut.tokenize(card, cardTokenizeCallback)

        val captor = slot<CardResult>()
        verify { cardTokenizeCallback.onCardResult(capture(captor)) }

        val result = captor.captured
        assertTrue(result is CardResult.Failure)
        val actualError = result.error
        assertEquals(error, actualError)
    }

    @Test
    fun tokenize_whenGraphQLEnabled_sendsAnalyticsEventOnFailure() {
        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationSuccess(graphQLEnabledConfig)
            .build()

        val error = Exception()
        apiClient = MockkApiClientBuilder()
            .tokenizeGraphQLError(error)
            .build()

        val sut = CardClient(braintreeClient, apiClient, analyticsParamRepository)
        sut.tokenize(card, cardTokenizeCallback)

        verify { braintreeClient.sendAnalyticsEvent(CardAnalytics.CARD_TOKENIZE_FAILED, any(), true) }
    }

    @Test
    fun tokenize_whenGraphQLDisabled_sendsAnalyticsEventOnFailure() {
        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationSuccess(graphQLDisabledConfig)
            .build()

        val error = Exception()
        apiClient = MockkApiClientBuilder()
            .tokenizeRESTError(error)
            .build()

        val sut = CardClient(braintreeClient, apiClient, analyticsParamRepository)
        sut.tokenize(card, cardTokenizeCallback)

        verify { braintreeClient.sendAnalyticsEvent(CardAnalytics.CARD_TOKENIZE_FAILED, any(), true) }
    }

    @Test
    fun tokenize_propagatesConfigurationFetchError() {
        val configError = Exception("Configuration error.")
        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationError(configError)
            .build()

        val sut = CardClient(braintreeClient, apiClient, analyticsParamRepository)
        sut.tokenize(card, cardTokenizeCallback)

        val captor = slot<CardResult>()
        verify { cardTokenizeCallback.onCardResult(capture(captor)) }

        val result = captor.captured
        assertTrue(result is CardResult.Failure)
        val actualError = result.error
        assertEquals(configError, actualError)
    }

    @Test
    fun tokenizeException_analyticsEventIsSentWithErrorDescription() {
        val errorDescription = "Configuration error."
        val configError = Exception(errorDescription)
        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationError(configError)
            .build()

        val sut = CardClient(braintreeClient, apiClient, analyticsParamRepository)
        sut.tokenize(card, cardTokenizeCallback)

        val errorParams = AnalyticsEventParams(
            errorDescription = errorDescription
        )
        verify { braintreeClient.sendAnalyticsEvent(CardAnalytics.CARD_TOKENIZE_FAILED, errorParams, true) }
    }
}