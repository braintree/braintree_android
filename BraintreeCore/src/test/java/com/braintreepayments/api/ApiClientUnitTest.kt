package com.braintreepayments.api

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.mockk.*
import org.json.JSONException
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ApiClientUnitTest {

    private lateinit var context: Context
    private lateinit var tokenizeCallback: TokenizeCallback

    private lateinit var graphQLEnabledConfig: Configuration
    private lateinit var graphQLDisabledConfig: Configuration

    @Before
    @Throws(JSONException::class)
    fun beforeEach() {
        context = ApplicationProvider.getApplicationContext()
        tokenizeCallback = mockk(relaxed = true)

        graphQLEnabledConfig = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_GRAPHQL)
        graphQLDisabledConfig = Configuration.fromJson(Fixtures.CONFIGURATION_WITHOUT_ACCESS_TOKEN)
    }

    @Test
    @Throws(JSONException::class)
    fun tokenizeREST_setsSessionIdBeforeTokenizing() {
        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationSuccess(graphQLDisabledConfig)
            .sessionId("session-id")
            .build()

        val bodySlot = slot<String>()
        every { braintreeClient.sendPOST(any(), capture(bodySlot), any()) } returns Unit

        val sut = ApiClient(braintreeClient)
        val card = spyk(Card())
        sut.tokenizeREST(card, tokenizeCallback)

        verifyOrder {
            card.setSessionId("session-id")
            braintreeClient.sendPOST(any(), any(), any())
        }

        val data = JSONObject(bodySlot.captured).getJSONObject("_meta")
        assertEquals("session-id", data.getString("sessionId"))
    }

    @Test
    @Throws(BraintreeException::class, InvalidArgumentException::class, JSONException::class)
    fun tokenizeGraphQL_tokenizesCardsWithGraphQL() {
        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationSuccess(graphQLEnabledConfig)
            .authorizationSuccess(Authorization.fromString(Fixtures.BASE64_CLIENT_TOKEN))
            .build()

        val graphQLBodySlot = slot<String>()
        every { braintreeClient.sendGraphQLPOST(capture(graphQLBodySlot), any()) } returns Unit

        val sut = ApiClient(braintreeClient)
        val card = Card()
        sut.tokenizeGraphQL(card.buildJSONForGraphQL(), tokenizeCallback)

        verify(inverse = true) { braintreeClient.sendPOST(any(), any(), any()) }
        assertEquals(card.buildJSONForGraphQL().toString(), graphQLBodySlot.captured)
    }

    @Test
    @Throws(BraintreeException::class, JSONException::class)
    fun tokenizeGraphQL_sendGraphQLAnalyticsEventWhenEnabled() {
        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationSuccess(graphQLEnabledConfig)
            .build()
        val card = Card()
        val sut = ApiClient(braintreeClient)
        sut.tokenizeGraphQL(card.buildJSONForGraphQL(), tokenizeCallback)

        verify { braintreeClient.sendAnalyticsEvent("card.graphql.tokenization.started") }
    }

    @Test
    fun tokenizeREST_tokenizesPaymentMethodsWithREST() {
        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationSuccess(graphQLEnabledConfig)
            .build()

        val sut = ApiClient(braintreeClient)
        sut.tokenizeREST(PayPalAccount(), tokenizeCallback)
        sut.tokenizeREST(UnionPayCard(), tokenizeCallback)
        sut.tokenizeREST(VenmoAccount(), tokenizeCallback)

        verify(inverse = true) { braintreeClient.sendGraphQLPOST(any(), any()) }
    }

    @Test
    @Throws(BraintreeException::class, JSONException::class)
    fun tokenizeGraphQL_sendGraphQLAnalyticsEventOnSuccess() {
        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationSuccess(graphQLEnabledConfig)
            .sendGraphQLPOSTSuccessfulResponse(Fixtures.GRAPHQL_RESPONSE_CREDIT_CARD)
            .build()

        val card = Card()
        val sut = ApiClient(braintreeClient)
        sut.tokenizeGraphQL(card.buildJSONForGraphQL(), tokenizeCallback)

        verify { braintreeClient.sendAnalyticsEvent("card.graphql.tokenization.success") }
    }

    @Test
    @Throws(BraintreeException::class, JSONException::class)
    fun tokenizeGraphQL_sendGraphQLAnalyticsEventOnFailure() {
        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationSuccess(graphQLEnabledConfig)
            .sendGraphQLPOSTErrorResponse(ErrorWithResponse.fromGraphQLJson(Fixtures.ERRORS_GRAPHQL_CREDIT_CARD_ERROR))
            .build()
        val card = Card()
        val sut = ApiClient(braintreeClient)
        sut.tokenizeGraphQL(card.buildJSONForGraphQL(), tokenizeCallback)
        verify { braintreeClient.sendAnalyticsEvent("card.graphql.tokenization.failure") }
    }

    @Test
    fun versionedPath_returnsv1Path() {
        assertEquals("/v1/test/path", ApiClient.versionedPath("test/path"))
    }
}
