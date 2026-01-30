package com.braintreepayments.api.core

import com.braintreepayments.api.card.Card
import com.braintreepayments.api.testutils.Fixtures
import com.braintreepayments.api.testutils.MockkBraintreeClientBuilder
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import io.mockk.verifyOrder
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

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ApiClientUnitTest {

    private lateinit var analyticsParamRepository: AnalyticsParamRepository

    private lateinit var tokenizeCallback: TokenizeCallback

    private lateinit var graphQLEnabledConfig: Configuration
    private lateinit var graphQLDisabledConfig: Configuration
    private val testDispatcher = StandardTestDispatcher()

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
    fun tokenizeREST_setsSessionIdBeforeTokenizing() = runTest(testDispatcher) {
        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationSuccess(graphQLDisabledConfig)
            .sendPostSuccessfulResponse("{}")
            .build()

        every { analyticsParamRepository.sessionId } returns "session-id"
        val bodySlot = slot<String>()
        coEvery {
            braintreeClient.sendPOST(
                url = any<String>(),
                data = capture(bodySlot),
            )
        } returns "{}"
        val testScope = TestScope(testDispatcher)
        val sut = ApiClient(
            braintreeClient = braintreeClient,
            analyticsParamRepository = analyticsParamRepository,
            dispatcher = testDispatcher,
            coroutineScope = testScope
        )
        val card = spyk(Card())
        sut.tokenizeREST(card, tokenizeCallback)

        advanceUntilIdle()

        verifyOrder {
            card.sessionId = "session-id"
        }

        coVerify {
            braintreeClient.sendPOST(
                url = any<String>(),
                data = any<String>(),
            )
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

        coVerify(inverse = true) {
            braintreeClient.sendPOST(
                url = any<String>(),
                data = any<String>(),
            )
        }
        assertEquals(card.buildJSONForGraphQL().toString(), graphQLBodySlot.captured.toString())
    }

    @Test
    fun tokenizeREST_tokenizesPaymentMethodsWithREST() = runTest(testDispatcher) {
        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationSuccess(graphQLEnabledConfig)
            .sendPostSuccessfulResponse("{}")
            .build()
        val testScope = TestScope(testDispatcher)
        val sut = ApiClient(
            braintreeClient = braintreeClient,
            dispatcher = testDispatcher,
            coroutineScope = testScope
        )
        sut.tokenizeREST(mockk(relaxed = true), tokenizeCallback)
        sut.tokenizeREST(mockk(relaxed = true), tokenizeCallback)

        advanceUntilIdle()

        verify(inverse = true) { braintreeClient.sendGraphQLPOST(any(), any()) }
    }

    @Test
    fun `when tokenizeREST is called, braintreeClient sendPOST is called with empty headers`() = runTest(testDispatcher) {
        val braintreeClient = MockkBraintreeClientBuilder()
            .sendPostSuccessfulResponse("{}")
            .build()
        val testScope = TestScope(testDispatcher)
        val sut = ApiClient(
            braintreeClient = braintreeClient,
            dispatcher = testDispatcher,
            coroutineScope = testScope
        )

        sut.tokenizeREST(mockk(relaxed = true), mockk(relaxed = true))
        advanceUntilIdle()

        coVerify {
            braintreeClient.sendPOST(
                url = any<String>(),
                data = any<String>(),
            )
        }
    }

    @Test
    fun versionedPath_returnsv1Path() {
        assertEquals("/v1/test/path", ApiClient.versionedPath("test/path"))
    }
}
