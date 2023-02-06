package com.braintreepayments.api

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.json.JSONException
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner
import java.net.MalformedURLException
import java.net.URISyntaxException
import java.net.URL
import java.nio.charset.StandardCharsets

@RunWith(RobolectricTestRunner::class)
class BraintreeHttpClientUnitTest {

    private lateinit var httpClient: HttpClient
    private lateinit var httpResponseCallback: HttpResponseCallback

    @Before
    fun beforeEach() {
        httpClient = mockk()
        httpResponseCallback = mockk()
    }

    @Test
    fun get_withNullConfiguration_requiresRequiresRequestToHaveAnAbsolutePath() {
        val tokenizationKey = mockk<Authorization>()
        val callback = mockk<HttpResponseCallback>()

        val exceptionSlot = slot<BraintreeException>()
        every { callback.onResult(null, capture(exceptionSlot)) } returns Unit

        val sut = BraintreeHttpClient(httpClient)
        sut.get("sample/path", null, tokenizationKey, callback)

        val exception = exceptionSlot.captured
        assertEquals(
            "Braintree HTTP GET request without configuration cannot have a relative path.",
            exception.message
        )
    }

    @Test
    @Throws(Exception::class)
    fun get_withNullConfigurationAndAbsoluteURL_doesNotSetABaseURLOnTheRequest() {
        val tokenizationKey: Authorization = TokenizationKey(Fixtures.TOKENIZATION_KEY)
        val callback = mockk<HttpResponseCallback>()

        val httpRequestSlot = slot<HttpRequest>()
        every {
            httpClient.sendRequest(capture(httpRequestSlot), HttpClient.NO_RETRY, callback)
        } returns Unit

        val sut = BraintreeHttpClient(httpClient)
        sut.get("https://example.com/sample/path", null, tokenizationKey, callback)

        val httpRequest = httpRequestSlot.captured
        assertEquals(URL("https://example.com/sample/path"), httpRequest.url)
    }

    @Test
    @Throws(MalformedURLException::class, URISyntaxException::class)
    fun get_withTokenizationKey_forwardsHttpRequestToHttpClient() {
        val tokenizationKey: Authorization = TokenizationKey(Fixtures.TOKENIZATION_KEY)
        val sut = BraintreeHttpClient(httpClient)
        val configuration = Mockito.mock(
            Configuration::class.java
        )
        Mockito.`when`(configuration.clientApiUrl).thenReturn("https://example.com")
        val callback = Mockito.mock(
            HttpResponseCallback::class.java
        )
        sut["sample/path", configuration, tokenizationKey, callback]
        val captor = ArgumentCaptor.forClass(
            HttpRequest::class.java
        )
        Mockito.verify(httpClient).sendRequest(
            captor.capture(),
            ArgumentMatchers.eq(HttpClient.NO_RETRY),
            ArgumentMatchers.same(callback)
        )
        val httpRequest = captor.value
        val headers = httpRequest.headers
        assertEquals(URL("https://example.com/sample/path"), httpRequest.url)
        assertEquals(
            "braintree/android/" + BuildConfig.VERSION_NAME,
            headers["User-Agent"]
        )
        assertEquals(Fixtures.TOKENIZATION_KEY, headers["Client-Key"])
        assertEquals("GET", httpRequest.method)
    }

    @Test
    @Throws(MalformedURLException::class, URISyntaxException::class)
    fun get_withClientToken_forwardsHttpRequestToHttpClient() {
        val clientToken =
            Authorization.fromString(FixturesHelper.base64Encode(Fixtures.CLIENT_TOKEN))
        val sut = BraintreeHttpClient(httpClient)
        val configuration = Mockito.mock(
            Configuration::class.java
        )
        Mockito.`when`(configuration.clientApiUrl).thenReturn("https://example.com")
        val callback = Mockito.mock(
            HttpResponseCallback::class.java
        )
        sut["sample/path", configuration, clientToken, callback]
        val captor = ArgumentCaptor.forClass(
            HttpRequest::class.java
        )
        Mockito.verify(httpClient).sendRequest(
            captor.capture(),
            ArgumentMatchers.eq(HttpClient.NO_RETRY),
            ArgumentMatchers.same(callback)
        )
        val httpRequest = captor.value
        val headers = httpRequest.headers
        val expectedUrlString = String.format(
            "https://example.com/sample/path?authorizationFingerprint=%s",
            clientToken.bearer
        )
        assertEquals(URL(expectedUrlString), httpRequest.url)
        assertEquals(
            "braintree/android/" + BuildConfig.VERSION_NAME,
            headers["User-Agent"]
        )
        assertNull(headers["Client-Key"])
        assertEquals("GET", httpRequest.method)
    }

