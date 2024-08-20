package com.braintreepayments.api.core

import com.braintreepayments.api.testutils.Fixtures
import com.braintreepayments.api.testutils.FixturesHelper
import com.braintreepayments.api.sharedutils.HttpClient
import com.braintreepayments.api.sharedutils.HttpMethod
import com.braintreepayments.api.sharedutils.HttpRequest
import com.braintreepayments.api.sharedutils.NetworkResponseCallback
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import org.json.JSONException
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyInt
import org.robolectric.RobolectricTestRunner
import java.net.MalformedURLException
import java.net.URISyntaxException
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.Locale
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
class BraintreeHttpClientUnitTest {

    private lateinit var httpClient: HttpClient
    private lateinit var httpResponseCallback: NetworkResponseCallback

    @Before
    fun beforeEach() {
        httpClient = mockk(relaxed = true)
        httpResponseCallback = mockk()
    }

    @Test
    fun sendRequest_withNullConfiguration_requiresRequestToHaveAnAbsolutePath() {
        val tokenizationKey = mockk<Authorization>()
        val callback = mockk<NetworkResponseCallback>()

        val exceptionSlot = slot<BraintreeException>()
        every { callback.onResult(null, capture(exceptionSlot)) } returns Unit

        val sut = BraintreeHttpClient(httpClient)
        val request = InternalHttpRequest(
            method = HttpMethod.GET,
            path = "sample/path"
        )
        sut.sendRequest(request, authorization = tokenizationKey, callback = callback)

        val exception = exceptionSlot.captured
        assertEquals(
            "Braintree HTTP GET request without configuration cannot have a relative path.",
            exception.message
        )
    }

    @Test
    @Throws(Exception::class)
    fun sendRequest_withNullConfigurationAndAbsoluteURL_doesNotSetABaseURLOnTheRequest() {
        val tokenizationKey: Authorization = TokenizationKey(Fixtures.TOKENIZATION_KEY)
        val callback = mockk<NetworkResponseCallback>()

        val httpRequestSlot = slot<HttpRequest>()
        every {
            httpClient.sendRequest(capture(httpRequestSlot), HttpClient.NO_RETRY, callback)
        } returns Unit

        val sut = BraintreeHttpClient(httpClient)
        val request = InternalHttpRequest(
            method = HttpMethod.GET,
            path = "https://example.com/sample/path"
        )
        sut.sendRequest(request, authorization = tokenizationKey, callback = callback)

        val httpRequest = httpRequestSlot.captured
        assertEquals(URL("https://example.com/sample/path"), httpRequest.url)
    }

    @Test
    @Throws(MalformedURLException::class, URISyntaxException::class)
    fun sendRequest_withTokenizationKey_forwardsHttpRequestToHttpClient() {
        val tokenizationKey: Authorization = TokenizationKey(Fixtures.TOKENIZATION_KEY)
        val configuration = mockk<Configuration>()
        every { configuration.clientApiUrl } returns "https://example.com"

        val httpRequestSlot = slot<HttpRequest>()
        val callback = mockk<NetworkResponseCallback>()
        every {
            httpClient.sendRequest(capture(httpRequestSlot), HttpClient.NO_RETRY, callback)
        } returns Unit

        val sut = BraintreeHttpClient(httpClient)
        val request = InternalHttpRequest(
            method = HttpMethod.GET,
            path = "sample/path"
        )
        sut.sendRequest(request, configuration, tokenizationKey, callback)

        val httpRequest = httpRequestSlot.captured
        val headers = httpRequest.headers
        assertEquals(URL("https://example.com/sample/path"), httpRequest.url)
        assertEquals("braintree/android/" + BuildConfig.VERSION_NAME, headers["User-Agent"])
        assertEquals(Fixtures.TOKENIZATION_KEY, headers["Client-Key"])
        assertEquals(HttpMethod.GET, httpRequest.method)
    }

