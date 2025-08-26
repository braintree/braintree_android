package com.braintreepayments.api.core

import com.braintreepayments.api.sharedutils.HttpClient
import com.braintreepayments.api.sharedutils.Method
import com.braintreepayments.api.sharedutils.NetworkResponseCallback
import com.braintreepayments.api.sharedutils.OkHttpRequest
import com.braintreepayments.api.testutils.Fixtures
import com.braintreepayments.api.testutils.FixturesHelper
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import org.json.JSONException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

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
    fun `when get is called with TokenizationKey, correct request is sent`() {
        val tokenizationKey = TokenizationKey(Fixtures.TOKENIZATION_KEY)
        val requestSlot = slot<OkHttpRequest>()
        every { httpClient.sendRequest(capture(requestSlot), callback) } just runs

        val sut = BraintreeHttpClient(httpClient)
        sut.get("v1/payment_methods", configuration, tokenizationKey, callback)

        val request = requestSlot.captured
        assertEquals("https://api.braintreegateway.com/v1/payment_methods", request.url)
        assertTrue(request.method is Method.Get)
        assertEquals("braintree/android/" + BuildConfig.VERSION_NAME, request.headers["User-Agent"])
        assertEquals(Fixtures.TOKENIZATION_KEY, request.headers["Client-Key"])
    }

    @Test
    fun `when get is called with ClientToken, authorization fingerprint is appended to URL and header`() {
        val clientToken = Authorization.fromString(
            FixturesHelper.base64Encode(Fixtures.CLIENT_TOKEN)
        ) as ClientToken
        val requestSlot = slot<OkHttpRequest>()
        every { httpClient.sendRequest(capture(requestSlot), callback) } just runs

        val sut = BraintreeHttpClient(httpClient)
        sut.get("v1/payment_methods", configuration, clientToken, callback)

        val request = requestSlot.captured
        val expectedUrl =
            "https://api.braintreegateway.com/v1/payment_methods?authorizationFingerprint=${clientToken.bearer}"
        assertEquals(expectedUrl, request.url)
        assertEquals("Bearer ${clientToken.bearer}", request.headers["Authorization"])
        assertNull(request.headers["Client-Key"])
    }

    @Test
    fun `when get is called with absolute URL, configuration base URL is ignored`() {
        val tokenizationKey = TokenizationKey(Fixtures.TOKENIZATION_KEY)
        val requestSlot = slot<OkHttpRequest>()
        every { httpClient.sendRequest(capture(requestSlot), callback) } just runs

        val sut = BraintreeHttpClient(httpClient)
        sut.get("https://example.com/custom/path", configuration, tokenizationKey, callback)

        val request = requestSlot.captured
        assertEquals("https://example.com/custom/path", request.url)
    }

    @Test
    fun `when get is called with null configuration and relative path, BraintreeException is thrown`() {
        val tokenizationKey = TokenizationKey(Fixtures.TOKENIZATION_KEY)
        val errorSlot = slot<BraintreeException>()
        every { callback.onResult(null, capture(errorSlot)) } just runs

        val sut = BraintreeHttpClient(httpClient)
        sut.get("v1/payment_methods", null, tokenizationKey, callback)

        verify { callback.onResult(null, any<BraintreeException>()) }
        assertTrue(errorSlot.captured.message!!.contains("relative path"))
    }

    @Test
    fun `when get is called with InvalidAuthorization, callback is called with error`() {
        val invalidAuth = InvalidAuthorization("invalid_token", "Invalid token")
        val errorSlot = slot<BraintreeException>()
        every { callback.onResult(null, capture(errorSlot)) } just runs

        val sut = BraintreeHttpClient(httpClient)
        sut.get("v1/payment_methods", configuration, invalidAuth, callback)

        verify { callback.onResult(null, any<BraintreeException>()) }
        assertEquals("Invalid token", errorSlot.captured.message)
    }

    @Test
    fun `when get is called with authorization bearer, Authorization header is added`() {
        val clientToken = Authorization.fromString(
            FixturesHelper.base64Encode(Fixtures.CLIENT_TOKEN)
        ) as ClientToken
        val requestSlot = slot<OkHttpRequest>()
        every { httpClient.sendRequest(capture(requestSlot), callback) } just runs

        val sut = BraintreeHttpClient(httpClient)
        sut.get("v1/payment_methods", configuration, clientToken, callback)

        val request = requestSlot.captured
        assertEquals("Bearer ${clientToken.bearer}", request.headers["Authorization"])
    }

    // POST method tests

    @Test
    fun `when post is called with TokenizationKey, correct request is sent`() {
        val tokenizationKey = TokenizationKey(Fixtures.TOKENIZATION_KEY)
        val requestSlot = slot<OkHttpRequest>()
        every { httpClient.sendRequest(capture(requestSlot), callback) } just runs

        val sut = BraintreeHttpClient(httpClient)
        sut.post("v1/payment_methods", "{\"test\":\"data\"}", configuration, tokenizationKey, callback = callback)

        val request = requestSlot.captured
        assertEquals("https://api.braintreegateway.com/v1/payment_methods", request.url)
        assertTrue(request.method is Method.Post)
        val postMethod = request.method as Method.Post
        assertEquals("{\"test\":\"data\"}", postMethod.body)
        assertEquals("braintree/android/" + BuildConfig.VERSION_NAME, request.headers["User-Agent"])
        assertEquals(Fixtures.TOKENIZATION_KEY, request.headers["Client-Key"])
    }

    @Test
    fun `when post is called with ClientToken, authorization fingerprint is added to request body`() {
        val clientToken = Authorization.fromString(
            FixturesHelper.base64Encode(Fixtures.CLIENT_TOKEN)
        ) as ClientToken
        val requestSlot = slot<OkHttpRequest>()
        every { httpClient.sendRequest(capture(requestSlot), callback) } just runs

        val sut = BraintreeHttpClient(httpClient)
        sut.post("v1/payment_methods", "{}", configuration, clientToken, callback = callback)

        val request = requestSlot.captured
        val postMethod = request.method as Method.Post
        assertTrue(postMethod.body.contains("authorizationFingerprint"))
        assertTrue(postMethod.body.contains(clientToken.authorizationFingerprint))
        assertEquals("Bearer ${clientToken.bearer}", request.headers["Authorization"])
        assertNull(request.headers["Client-Key"])
    }

    @Test
    fun `when post is called with additional headers, headers are included in request`() {
        val tokenizationKey = TokenizationKey(Fixtures.TOKENIZATION_KEY)
        val additionalHeaders = mapOf("X-Custom-Header" to "custom-value", "Accept" to "application/json")
        val requestSlot = slot<OkHttpRequest>()
        every { httpClient.sendRequest(capture(requestSlot), callback) } just runs

        val sut = BraintreeHttpClient(httpClient)
        sut.post(
            "v1/payment_methods",
            "{}",
            configuration,
            tokenizationKey,
            additionalHeaders,
            callback
        )

        val request = requestSlot.captured
        assertEquals("custom-value", request.headers["X-Custom-Header"])
        assertEquals("application/json", request.headers["Accept"])
    }

    @Test
    fun `when post is called with absolute URL, configuration base URL is ignored`() {
        val tokenizationKey = TokenizationKey(Fixtures.TOKENIZATION_KEY)
        val requestSlot = slot<OkHttpRequest>()
        every { httpClient.sendRequest(capture(requestSlot), callback) } just runs

        val sut = BraintreeHttpClient(httpClient)
        sut.post("https://example.com/custom/path", "{}", configuration, tokenizationKey, callback = callback)

        val request = requestSlot.captured
        assertEquals("https://example.com/custom/path", request.url)
    }

    @Test
    fun `when post is called with null configuration and relative path, BraintreeException is thrown`() {
        val tokenizationKey = TokenizationKey(Fixtures.TOKENIZATION_KEY)
        val errorSlot = slot<BraintreeException>()
        every { callback.onResult(null, capture(errorSlot)) } just runs

        val sut = BraintreeHttpClient(httpClient)
        sut.post("v1/payment_methods", "{}", null, tokenizationKey, callback = callback)

        verify { callback.onResult(null, any<BraintreeException>()) }
        assertTrue(errorSlot.captured.message!!.contains("relative path"))
    }

    @Test
    fun `when post is called with InvalidAuthorization, callback is called with error`() {
        val invalidAuth = InvalidAuthorization("invalid_token", "Invalid token")
        val errorSlot = slot<BraintreeException>()
        every { callback.onResult(null, capture(errorSlot)) } just runs

        val sut = BraintreeHttpClient(httpClient)
        sut.post("v1/payment_methods", "{}", configuration, invalidAuth, callback = callback)

        verify { callback.onResult(null, any<BraintreeException>()) }
        assertEquals("Invalid token", errorSlot.captured.message)
    }

    @Test
    fun `when post is called with invalid JSON in ClientToken request body, callback is called with JSONException`() {
        val clientToken = Authorization.fromString(
            FixturesHelper.base64Encode(Fixtures.CLIENT_TOKEN)
        ) as ClientToken
        val errorSlot = slot<JSONException>()
        every { callback.onResult(null, capture(errorSlot)) } just runs

        val sut = BraintreeHttpClient(httpClient)
        sut.post("v1/payment_methods", "invalid json", configuration, clientToken, callback = callback)

        verify { callback.onResult(null, any<JSONException>()) }
    }

    @Test
    fun `when post is called with null callback and InvalidAuthorization, request does not crash`() {
        val invalidAuth = InvalidAuthorization("invalid_token", "Invalid token")

        val sut = BraintreeHttpClient(httpClient)
        sut.post("v1/payment_methods", "{}", configuration, invalidAuth, callback = null)

        // Should not crash
    }

    @Test
    fun `when post is called with null callback and JSON exception occurs, request does not crash`() {
        val clientToken = Authorization.fromString(
            FixturesHelper.base64Encode(Fixtures.CLIENT_TOKEN)
        ) as ClientToken

        val sut = BraintreeHttpClient(httpClient)
        sut.post("v1/payment_methods", "invalid json", configuration, clientToken, callback = null)

        // Should not crash
    }

    @Test
    fun `when post is called with null callback and BraintreeException occurs, request does not crash`() {
        val tokenizationKey = TokenizationKey(Fixtures.TOKENIZATION_KEY)

        val sut = BraintreeHttpClient(httpClient)
        sut.post("v1/payment_methods", "{}", null, tokenizationKey, callback = null)

        // Should not crash
    }

    // Header assembly tests

    @Test
    fun `when assembleHeaders is called, User-Agent header is included`() {
        val tokenizationKey = TokenizationKey(Fixtures.TOKENIZATION_KEY)
        val requestSlot = slot<OkHttpRequest>()
        every { httpClient.sendRequest(capture(requestSlot), callback) } just runs

        val sut = BraintreeHttpClient(httpClient)
        sut.get("v1/payment_methods", configuration, tokenizationKey, callback)

        val request = requestSlot.captured
        assertEquals("braintree/android/" + BuildConfig.VERSION_NAME, request.headers["User-Agent"])
    }

    @Test
    fun `when assembleHeaders is called with null bearer, Authorization header is not included`() {
        val authorization = mockk<Authorization>()
        every { authorization.bearer } returns null
        val requestSlot = slot<OkHttpRequest>()
        every { httpClient.sendRequest(capture(requestSlot), callback) } just runs

        val sut = BraintreeHttpClient(httpClient)
        sut.get("v1/payment_methods", configuration, authorization, callback)

        val request = requestSlot.captured
        assertNull(request.headers["Authorization"])
        assertNull(request.headers["Client-Key"])
    }

    @Test
    fun `when get is called with null configuration and absolute URL, base URL is not set`() {
        val tokenizationKey = TokenizationKey(Fixtures.TOKENIZATION_KEY)
        val requestSlot = slot<OkHttpRequest>()
        every { httpClient.sendRequest(capture(requestSlot), callback) } just runs

        val sut = BraintreeHttpClient(httpClient)
        sut.get("https://example.com/custom/path", null, tokenizationKey, callback)

        val request = requestSlot.captured
        assertEquals("https://example.com/custom/path", request.url)
    }

    @Test
    fun `when post is called with null configuration and absolute URL, base URL is not set`() {
        val tokenizationKey = TokenizationKey(Fixtures.TOKENIZATION_KEY)
        val requestSlot = slot<OkHttpRequest>()
        every { httpClient.sendRequest(capture(requestSlot), callback) } just runs

        val sut = BraintreeHttpClient(httpClient)
        sut.post("https://example.com/custom/path", "{}", null, tokenizationKey, callback = callback)

        val request = requestSlot.captured
        assertEquals("https://example.com/custom/path", request.url)
    }
}
