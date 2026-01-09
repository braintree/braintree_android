package com.braintreepayments.api.core

import com.braintreepayments.api.sharedutils.HttpClient
import com.braintreepayments.api.sharedutils.HttpResponse
import com.braintreepayments.api.sharedutils.HttpResponseTiming
import com.braintreepayments.api.sharedutils.Method
import com.braintreepayments.api.sharedutils.NetworkResponseCallback
import com.braintreepayments.api.sharedutils.OkHttpRequest
import io.mockk.coEvery
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class BraintreeGraphQLClientUnitTest {

    private lateinit var httpClient: HttpClient
    private lateinit var callback: NetworkResponseCallback

    @Before
    fun setUp() {
        httpClient = mockk(relaxed = true)
        callback = mockk(relaxed = true)
    }

    @Test
    fun `when post is called with invalid authorization, callback is called with error`() {
        val testDispatcher = StandardTestDispatcher()
        val testScope = TestScope(testDispatcher)
        val sut = BraintreeGraphQLClient(httpClient, testDispatcher, testScope)
        val invalidAuth = InvalidAuthorization(
            rawValue = "bad auth",
            errorMessage = "bad auth"
        )
        val config = mockk<Configuration>(relaxed = true)
        every { config.graphQLUrl } returns "https://graphql.example.com"

        sut.post("{}", config, invalidAuth, callback)
        verify {
            callback.onResult(match {
                it is NetworkResponseCallback.Result.Failure &&
                    it.error is BraintreeException && it.error.message == "bad auth"
            })
        }
        confirmVerified(callback)
    }

    @Test
    fun `when post is called with valid authorization, request is sent with correct headers and body`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)
        val sut = BraintreeGraphQLClient(httpClient, testDispatcher, testScope)
        val auth = mockk<Authorization>(relaxed = true)
        every { auth.bearer } returns "token123"
        val config = mockk<Configuration>(relaxed = true)
        every { config.graphQLUrl } returns "https://graphql.example.com"
        val slot = slot<OkHttpRequest>()
        val mockResponse = HttpResponse(body = "{}", timing = HttpResponseTiming(0, 0))
        coEvery { httpClient.sendRequest(capture(slot)) } returns mockResponse

        sut.post("{\"query\":\"test\"}", config, auth, callback)

        advanceUntilIdle()

        val req = slot.captured
        assertTrue(req.method is Method.Post)
        assertEquals("{\"query\":\"test\"}", (req.method as Method.Post).body)
        assertEquals("https://graphql.example.com", req.url)
        assertEquals("braintree/android/" + BuildConfig.VERSION_NAME, req.headers["User-Agent"])
        assertEquals("Bearer token123", req.headers["Authorization"])
        assertEquals(GraphQLConstants.Headers.API_VERSION, req.headers["Braintree-Version"])
        verify {
            callback.onResult(match { it is NetworkResponseCallback.Result.Success })
        }
    }
}