    @Test
    @Throws(MalformedURLException::class, URISyntaxException::class)
    fun sendRequest_withClientToken_forwardsHttpRequestToHttpClient() {
        val clientToken =
            Authorization.fromString(FixturesHelper.base64Encode(Fixtures.CLIENT_TOKEN))
        val configuration = mockk<Configuration>()
        every { configuration.clientApiUrl } returns "https://example.com"

        val httpRequestSlot = slot<HttpRequest>()
        val callback = mockk<NetworkResponseCallback>()
        every {
            httpClient.sendRequest(capture(httpRequestSlot), HttpClient.NO_RETRY, callback)
        } returns Unit

        val sut = BraintreeHttpClient(httpClient)
        val request = InternalHttpRequest(
            method = HttpMethod.GET,
            path = "sample/path"
        )
        sut.sendRequest(request, configuration, clientToken, callback)

        val httpRequest = httpRequestSlot.captured
        val headers = httpRequest.headers
        val expectedUrlString = String.format(
            Locale.US,
            "https://example.com/sample/path?authorizationFingerprint=%s",
            clientToken.bearer
        )
        assertEquals(URL(expectedUrlString), httpRequest.url)
        assertEquals("braintree/android/" + BuildConfig.VERSION_NAME, headers["User-Agent"])
        assertNull(headers["Client-Key"])
        assertEquals(HttpMethod.GET, httpRequest.method)
    }

    @Test
    fun sendRequest_withInvalidToken_forwardsExceptionToCallback() {
        val authorization: Authorization =
            InvalidAuthorization("invalid", "token invalid")
        val configuration = mockk<Configuration>()

        val exceptionSlot = slot<BraintreeException>()
        val callback = mockk<NetworkResponseCallback>()
        every { callback.onResult(null, capture(exceptionSlot)) } returns Unit

        val sut = BraintreeHttpClient(httpClient)
        val request = InternalHttpRequest(
            method = HttpMethod.GET,
            path = "sample/path"
        )
        sut.sendRequest(request, configuration, authorization, callback)

        val exception = exceptionSlot.captured
        assertEquals("token invalid", exception.message)
    }

    @Test
    @Throws(Exception::class)
    fun sendRequestSync_withTokenizationKey_forwardsPOSTRequestToHttpClient() {
        val tokenizationKey: Authorization = TokenizationKey(Fixtures.TOKENIZATION_KEY)

        val configuration = mockk<Configuration>()
        every { configuration.clientApiUrl } returns "https://example.com"

        val httpRequestSlot = slot<HttpRequest>()
        every { httpClient.sendRequest(capture(httpRequestSlot)) } returns "sample result"

        val sut = BraintreeHttpClient(httpClient)
        val request = InternalHttpRequest(
            method = HttpMethod.POST,
            path = "sample/path",
            data = "{}"
        )
        val result = sut.sendRequestSync(request, configuration, tokenizationKey)
        assertEquals("sample result", result)

        val httpRequest = httpRequestSlot.captured
        val headers = httpRequest.headers
        assertEquals(URL("https://example.com/sample/path"), httpRequest.url)
        assertEquals(
            "braintree/android/" + BuildConfig.VERSION_NAME,
            headers["User-Agent"]
        )
        assertEquals(Fixtures.TOKENIZATION_KEY, headers["Client-Key"])
        assertEquals(HttpMethod.POST, httpRequest.method)
        assertEquals("{}", String(httpRequest.data, StandardCharsets.UTF_8))
    }

    @Test
    @Throws(Exception::class)
    fun sendRequestSync_withClientToken_forwardsPOSTRequestToHttpClient() {
        val clientToken = Authorization.fromString(
            FixturesHelper.base64Encode(Fixtures.CLIENT_TOKEN)
        ) as ClientToken

        val configuration = mockk<Configuration>()
        every { configuration.clientApiUrl } returns "https://example.com"

        val httpRequestSlot = slot<HttpRequest>()
        every { httpClient.sendRequest(capture(httpRequestSlot)) } returns "sample result"

        val sut = BraintreeHttpClient(httpClient)
        val request = InternalHttpRequest(
            method = HttpMethod.POST,
            path = "sample/path",
            data = "{}"
        )
        val result = sut.sendRequestSync(request, configuration, clientToken)
        assertEquals("sample result", result)

        val httpRequest = httpRequestSlot.captured
        val headers = httpRequest.headers
        assertEquals(URL("https://example.com/sample/path"), httpRequest.url)
        assertEquals("braintree/android/" + BuildConfig.VERSION_NAME, headers["User-Agent"])

        assertNull(headers["Client-Key"])
        assertEquals(HttpMethod.POST, httpRequest.method)
        val expectedData =
            """{"authorizationFingerprint":"${clientToken.authorizationFingerprint}"}"""
        assertEquals(expectedData, String(httpRequest.data, StandardCharsets.UTF_8))
    }