    @Test
    fun get_withInvalidToken_forwardsExceptionToCallback() {
        val authorization: Authorization = InvalidAuthorization("invalid", "token invalid")
        val sut = BraintreeHttpClient(httpClient)
        val configuration = Mockito.mock(
            Configuration::class.java
        )
        val callback = Mockito.mock(
            HttpResponseCallback::class.java
        )
        sut["sample/path", configuration, authorization, callback]
        val captor = ArgumentCaptor.forClass(
            BraintreeException::class.java
        )
        Mockito.verify(callback)
            .onResult(ArgumentMatchers.isNull<Any>() as String, captor.capture())
        val exception = captor.value
        assertEquals("token invalid", exception.message)
    }

    @Test
    @Throws(Exception::class)
    fun postSync_withTokenizationKey_forwardsHttpRequestToHttpClient() {
        val tokenizationKey: Authorization = TokenizationKey(Fixtures.TOKENIZATION_KEY)
        val sut = BraintreeHttpClient(httpClient)
        val configuration = Mockito.mock(
            Configuration::class.java
        )
        Mockito.`when`(configuration.clientApiUrl).thenReturn("https://example.com")
        Mockito.`when`(
            httpClient.sendRequest(
                ArgumentMatchers.any(
                    HttpRequest::class.java
                )
            )
        ).thenReturn("sample result")
        val result = sut.post("sample/path", "{}", configuration, tokenizationKey)
        assertEquals("sample result", result)
        val captor = ArgumentCaptor.forClass(
            HttpRequest::class.java
        )
        Mockito.verify(httpClient).sendRequest(captor.capture())
        val httpRequest = captor.value
        val headers = httpRequest.headers
        assertEquals(URL("https://example.com/sample/path"), httpRequest.url)
        assertEquals(
            "braintree/android/" + BuildConfig.VERSION_NAME,
            headers["User-Agent"]
        )
        assertEquals(Fixtures.TOKENIZATION_KEY, headers["Client-Key"])
        assertEquals("POST", httpRequest.method)
        assertEquals("{}", String(httpRequest.data, StandardCharsets.UTF_8))
    }

    @Test
    @Throws(Exception::class)
    fun postSync_withClientToken_forwardsHttpRequestToHttpClient() {
        val clientToken = Authorization.fromString(
            FixturesHelper.base64Encode(Fixtures.CLIENT_TOKEN)
        ) as ClientToken

        val sut = BraintreeHttpClient(httpClient)
        val configuration = Mockito.mock(
            Configuration::class.java
        )
        Mockito.`when`(configuration.clientApiUrl).thenReturn("https://example.com")
        Mockito.`when`(
            httpClient.sendRequest(
                ArgumentMatchers.any(
                    HttpRequest::class.java
                )
            )
        ).thenReturn("sample result")
        val result = sut.post("sample/path", "{}", configuration, clientToken)
        assertEquals("sample result", result)
        val captor = ArgumentCaptor.forClass(
            HttpRequest::class.java
        )
        Mockito.verify(httpClient).sendRequest(captor.capture())
        val httpRequest = captor.value
        val headers = httpRequest.headers
        assertEquals(URL("https://example.com/sample/path"), httpRequest.url)
        assertEquals(
            "braintree/android/" + BuildConfig.VERSION_NAME,
            headers["User-Agent"]
        )
        assertNull(headers["Client-Key"])
        assertEquals("POST", httpRequest.method)
        val expectedData = String.format(
            "{\"authorizationFingerprint\":\"%s\"}",
            clientToken.authorizationFingerprint
        )
        assertEquals(
            expectedData,
            String(httpRequest.data, StandardCharsets.UTF_8)
        )
    }

