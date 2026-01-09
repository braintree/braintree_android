package com.braintreepayments.api.core

import com.braintreepayments.api.sharedutils.HttpClient
import com.braintreepayments.api.sharedutils.HttpResponse
import com.braintreepayments.api.sharedutils.HttpResponseTiming
import com.braintreepayments.api.sharedutils.Method
import com.braintreepayments.api.sharedutils.NetworkResponseCallback
import com.braintreepayments.api.sharedutils.OkHttpRequest
import com.braintreepayments.api.testutils.Fixtures
import com.braintreepayments.api.testutils.FixturesHelper
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.json.JSONException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class BraintreeHttpClientUnitTest {

    private lateinit var httpClient: HttpClient
    private lateinit var configuration: Configuration
    private lateinit var callback: NetworkResponseCallback

    @Before
    fun beforeEach() {
        httpClient = mockk(relaxed = true)
        configuration = mockk()
        callback = mockk(relaxed = true)

        every { configuration.clientApiUrl } returns "https://api.braintreegateway.com/"
    }

    // GET method tests

    @Test
    fun `when get is called with TokenizationKey, correct request is sent`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)
        val sut = BraintreeHttpClient(httpClient, testDispatcher, testScope)
        val tokenizationKey = TokenizationKey(Fixtures.TOKENIZATION_KEY)
        val requestSlot = slot<OkHttpRequest>()
        val mockResponse = HttpResponse(body = "{}", timing = HttpResponseTiming(0, 0))
        coEvery { httpClient.sendRequest(capture(requestSlot)) } returns mockResponse

        sut.get("v1/payment_methods", configuration, tokenizationKey, callback)

        advanceUntilIdle()

        val request = requestSlot.captured
        assertEquals("https://api.braintreegateway.com/v1/payment_methods", request.url)
        assertTrue(request.method is Method.Get)
        assertEquals("braintree/android/" + BuildConfig.VERSION_NAME, request.headers["User-Agent"])
        assertEquals(Fixtures.TOKENIZATION_KEY, request.headers["Client-Key"])
        verify {
            callback.onResult(match { it is NetworkResponseCallback.Result.Success })
        }
    }

    @Test
    fun `when get is called with ClientToken, authorization fingerprint is appended to URL and header`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)
        val sut = BraintreeHttpClient(httpClient, testDispatcher, testScope)
        val clientToken = Authorization.fromString(
            FixturesHelper.base64Encode(Fixtures.CLIENT_TOKEN)
        ) as ClientToken
        val requestSlot = slot<OkHttpRequest>()
        val mockResponse = HttpResponse(body = "{}", timing = HttpResponseTiming(0, 0))
        coEvery { httpClient.sendRequest(capture(requestSlot)) } returns mockResponse

        sut.get("v1/payment_methods", configuration, clientToken, callback)

        advanceUntilIdle()

        val request = requestSlot.captured
        val expectedUrl =
            "https://api.braintreegateway.com/v1/payment_methods?authorizationFingerprint=${clientToken.bearer}"
        assertEquals(expectedUrl, request.url)
        assertEquals("Bearer ${clientToken.bearer}", request.headers["Authorization"])
        assertNull(request.headers["Client-Key"])
        verify {
            callback.onResult(match { it is NetworkResponseCallback.Result.Success })
        }
    }

    @Test
    fun `when get is called with absolute URL, configuration base URL is ignored`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)
        val sut = BraintreeHttpClient(httpClient, testDispatcher, testScope)
        val tokenizationKey = TokenizationKey(Fixtures.TOKENIZATION_KEY)
        val requestSlot = slot<OkHttpRequest>()
        val mockResponse = HttpResponse(body = "{}", timing = HttpResponseTiming(0, 0))
        coEvery { httpClient.sendRequest(capture(requestSlot)) } returns mockResponse

        sut.get("https://example.com/custom/path", configuration, tokenizationKey, callback)

        advanceUntilIdle()

        val request = requestSlot.captured
        assertEquals("https://example.com/custom/path", request.url)
        verify {
            callback.onResult(match { it is NetworkResponseCallback.Result.Success })
        }
    }

    @Test
    fun `when get is called with null configuration and relative path, BraintreeException is thrown`() = runTest {
        val tokenizationKey = TokenizationKey(Fixtures.TOKENIZATION_KEY)
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)
        val sut = BraintreeHttpClient(httpClient, testDispatcher, testScope)

        sut.get("v1/payment_methods", null, tokenizationKey, callback)

        verify {
            callback.onResult(match {
                it is NetworkResponseCallback.Result.Failure &&
                    it.error is BraintreeException &&
                    it.error.message!!.contains("relative path")
            })
        }
    }

    @Test
    fun `when get is called with InvalidAuthorization, callback is called with error`() = runTest {
        val invalidAuth = InvalidAuthorization("invalid_token", "Invalid token")
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)
        val sut = BraintreeHttpClient(httpClient, testDispatcher, testScope)

        sut.get("v1/payment_methods", configuration, invalidAuth, callback)

        verify {
            callback.onResult(match {
                it is NetworkResponseCallback.Result.Failure &&
                    it.error is BraintreeException &&
                    it.error.message == "Invalid token"
            })
        }
    }

    @Test
    fun `when get is called with authorization bearer, Authorization header is added`() = runTest {
        val clientToken = Authorization.fromString(
            FixturesHelper.base64Encode(Fixtures.CLIENT_TOKEN)
        ) as ClientToken
        val requestSlot = slot<OkHttpRequest>()
        val mockResponse = HttpResponse(body = "{}", timing = HttpResponseTiming(0, 0))
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)
        val sut = BraintreeHttpClient(httpClient, testDispatcher, testScope)
        coEvery { httpClient.sendRequest(capture(requestSlot)) } returns mockResponse

        sut.get("v1/payment_methods", configuration, clientToken, callback)

        advanceUntilIdle()

        val request = requestSlot.captured
        assertEquals("Bearer ${clientToken.bearer}", request.headers["Authorization"])
        verify {
            callback.onResult(match { it is NetworkResponseCallback.Result.Success })
        }
    }

    // POST method tests

    @Test
    fun `when post is called with TokenizationKey, correct request is sent`() = runTest {
        val tokenizationKey = TokenizationKey(Fixtures.TOKENIZATION_KEY)
        val requestSlot = slot<OkHttpRequest>()
        val mockResponse = HttpResponse(body = "{}", timing = HttpResponseTiming(0, 0))
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)
        val sut = BraintreeHttpClient(httpClient, testDispatcher, testScope)
        coEvery { httpClient.sendRequest(capture(requestSlot)) } returns mockResponse

        sut.post("v1/payment_methods", "{\"test\":\"data\"}", configuration, tokenizationKey, callback = callback)

        advanceUntilIdle()

        val request = requestSlot.captured
        assertEquals("https://api.braintreegateway.com/v1/payment_methods", request.url)
        assertTrue(request.method is Method.Post)
        val postMethod = request.method as Method.Post
        assertEquals("{\"test\":\"data\"}", postMethod.body)
        assertEquals("braintree/android/" + BuildConfig.VERSION_NAME, request.headers["User-Agent"])
        assertEquals(Fixtures.TOKENIZATION_KEY, request.headers["Client-Key"])
        verify {
            callback.onResult(match { it is NetworkResponseCallback.Result.Success })
        }
    }

    @Test
    fun `when post is called with ClientToken, authorization fingerprint is added to request body`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)
        val sut = BraintreeHttpClient(httpClient, testDispatcher, testScope)
        val clientToken = Authorization.fromString(
            FixturesHelper.base64Encode(Fixtures.CLIENT_TOKEN)
        ) as ClientToken
        val requestSlot = slot<OkHttpRequest>()
        val mockResponse = HttpResponse(body = "{}", timing = HttpResponseTiming(0, 0))
        coEvery { httpClient.sendRequest(capture(requestSlot)) } returns mockResponse

        sut.post("v1/payment_methods", "{}", configuration, clientToken, callback = callback)

        advanceUntilIdle()

        val request = requestSlot.captured
        val postMethod = request.method as Method.Post
        assertTrue(postMethod.body.contains("authorizationFingerprint"))
        assertTrue(postMethod.body.contains(clientToken.authorizationFingerprint))
        assertEquals("Bearer ${clientToken.bearer}", request.headers["Authorization"])
        assertNull(request.headers["Client-Key"])
        verify {
            callback.onResult(match { it is NetworkResponseCallback.Result.Success })
        }
    }

    @Test
    fun `when post is called with additional headers, headers are included in request`() = runTest {
        val tokenizationKey = TokenizationKey(Fixtures.TOKENIZATION_KEY)
        val additionalHeaders = mapOf("X-Custom-Header" to "custom-value", "Accept" to "application/json")
        val requestSlot = slot<OkHttpRequest>()
        val mockResponse = HttpResponse(body = "{}", timing = HttpResponseTiming(0, 0))
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)
        val sut = BraintreeHttpClient(httpClient, testDispatcher, testScope)
        coEvery { httpClient.sendRequest(capture(requestSlot)) } returns mockResponse

        sut.post(
            "v1/payment_methods",
            "{}",
            configuration,
            tokenizationKey,
            additionalHeaders,
            callback
        )

        advanceUntilIdle()

        val request = requestSlot.captured
        assertEquals("custom-value", request.headers["X-Custom-Header"])
        assertEquals("application/json", request.headers["Accept"])
        verify {
            callback.onResult(match { it is NetworkResponseCallback.Result.Success })
        }
    }

    @Test
    fun `when post is called with absolute URL, configuration base URL is ignored`() = runTest {
        val tokenizationKey = TokenizationKey(Fixtures.TOKENIZATION_KEY)
        val requestSlot = slot<OkHttpRequest>()
        val mockResponse = HttpResponse(body = "{}", timing = HttpResponseTiming(0, 0))
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)
        val sut = BraintreeHttpClient(httpClient, testDispatcher, testScope)
        coEvery { httpClient.sendRequest(capture(requestSlot)) } returns mockResponse

        sut.post("https://example.com/custom/path", "{}", configuration, tokenizationKey, callback = callback)

        advanceUntilIdle()

        val request = requestSlot.captured
        assertEquals("https://example.com/custom/path", request.url)
        verify {
            callback.onResult(match { it is NetworkResponseCallback.Result.Success })
        }
    }

    @Test
    fun `when post is called with null configuration and relative path, BraintreeException is thrown`() = runTest {
        val tokenizationKey = TokenizationKey(Fixtures.TOKENIZATION_KEY)
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)
        val sut = BraintreeHttpClient(httpClient, testDispatcher, testScope)

        sut.post("v1/payment_methods", "{}", null, tokenizationKey, callback = callback)

        verify {
            callback.onResult(match {
                it is NetworkResponseCallback.Result.Failure &&
                    it.error is BraintreeException &&
                    it.error.message!!.contains("relative path")
            })
        }
    }

    @Test
    fun `when post is called with InvalidAuthorization, callback is called with error`() = runTest {
        val invalidAuth = InvalidAuthorization("invalid_token", "Invalid token")
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)
        val sut = BraintreeHttpClient(httpClient, testDispatcher, testScope)

        sut.post("v1/payment_methods", "{}", configuration, invalidAuth, callback = callback)

        verify {
            callback.onResult(match {
                it is NetworkResponseCallback.Result.Failure &&
                    it.error is BraintreeException &&
                    it.error.message == "Invalid token"
            })
        }
    }

    @Test
    fun `when post is called with invalid JSON, callback is called with JSONException`() = runTest {
        val clientToken = Authorization.fromString(
            FixturesHelper.base64Encode(Fixtures.CLIENT_TOKEN)
        ) as ClientToken
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)
        val sut = BraintreeHttpClient(httpClient, testDispatcher, testScope)

        sut.post("v1/payment_methods", "invalid json", configuration, clientToken, callback = callback)

        verify {
            callback.onResult(match {
                it is NetworkResponseCallback.Result.Failure &&
                    it.error is JSONException
            })
        }
    }

    @Test
    fun `when post is called with null callback and InvalidAuthorization, request does not crash`() = runTest {
        val invalidAuth = InvalidAuthorization("invalid_token", "Invalid token")
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)
        val sut = BraintreeHttpClient(httpClient, testDispatcher, testScope)

        sut.post("v1/payment_methods", "{}", configuration, invalidAuth, callback = null)

        // Should not crash
    }

    @Test
    fun `when post is called with null callback and JSON exception occurs, request does not crash`() = runTest {
        val clientToken = Authorization.fromString(
            FixturesHelper.base64Encode(Fixtures.CLIENT_TOKEN)
        ) as ClientToken
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)
        val sut = BraintreeHttpClient(httpClient, testDispatcher, testScope)

        sut.post("v1/payment_methods", "invalid json", configuration, clientToken, callback = null)

        // Should not crash
    }

    @Test
    fun `when post is called with null callback and BraintreeException occurs, request does not crash`() = runTest {
        val tokenizationKey = TokenizationKey(Fixtures.TOKENIZATION_KEY)
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)
        val sut = BraintreeHttpClient(httpClient, testDispatcher, testScope)

        sut.post("v1/payment_methods", "{}", null, tokenizationKey, callback = null)

        // Should not crash
    }

    // Header assembly tests

    @Test
    fun `when assembleHeaders is called, User-Agent header is included`() = runTest {
        val tokenizationKey = TokenizationKey(Fixtures.TOKENIZATION_KEY)
        val requestSlot = slot<OkHttpRequest>()
        val mockResponse = HttpResponse(body = "{}", timing = HttpResponseTiming(0, 0))
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)
        val sut = BraintreeHttpClient(httpClient, testDispatcher, testScope)
        coEvery { httpClient.sendRequest(capture(requestSlot)) } returns mockResponse

        sut.get("v1/payment_methods", configuration, tokenizationKey, callback)

        advanceUntilIdle()

        val request = requestSlot.captured
        assertEquals("braintree/android/" + BuildConfig.VERSION_NAME, request.headers["User-Agent"])
        verify {
            callback.onResult(match { it is NetworkResponseCallback.Result.Success })
        }
    }

    @Test
    fun `when assembleHeaders is called with null bearer, Authorization header is not included`() = runTest {
        val authorization = mockk<Authorization>()
        every { authorization.bearer } returns null
        val requestSlot = slot<OkHttpRequest>()
        val mockResponse = HttpResponse(body = "{}", timing = HttpResponseTiming(0, 0))
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)
        val sut = BraintreeHttpClient(httpClient, testDispatcher, testScope)
        coEvery { httpClient.sendRequest(capture(requestSlot)) } returns mockResponse

        sut.get("v1/payment_methods", configuration, authorization, callback)

        advanceUntilIdle()

        val request = requestSlot.captured
        assertNull(request.headers["Authorization"])
        assertNull(request.headers["Client-Key"])
        verify {
            callback.onResult(match { it is NetworkResponseCallback.Result.Success })
        }
    }

    @Test
    fun `when get is called with null configuration and absolute URL, base URL is not set`() = runTest {
        val tokenizationKey = TokenizationKey(Fixtures.TOKENIZATION_KEY)
        val requestSlot = slot<OkHttpRequest>()
        val mockResponse = HttpResponse(body = "{}", timing = HttpResponseTiming(0, 0))
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)
        val sut = BraintreeHttpClient(httpClient, testDispatcher, testScope)
        coEvery { httpClient.sendRequest(capture(requestSlot)) } returns mockResponse

        sut.get("https://example.com/custom/path", null, tokenizationKey, callback)

        advanceUntilIdle()

        val request = requestSlot.captured
        assertEquals("https://example.com/custom/path", request.url)
        verify {
            callback.onResult(match { it is NetworkResponseCallback.Result.Success })
        }
    }

    @Test
    fun `when post is called with null configuration and absolute URL, base URL is not set`() = runTest {
        val tokenizationKey = TokenizationKey(Fixtures.TOKENIZATION_KEY)
        val requestSlot = slot<OkHttpRequest>()
        val mockResponse = HttpResponse(body = "{}", timing = HttpResponseTiming(0, 0))
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)
        val sut = BraintreeHttpClient(httpClient, testDispatcher, testScope)
        coEvery { httpClient.sendRequest(capture(requestSlot)) } returns mockResponse

        sut.post("https://example.com/custom/path", "{}", null, tokenizationKey, callback = callback)

        advanceUntilIdle()

        val request = requestSlot.captured
        assertEquals("https://example.com/custom/path", request.url)
        verify {
            callback.onResult(match { it is NetworkResponseCallback.Result.Success })
        }
    }
}