    @Test
    fun sendRequestSync_withNullConfiguration_andRelativeUrl_throwsError() {
        val clientToken = Authorization.fromString(
            FixturesHelper.base64Encode(Fixtures.CLIENT_TOKEN)
        ) as ClientToken

        val sut = BraintreeHttpClient(httpClient)
        val request = InternalHttpRequest(
            method = HttpMethod.POST,
            path = "sample/path",
            data = "{}"
        )
        try {
            sut.sendRequestSync(request, null, clientToken)
        } catch (e: Exception) {
            assertTrue(e is BraintreeException)
            assertEquals(
                "Braintree HTTP POST request without configuration cannot have a relative path.",
                e.message
            )
        }
    }

    @Test
    @Throws(Exception::class)
    fun sendRequestSync_withNullConfiguration_andAbsoluteURL_doesNotSetABaseURLOnTheRequest() {
        val clientToken = Authorization.fromString(
            FixturesHelper.base64Encode(Fixtures.CLIENT_TOKEN)
        ) as ClientToken

        val httpRequestSlot = slot<HttpRequest>()
        every { httpClient.sendRequest(capture(httpRequestSlot)) } returns ""

        val sut = BraintreeHttpClient(httpClient)
        val request = InternalHttpRequest(
            method = HttpMethod.POST,
            path = "https://example.com/sample/path",
            data = "{}"
        )
        sut.sendRequestSync(request, null, clientToken)

        val httpRequest = httpRequestSlot.captured
        assertEquals(URL("https://example.com/sample/path"), httpRequest.url)
    }

    @Test
    fun sendRequestSync_withInvalidToken_throwsBraintreeException() {
        val authorization: Authorization =
            InvalidAuthorization("invalid", "token invalid")
        val configuration = mockk<Configuration>()

        val sut = BraintreeHttpClient(httpClient)
        val request = InternalHttpRequest(
            method = HttpMethod.POST,
            path = "https://example.com/sample/path",
            data = "{}"
        )
        try {
            sut.sendRequestSync(request, configuration, authorization)
        } catch (e: Exception) {
            assertTrue(e is BraintreeException)
            assertEquals("token invalid", e.message)
        }
    }

    @Test
    @Throws(MalformedURLException::class, URISyntaxException::class)
    fun sendRequest_withTokenizationKey_forwardsPOSTRequestToHttpClient() {
        val tokenizationKey: Authorization = TokenizationKey(Fixtures.TOKENIZATION_KEY)
        val configuration = mockk<Configuration>()
        every { configuration.clientApiUrl } returns "https://example.com"

        val callback = mockk<NetworkResponseCallback>()
        val httpRequestSlot = slot<HttpRequest>()
        every { httpClient.sendRequest(capture(httpRequestSlot), anyInt(), callback) } returns Unit

        val sut = BraintreeHttpClient(httpClient)
        val request = InternalHttpRequest(
            method = HttpMethod.POST,
            path = "sample/path",
            data = "{}"
        )
        sut.sendRequest(request, configuration, tokenizationKey, callback)

        val httpRequest = httpRequestSlot.captured
        val headers = httpRequest.headers
        assertEquals(URL("https://example.com/sample/path"), httpRequest.url)
        assertEquals("braintree/android/" + BuildConfig.VERSION_NAME, headers["User-Agent"])
        assertEquals(Fixtures.TOKENIZATION_KEY, headers["Client-Key"])
        assertEquals(HttpMethod.POST, httpRequest.method)
        assertEquals("{}", String(httpRequest.data, StandardCharsets.UTF_8))
    }

