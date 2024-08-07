package com.braintreepayments.api.core

import com.braintreepayments.api.testutils.Fixtures
import com.braintreepayments.api.testutils.MockkBraintreeClientBuilder
import com.braintreepayments.api.card.Card
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

    private lateinit var tokenizeCallback: TokenizeCallback

    private lateinit var graphQLEnabledConfig: Configuration
    private lateinit var graphQLDisabledConfig: Configuration

    @Before
    @Throws(JSONException::class)
    fun beforeEach() {
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
        every { braintreeClient.sendPOST(any(), capture(bodySlot), any(), any()) } returns Unit

        val sut = ApiClient(braintreeClient)
        val card = spyk(Card())
        sut.tokenizeREST(card, tokenizeCallback)

        verifyOrder {
            card.setSessionId("session-id")
            braintreeClient.sendPOST(any(), any(), any(), any())
        }

        val data = JSONObject(bodySlot.captured).getJSONObject("_meta")
        assertEquals("session-id", data.getString("sessionId"))
    }

    @Test
    @Throws(BraintreeException::class, InvalidArgumentException::class, JSONException::class)
    fun tokenizeGraphQL_tokenizesCardsWithGraphQL() {
        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationSuccess(graphQLEnabledConfig)
            .build()

        val graphQLBodySlot = slot<JSONObject>()
        every { braintreeClient.sendGraphQLPOST(capture(graphQLBodySlot), any()) } returns Unit

        val sut = ApiClient(braintreeClient)
        val card = Card()
        sut.tokenizeGraphQL(card.buildJSONForGraphQL(), tokenizeCallback)

        verify(inverse = true) { braintreeClient.sendPOST(any(), any(), any(), any()) }
        assertEquals(card.buildJSONForGraphQL().toString(), graphQLBodySlot.captured.toString())
    }

    @Test
    fun tokenizeREST_tokenizesPaymentMethodsWithREST() {
        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationSuccess(graphQLEnabledConfig)
            .build()

        val sut = ApiClient(braintreeClient)
        sut.tokenizeREST(mockk(relaxed = true), tokenizeCallback)
        sut.tokenizeREST(mockk(relaxed = true), tokenizeCallback)

        verify(inverse = true) { braintreeClient.sendGraphQLPOST(any(), any()) }
    }

    @Test
    fun `when tokenizeREST is called, braintreeClient sendPOST is called with empty headers`() {
        val braintreeClient = MockkBraintreeClientBuilder().build()
        val sut = ApiClient(braintreeClient)

        sut.tokenizeREST(mockk(relaxed = true), mockk(relaxed = true))

        verify { braintreeClient.sendPOST(any(), any(), emptyMap(), any()) }
    }

    @Test
    fun versionedPath_returnsv1Path() {
        assertEquals("/v1/test/path", ApiClient.versionedPath("test/path"))
    }
}