    @Test
    fun postSync_withNullConfiguration_andRelativeUrl_throwsError() {
        val clientToken = Authorization.fromString(
            FixturesHelper.base64Encode(Fixtures.CLIENT_TOKEN)
        ) as ClientToken
        val sut = BraintreeHttpClient(httpClient)
        try {
            sut.post("sample/path", "{}", null, clientToken)
        } catch (e: Exception) {
            assertTrue(e is BraintreeException)
            assertEquals(
                "Braintree HTTP GET request without configuration cannot have a relative path.",
                e.message
            )
        }
    }

    @Test
    @Throws(Exception::class)
    fun postSync_withNullConfiguration_andAbsoluteURL_doesNotSetABaseURLOnTheRequest() {
        val clientToken = Authorization.fromString(
            FixturesHelper.base64Encode(Fixtures.CLIENT_TOKEN)
        ) as ClientToken
        val sut = BraintreeHttpClient(httpClient)
        sut.post("https://example.com/sample/path", "{}", null, clientToken)
        val captor = ArgumentCaptor.forClass(
            HttpRequest::class.java
        )
        Mockito.verify(httpClient).sendRequest(captor.capture())
        val httpRequest = captor.value
        assertEquals(URL("https://example.com/sample/path"), httpRequest.url)
    }

    @Test
    fun postSync_withInvalidToken_throwsBraintreeException() {
        val authorization: Authorization = InvalidAuthorization("invalid", "token invalid")
        val sut = BraintreeHttpClient(httpClient)
        val configuration = Mockito.mock(
            Configuration::class.java
        )
        try {
            sut.post("https://example.com/sample/path", "{}", configuration, authorization)
        } catch (e: Exception) {
            assertTrue(e is BraintreeException)
            assertEquals("token invalid", e.message)
        }
    }

    @Test
    @Throws(MalformedURLException::class, URISyntaxException::class)
    fun postAsync_withTokenizationKey_forwardsHttpRequestToHttpClient() {
        val tokenizationKey: Authorization = TokenizationKey(Fixtures.TOKENIZATION_KEY)
        val sut = BraintreeHttpClient(httpClient)
        val configuration = Mockito.mock(
            Configuration::class.java
        )
        Mockito.`when`(configuration.clientApiUrl).thenReturn("https://example.com")
        val callback = Mockito.mock(
            HttpResponseCallback::class.java
        )
        sut.post("sample/path", "{}", configuration, tokenizationKey, callback)
        val captor = ArgumentCaptor.forClass(
            HttpRequest::class.java
        )
        Mockito.verify(httpClient).sendRequest(captor.capture(), ArgumentMatchers.same(callback))
        val httpRequest = captor.value
        val headers = httpRequest.headers
        assertEquals(URL("https://example.com/sample/path"), httpRequest.url)
        assertEquals(
            "braintree/android/" + BuildConfig.VERSION_NAME,
            headers["User-Agent"]
        )
        assertEquals(Fixtures.TOKENIZATION_KEY, headers["Client-Key"])
        assertEquals("POST", httpRequest.method)
        assertEquals("{}", String(httpRequest.data, StandardCharsets.UTF_8))
    }

    @Test
    @Throws(MalformedURLException::class, URISyntaxException::class)
    fun postAsync_withClientToken_forwardsHttpRequestToHttpClient() {
        val clientToken = Authorization.fromString(
            FixturesHelper.base64Encode(Fixtures.CLIENT_TOKEN)
        ) as ClientToken

        val sut = BraintreeHttpClient(httpClient)
        val configuration = Mockito.mock(
            Configuration::class.java
        )
        Mockito.`when`(configuration.clientApiUrl).thenReturn("https://example.com")
        val callback = Mockito.mock(
            HttpResponseCallback::class.java
        )
        sut.post("sample/path", "{}", configuration, clientToken, callback)
        val captor = ArgumentCaptor.forClass(
            HttpRequest::class.java
        )
        Mockito.verify(httpClient).sendRequest(captor.capture(), ArgumentMatchers.same(callback))
        val httpRequest = captor.value
        val headers = httpRequest.headers
        assertEquals(URL("https://example.com/sample/path"), httpRequest.url)
        assertEquals(
            "braintree/android/" + BuildConfig.VERSION_NAME,
            headers["User-Agent"]
        )
        assertNull(headers["Client-Key"])
        assertEquals("POST", httpRequest.method)
        val expectedData = String.format(
            "{\"authorizationFingerprint\":\"%s\"}",
            clientToken.authorizationFingerprint
        )
        assertEquals(
            expectedData,
            String(httpRequest.data, StandardCharsets.UTF_8)
        )
    }