    @Test
    @Throws(MalformedURLException::class, URISyntaxException::class)
    fun sendRequest_withClientToken_forwardsPOSTRequestToHttpClient() {
        val clientToken = Authorization.fromString(
            FixturesHelper.base64Encode(Fixtures.CLIENT_TOKEN)
        ) as ClientToken

        val configuration = mockk<Configuration>()
        every { configuration.clientApiUrl } returns "https://example.com"

        val callback = mockk<NetworkResponseCallback>()
        val httpRequestSlot = slot<HttpRequest>()
        every { httpClient.sendRequest(capture(httpRequestSlot), anyInt(), callback) } returns Unit

        val sut = BraintreeHttpClient(httpClient)
        val request = InternalHttpRequest(
            method = HttpMethod.POST,
            path = "sample/path",
            data = "{}"
        )
        sut.sendRequest(request, configuration, clientToken, callback)

        val httpRequest = httpRequestSlot.captured
        val headers = httpRequest.headers
        assertEquals(URL("https://example.com/sample/path"), httpRequest.url)
        assertEquals("braintree/android/" + BuildConfig.VERSION_NAME, headers["User-Agent"])
        assertNull(headers["Client-Key"])
        assertEquals(HttpMethod.POST, httpRequest.method)
        val expectedData =
            """{"authorizationFingerprint":"${clientToken.authorizationFingerprint}"}"""
        assertEquals(expectedData, String(httpRequest.data, StandardCharsets.UTF_8))
    }

    @Test
    fun sendRequest_withNullConfiguration_andRelativeUrl_postsCallbackError() {
        val clientToken = Authorization.fromString(
            FixturesHelper.base64Encode(Fixtures.CLIENT_TOKEN)
        ) as ClientToken

        val exceptionSlot = slot<BraintreeException>()
        val callback = mockk<NetworkResponseCallback>()
        every { callback.onResult(null, capture(exceptionSlot)) } returns Unit

        // NOTE: duplicate of sendRequest_withNullConfiguration_requiresRequestToHaveAnAbsolutePath
        val sut = BraintreeHttpClient(httpClient)
        val request = InternalHttpRequest(
            method = HttpMethod.POST,
            path = "sample/path",
            data = "{}"
        )
        sut.sendRequest(request, null, clientToken, callback)

        val exception = exceptionSlot.captured
        assertEquals(
            "Braintree HTTP POST request without configuration cannot have a relative path.",
            exception.message
        )
    }

    @Test
    @Throws(Exception::class)
    fun sendRequest_withNullConfiguration_andAbsoluteURL_doesNotSetABaseURLOnTheRequest() {
        val clientToken = Authorization.fromString(
            FixturesHelper.base64Encode(Fixtures.CLIENT_TOKEN)
        ) as ClientToken

        // NOTE: duplicate of sendRequest_withNullConfigurationAndAbsoluteURL_doesNotSetABaseURLOnTheRequest
        val httpRequestSlot = slot<HttpRequest>()
        val callback = mockk<NetworkResponseCallback>()
        every { httpClient.sendRequest(capture(httpRequestSlot), anyInt(), callback) } returns Unit

        val sut = BraintreeHttpClient(httpClient)
        val request = InternalHttpRequest(
            method = HttpMethod.POST,
            path = "https://example.com/sample/path",
            data = "{}"
        )
        sut.sendRequest(request, null, clientToken, callback)

        val httpRequest = httpRequestSlot.captured
        assertEquals(URL("https://example.com/sample/path"), httpRequest.url)
    }

