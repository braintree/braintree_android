package com.braintreepayments.api.core

import com.braintreepayments.api.testutils.Fixtures
import com.braintreepayments.api.sharedutils.HttpClient
import com.braintreepayments.api.sharedutils.HttpRequest
import com.braintreepayments.api.sharedutils.NetworkResponseCallback
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.json.JSONException
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.net.MalformedURLException
import java.net.URISyntaxException
import java.net.URL
import java.nio.charset.StandardCharsets

@RunWith(RobolectricTestRunner::class)
class BraintreeGraphQLClientUnitTest {

    private lateinit var httpClient: HttpClient
    private lateinit var httpResponseCallback: NetworkResponseCallback
    private lateinit var configuration: Configuration
    private lateinit var authorization: Authorization

    @Before
    @Throws(JSONException::class)
    fun beforeEach() {
        httpClient = mockk()
        httpResponseCallback = mockk()
        authorization = Authorization.fromString(Fixtures.BASE64_CLIENT_TOKEN)
        configuration = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_GRAPHQL)
    }

    @Test
    @Throws(MalformedURLException::class, URISyntaxException::class)
    fun post_withDataAndConfigurationAndCallback_sendsHttpRequest() {
        val httpRequestSlot = slot<HttpRequest>()
        every {
            httpClient.sendRequest(capture(httpRequestSlot), httpResponseCallback)
        } returns Unit

        val sut = BraintreeGraphQLClient(httpClient)
        sut.post("data", configuration, authorization, httpResponseCallback)

        val httpRequest = httpRequestSlot.captured
        assertEquals(URL("https://example-graphql.com/graphql"), httpRequest.url)
        assertEquals("data", String(httpRequest.data ?: ByteArray(0), StandardCharsets.UTF_8))
        assertEquals("POST", httpRequest.method)

        val headers = httpRequest.headers
        assertEquals("braintree/android/" + BuildConfig.VERSION_NAME, headers["User-Agent"])
        assertEquals("Bearer encoded_auth_fingerprint", headers["Authorization"])
        assertEquals("2024-08-23", headers["Braintree-Version"])
    }

    @Test
    fun post_withDataAndConfigurationAndCallback_withInvalidToken_forwardsExceptionToCallback() {
        val authorization = InvalidAuthorization("invalid", "token invalid")

        val exceptionSlot = slot<BraintreeException>()
        every {
            httpResponseCallback.onResult(null, capture(exceptionSlot))
        } returns Unit

        val sut = BraintreeGraphQLClient(httpClient)
        sut.post("sample/path", configuration, authorization, httpResponseCallback)

        val exception = exceptionSlot.captured
        assertEquals("token invalid", exception.message)
    }
}