    @Test
    fun postAsync_withNullConfiguration_andRelativeUrl_postsCallbackError() {
        val clientToken = Authorization.fromString(
            FixturesHelper.base64Encode(Fixtures.CLIENT_TOKEN)
        ) as ClientToken

        val sut = BraintreeHttpClient(httpClient)
        val callback = Mockito.mock(
            HttpResponseCallback::class.java
        )
        sut.post("sample/path", "{}", null, clientToken, callback)
        val captor = ArgumentCaptor.forClass(
            Exception::class.java
        )
        Mockito.verify(callback)
            .onResult(ArgumentMatchers.isNull<Any>() as String, captor.capture())
        val exception = captor.value
        assertTrue(exception is BraintreeException)
        assertEquals(
            "Braintree HTTP GET request without configuration cannot have a relative path.",
            exception.message
        )
    }

    @Test
    @Throws(Exception::class)
    fun postAsync_withNullConfiguration_andAbsoluteURL_doesNotSetABaseURLOnTheRequest() {
        val clientToken = Authorization.fromString(
            FixturesHelper.base64Encode(Fixtures.CLIENT_TOKEN)
        ) as ClientToken
        val sut = BraintreeHttpClient(httpClient)
        val callback = Mockito.mock(
            HttpResponseCallback::class.java
        )
        sut.post("https://example.com/sample/path", "{}", null, clientToken, callback)
        val captor = ArgumentCaptor.forClass(
            HttpRequest::class.java
        )
        Mockito.verify(httpClient).sendRequest(captor.capture(), ArgumentMatchers.same(callback))
        val httpRequest = captor.value
        assertEquals(URL("https://example.com/sample/path"), httpRequest.url)
    }

    @Test
    fun postAsync_withPathAndDataAndCallback_whenClientTokenAuthAndInvalidJSONPayload_postsCallbackError() {
        val configuration = Mockito.mock(
            Configuration::class.java
        )
        Mockito.`when`(configuration.clientApiUrl).thenReturn("https://example.com")
        val clientToken =
            Authorization.fromString(FixturesHelper.base64Encode(Fixtures.CLIENT_TOKEN))
        val callback = Mockito.mock(
            HttpResponseCallback::class.java
        )
        val sut = BraintreeHttpClient(httpClient)
        sut.post("sample/path", "not json", configuration, clientToken, callback)
        val captor = ArgumentCaptor.forClass(
            Exception::class.java
        )
        Mockito.verify(callback)
            .onResult(ArgumentMatchers.isNull<Any>() as String, captor.capture())
        val exception = captor.value
        assertTrue(exception is JSONException)
        assertEquals(
            "Value not of type java.lang.String cannot be converted to JSONObject",
            exception.message
        )
    }

    @Test
    fun postAsync_withInvalidToken_forwardsExceptionToCallback() {
        val authorization: Authorization = InvalidAuthorization("invalid", "token invalid")
        val sut = BraintreeHttpClient(httpClient)
        val configuration = Mockito.mock(
            Configuration::class.java
        )
        val callback = Mockito.mock(
            HttpResponseCallback::class.java
        )
        sut.post("sample/path", "{}", configuration, authorization, callback)
        val captor = ArgumentCaptor.forClass(
            BraintreeException::class.java
        )
        Mockito.verify(callback)
            .onResult(ArgumentMatchers.isNull<Any>() as String, captor.capture())
        val exception = captor.value
        assertEquals("token invalid", exception.message)
    }
}