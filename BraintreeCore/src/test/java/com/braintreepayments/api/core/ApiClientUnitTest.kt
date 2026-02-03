package com.braintreepayments.api.core

import com.braintreepayments.api.card.Card
import com.braintreepayments.api.testutils.Fixtures
import com.braintreepayments.api.testutils.MockkBraintreeClientBuilder
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.json.JSONException
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ApiClientUnitTest {

    private lateinit var analyticsParamRepository: AnalyticsParamRepository

    private lateinit var tokenizeCallback: TokenizeCallback

    private lateinit var graphQLEnabledConfig: Configuration
    private lateinit var graphQLDisabledConfig: Configuration

    @Before
    @Throws(JSONException::class)
    fun beforeEach() {
        analyticsParamRepository = mockk(relaxed = true)
        tokenizeCallback = mockk(relaxed = true)

        graphQLEnabledConfig = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_GRAPHQL)
        graphQLDisabledConfig = Configuration.fromJson(Fixtures.CONFIGURATION_WITHOUT_ACCESS_TOKEN)
    }

    @Test
    @Throws(JSONException::class)
    fun tokenizeREST_setsSessionIdBeforeTokenizing() {
        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationSuccess(graphQLDisabledConfig)
            .build()

        every { analyticsParamRepository.sessionId } returns "session-id"
        val bodySlot = slot<String>()
        every { braintreeClient.sendPOST(any(), capture(bodySlot), any(), any()) } returns Unit

        val sut = ApiClient(braintreeClient, analyticsParamRepository)
        val card = spyk(Card())
        sut.tokenizeREST(card, tokenizeCallback)

        verifyOrder {
            card.sessionId = "session-id"
            braintreeClient.sendPOST(any(), any(), any(), any())
        }

        val data = JSONObject(bodySlot.captured).getJSONObject("_meta")
        assertEquals("session-id", data.getString("sessionId"))
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    @Throws(BraintreeException::class, InvalidArgumentException::class, JSONException::class)
    fun tokenizeGraphQL_tokenizesCardsWithGraphQL() = runTest {
        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationSuccess(graphQLEnabledConfig)
            .build()

        val graphQLBodySlot = slot<JSONObject>()
        coEvery { braintreeClient.sendGraphQLPOST(capture(graphQLBodySlot)) } returns ""

        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)
        val sut = ApiClient(braintreeClient, analyticsParamRepository, testDispatcher, testScope)
        val card = Card()
        sut.tokenizeGraphQL(card.buildJSONForGraphQL(), tokenizeCallback)
        advanceUntilIdle()

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

        coVerify(inverse = true) { braintreeClient.sendGraphQLPOST(any()) }
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
