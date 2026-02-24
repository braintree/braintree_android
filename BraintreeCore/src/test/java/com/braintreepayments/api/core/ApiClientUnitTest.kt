package com.braintreepayments.api.core

import com.braintreepayments.api.card.Card
import com.braintreepayments.api.testutils.Fixtures
import com.braintreepayments.api.testutils.MockkBraintreeClientBuilder
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
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

    private lateinit var graphQLEnabledConfig: Configuration
    private lateinit var graphQLDisabledConfig: Configuration
    private val testDispatcher = StandardTestDispatcher()

    @Before
    @Throws(JSONException::class)
    fun beforeEach() {
        analyticsParamRepository = mockk(relaxed = true)

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

        val sut = ApiClient(
            braintreeClient = braintreeClient,
            analyticsParamRepository = analyticsParamRepository
        )
        val card = spyk(Card())
        sut.tokenizeREST(card)

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

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    @Throws(BraintreeException::class, InvalidArgumentException::class, JSONException::class)
    fun tokenizeGraphQL_tokenizesCardsWithGraphQL() = runTest(testDispatcher) {
        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationSuccess(graphQLEnabledConfig)
            .build()

        val graphQLBodySlot = slot<JSONObject>()
        coEvery { braintreeClient.sendGraphQLPOST(capture(graphQLBodySlot)) } returns "{}"

        val sut = ApiClient(braintreeClient, analyticsParamRepository)
        val card = Card()
        sut.tokenizeGraphQL(card.buildJSONForGraphQL())

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

        val sut = ApiClient(
            braintreeClient = braintreeClient,
        )
        sut.tokenizeREST(mockk(relaxed = true))

        coVerify(inverse = true) { braintreeClient.sendGraphQLPOST(any()) }
    }

    @Test
    fun `when tokenizeREST is called, braintreeClient sendPOST is called with empty headers`() =
        runTest(testDispatcher) {
        val braintreeClient = MockkBraintreeClientBuilder()
            .sendPostSuccessfulResponse("{}")
            .build()

        val sut = ApiClient(
            braintreeClient = braintreeClient,
        )

        sut.tokenizeREST(mockk(relaxed = true))

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