    @Test
    fun sendRequest_withPathAndDataAndCallback_whenClientTokenAuthAndInvalidJSONPayload_postsCallbackError() {
        val configuration = mockk<Configuration>()
        every { configuration.clientApiUrl } returns "https://example.com"

        val clientToken =
            Authorization.fromString(FixturesHelper.base64Encode(Fixtures.CLIENT_TOKEN))

        val exceptionSlot = slot<JSONException>()
        val callback = mockk<NetworkResponseCallback>()
        every { callback.onResult(null, capture(exceptionSlot)) } returns Unit

        val sut = BraintreeHttpClient(httpClient)
        val request = InternalHttpRequest(
            method = HttpMethod.POST,
            path = "sample/path",
            data = "not json",
        )
        sut.sendRequest(request, configuration, clientToken, callback)

        val exception = exceptionSlot.captured
        assertEquals(
            "Value not of type java.lang.String cannot be converted to JSONObject",
            exception.message
        )
    }

    @Test
    fun sendRequest_withPOSTmethodAndInvalidToken_forwardsExceptionToCallback() {
        val configuration = mockk<Configuration>()
        val authorization: Authorization =
            InvalidAuthorization("invalid", "token invalid")

        // NOTE: duplicate of sendRequest_withInvalidToken_forwardsExceptionToCallback
        val exceptionSlot = slot<BraintreeException>()
        val callback = mockk<NetworkResponseCallback>()
        every { callback.onResult(null, capture(exceptionSlot)) } returns Unit

        val sut = BraintreeHttpClient(httpClient)
        val request = InternalHttpRequest(
            method = HttpMethod.POST,
            path = "sample/path",
            data = "{}",
        )
        sut.sendRequest(request, configuration, authorization, callback)

        val exception = exceptionSlot.captured
        assertEquals("token invalid", exception.message)
    }

    @Test
    fun `when post is called with authorization bearer, Authorization header is added to the request`() {
        val token: String = UUID.randomUUID().toString()
        val tokenizationKey = mockk<Authorization>()
        every { tokenizationKey.bearer } returns token

        val httpRequestSlot = slot<HttpRequest>()
        every { httpClient.sendRequest(capture(httpRequestSlot), anyInt(), any()) } just runs

        val sut = BraintreeHttpClient(httpClient)
        val request = InternalHttpRequest(
            method = HttpMethod.POST,
            path = "sample/path",
            data = "{}",
        )
        sut.sendRequest(
            request,
            configuration = mockk<Configuration>(relaxed = true),
            authorization = tokenizationKey,
            callback = mockk<NetworkResponseCallback>()
        )

        val headers = httpRequestSlot.captured.headers
        assertEquals(headers["Authorization"], "Bearer $token")
    }

    @Test
    fun `when post is called with null bearer, Authorization header is not added to the request`() {
        val tokenizationKey = mockk<Authorization>()
        every { tokenizationKey.bearer } returns null

        val httpRequestSlot = slot<HttpRequest>()
        every { httpClient.sendRequest(capture(httpRequestSlot), anyInt(), any()) } just runs

        val sut = BraintreeHttpClient(httpClient)
        val request = InternalHttpRequest(
            method = HttpMethod.POST,
            path = "sample/path",
            data = "{}",
        )
        sut.sendRequest(
            request,
            configuration = mockk<Configuration>(relaxed = true),
            authorization = tokenizationKey,
            callback = mockk<NetworkResponseCallback>()
        )

        val headers = httpRequestSlot.captured.headers
        assertNull(headers["Authorization"])
    }

    @Test
    fun `when post is called with additional headers, headers are added to the request`() {
        val headers = mapOf("name1" to "value1", "name2" to "value2")
        val callback = mockk<NetworkResponseCallback>()
        val sut = BraintreeHttpClient(httpClient)

        val request = InternalHttpRequest(
            method = HttpMethod.POST,
            path = "sample/path",
            data = "{}",
            additionalHeaders = headers
        )
        sut.sendRequest(
            request,
            configuration = mockk(relaxed = true),
            authorization = mockk(relaxed = true),
            callback = callback
        )

        verify {
            httpClient.sendRequest(withArg {
                assertEquals(it.headers["name1"], "value1")
                assertEquals(it.headers["name2"], "value2")
            }, anyInt(), callback)
        }
    }
}
