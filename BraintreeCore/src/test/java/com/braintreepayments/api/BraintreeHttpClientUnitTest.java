package com.braintreepayments.api;

import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.robolectric.RobolectricTestRunner;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Map;

import static com.braintreepayments.api.FixturesHelper.base64Encode;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class BraintreeHttpClientUnitTest {

    private HttpClient httpClient;
    HttpResponseCallback httpResponseCallback;

    @Before
    public void beforeEach() {
        httpClient = mock(HttpClient.class);
        httpResponseCallback = mock(HttpResponseCallback.class);
    }

    @Test
    public void get_withNullConfiguration_requiresRequiresRequestToHaveAnAbsolutePath() {
        Authorization tokenizationKey = mock(Authorization.class);
        BraintreeHttpClient sut = new BraintreeHttpClient(httpClient);

        HttpResponseCallback callback = mock(HttpResponseCallback.class);
        sut.get("sample/path", null, callback, tokenizationKey);

        ArgumentCaptor<Exception> captor = ArgumentCaptor.forClass(Exception.class);
        verify(callback).failure(captor.capture());

        Exception exception = captor.getValue();
        assertTrue(exception instanceof BraintreeException);
        assertEquals("Braintree HTTP GET request without configuration cannot have a relative path.", exception.getMessage());
    }

    @Test
    public void get_withTokenizationKey_forwardsHttpRequestToHttpClient() throws InvalidArgumentException, MalformedURLException, URISyntaxException {
        Authorization tokenizationKey = TokenizationKey.fromString(Fixtures.TOKENIZATION_KEY);
        BraintreeHttpClient sut = new BraintreeHttpClient(httpClient);

        Configuration configuration = mock(Configuration.class);
        when(configuration.getClientApiUrl()).thenReturn("https://example.com");

        HttpResponseCallback callback = mock(HttpResponseCallback.class);
        sut.get("sample/path", configuration, callback, tokenizationKey);

        ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient).sendRequest(captor.capture(), eq(HttpClient.NO_RETRY), same(callback));

        HttpRequest httpRequest = captor.getValue();
        Map<String, String> headers = httpRequest.getHeaders();
        assertEquals(new URL("https://example.com/sample/path"), httpRequest.getURL());
        assertEquals("braintree/android/" + BuildConfig.VERSION_NAME, headers.get("User-Agent"));
        assertEquals(Fixtures.TOKENIZATION_KEY, headers.get("Client-Key"));
        assertEquals("GET", httpRequest.getMethod());
    }

    @Test
    public void get_withClientToken_forwardsHttpRequestToHttpClient() throws InvalidArgumentException, MalformedURLException, URISyntaxException {
        Authorization clientToken = Authorization.fromString(base64Encode(Fixtures.CLIENT_TOKEN));
        BraintreeHttpClient sut = new BraintreeHttpClient(httpClient);

        Configuration configuration = mock(Configuration.class);
        when(configuration.getClientApiUrl()).thenReturn("https://example.com");

        HttpResponseCallback callback = mock(HttpResponseCallback.class);
        sut.get("sample/path", configuration, callback, clientToken);

        ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient).sendRequest(captor.capture(), eq(HttpClient.NO_RETRY), same(callback));

        HttpRequest httpRequest = captor.getValue();
        Map<String, String> headers = httpRequest.getHeaders();
        String expectedUrlString = String.format("https://example.com/sample/path?authorizationFingerprint=%s", clientToken.getBearer());
        assertEquals(new URL(expectedUrlString), httpRequest.getURL());
        assertEquals("braintree/android/" + BuildConfig.VERSION_NAME, headers.get("User-Agent"));
        assertNull(headers.get("Client-Key"));
        assertEquals("GET", httpRequest.getMethod());
    }

    @Test
    public void get_withPayPalUAT_forwardsHttpRequestToHttpClient() throws InvalidArgumentException, MalformedURLException, URISyntaxException {
        Authorization payPalUAT = Authorization.fromString(Fixtures.BASE64_PAYPAL_UAT);
        BraintreeHttpClient sut = new BraintreeHttpClient(httpClient);

        Configuration configuration = mock(Configuration.class);
        when(configuration.getClientApiUrl()).thenReturn("https://example.com");

        HttpResponseCallback callback = mock(HttpResponseCallback.class);
        sut.get("sample/path", configuration, callback, payPalUAT);

        ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient).sendRequest(captor.capture(), eq(HttpClient.NO_RETRY), same(callback));

        HttpRequest httpRequest = captor.getValue();
        Map<String, String> headers = httpRequest.getHeaders();
        String expectedUrlString = String.format("https://example.com/sample/path?authorizationFingerprint=%s", payPalUAT.getBearer());
        assertEquals(new URL(expectedUrlString), httpRequest.getURL());
        assertEquals("braintree/android/" + BuildConfig.VERSION_NAME, headers.get("User-Agent"));
        assertNull(headers.get("Client-Key"));
        assertEquals("GET", httpRequest.getMethod());
    }

    @Test
    public void postSync_withTokenizationKey_forwardsHttpRequestToHttpClient() throws Exception {
        Authorization tokenizationKey = TokenizationKey.fromString(Fixtures.TOKENIZATION_KEY);
        BraintreeHttpClient sut = new BraintreeHttpClient(httpClient);

        Configuration configuration = mock(Configuration.class);
        when(configuration.getClientApiUrl()).thenReturn("https://example.com");

        when(httpClient.sendRequest(any(HttpRequest.class))).thenReturn("sample result");

        String result = sut.post("sample/path", "{}", configuration, tokenizationKey);
        assertEquals("sample result", result);

        ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient).sendRequest(captor.capture());

        HttpRequest httpRequest = captor.getValue();
        Map<String, String> headers = httpRequest.getHeaders();
        assertEquals(new URL("https://example.com/sample/path"), httpRequest.getURL());
        assertEquals("braintree/android/" + BuildConfig.VERSION_NAME, headers.get("User-Agent"));
        assertEquals(Fixtures.TOKENIZATION_KEY, headers.get("Client-Key"));
        assertEquals("POST", httpRequest.getMethod());
        assertEquals("{}", httpRequest.getData());
    }

    @Test
    public void postSync_withClientToken_forwardsHttpRequestToHttpClient() throws Exception {
        ClientToken clientToken = (ClientToken) Authorization.fromString(base64Encode(Fixtures.CLIENT_TOKEN));
        BraintreeHttpClient sut = new BraintreeHttpClient(httpClient);

        Configuration configuration = mock(Configuration.class);
        when(configuration.getClientApiUrl()).thenReturn("https://example.com");

        when(httpClient.sendRequest(any(HttpRequest.class))).thenReturn("sample result");

        String result = sut.post("sample/path", "{}", configuration, clientToken);
        assertEquals("sample result", result);

        ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient).sendRequest(captor.capture());

        HttpRequest httpRequest = captor.getValue();
        Map<String, String> headers = httpRequest.getHeaders();
        assertEquals(new URL("https://example.com/sample/path"), httpRequest.getURL());
        assertEquals("braintree/android/" + BuildConfig.VERSION_NAME, headers.get("User-Agent"));
        assertNull(headers.get("Client-Key"));
        assertEquals("POST", httpRequest.getMethod());

        String expectedData = String.format("{\"authorizationFingerprint\":\"%s\"}", clientToken.getAuthorizationFingerprint());
        assertEquals(expectedData, httpRequest.getData());
    }

    @Test
    public void postSync_withNullConfiguration_andRelativeUrl_throwsError() throws InvalidArgumentException {
        ClientToken clientToken = (ClientToken) Authorization.fromString(base64Encode(Fixtures.CLIENT_TOKEN));
        BraintreeHttpClient sut = new BraintreeHttpClient(httpClient);

        try {
            sut.post("sample/path", "{}", null, clientToken);
        } catch (Exception e) {
            assertTrue(e instanceof BraintreeException);
            assertEquals("Braintree HTTP GET request without configuration cannot have a relative path.", e.getMessage());
        }
    }

    @Test
    public void postAsync_withTokenizationKey_forwardsHttpRequestToHttpClient() throws InvalidArgumentException, MalformedURLException, URISyntaxException {
        Authorization tokenizationKey = TokenizationKey.fromString(Fixtures.TOKENIZATION_KEY);
        BraintreeHttpClient sut = new BraintreeHttpClient(httpClient);

        Configuration configuration = mock(Configuration.class);
        when(configuration.getClientApiUrl()).thenReturn("https://example.com");

        HttpResponseCallback callback = mock(HttpResponseCallback.class);
        sut.post("sample/path", "{}", configuration, callback, tokenizationKey);

        ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient).sendRequest(captor.capture(), same(callback));

        HttpRequest httpRequest = captor.getValue();
        Map<String, String> headers = httpRequest.getHeaders();
        assertEquals(new URL("https://example.com/sample/path"), httpRequest.getURL());
        assertEquals("braintree/android/" + BuildConfig.VERSION_NAME, headers.get("User-Agent"));
        assertEquals(Fixtures.TOKENIZATION_KEY, headers.get("Client-Key"));
        assertEquals("POST", httpRequest.getMethod());
        assertEquals("{}", httpRequest.getData());
    }

    @Test
    public void postAsync_withClientToken_forwardsHttpRequestToHttpClient() throws InvalidArgumentException, MalformedURLException, URISyntaxException {
        ClientToken clientToken = (ClientToken) Authorization.fromString(base64Encode(Fixtures.CLIENT_TOKEN));
        BraintreeHttpClient sut = new BraintreeHttpClient(httpClient);

        Configuration configuration = mock(Configuration.class);
        when(configuration.getClientApiUrl()).thenReturn("https://example.com");

        HttpResponseCallback callback = mock(HttpResponseCallback.class);

        sut.post("sample/path", "{}", configuration, callback, clientToken);

        ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient).sendRequest(captor.capture(), same(callback));

        HttpRequest httpRequest = captor.getValue();
        Map<String, String> headers = httpRequest.getHeaders();
        assertEquals(new URL("https://example.com/sample/path"), httpRequest.getURL());
        assertEquals("braintree/android/" + BuildConfig.VERSION_NAME, headers.get("User-Agent"));
        assertNull(headers.get("Client-Key"));
        assertEquals("POST", httpRequest.getMethod());

        String expectedData = String.format("{\"authorizationFingerprint\":\"%s\"}", clientToken.getAuthorizationFingerprint());
        assertEquals(expectedData, httpRequest.getData());
    }

    @Test
    public void postAsync_withNullConfiguration_andRelativeUrl_postsCallbackError() throws InvalidArgumentException {
        ClientToken clientToken = (ClientToken) Authorization.fromString(base64Encode(Fixtures.CLIENT_TOKEN));
        BraintreeHttpClient sut = new BraintreeHttpClient(httpClient);

        HttpResponseCallback callback = mock(HttpResponseCallback.class);

        sut.post("sample/path", "{}", null, callback, clientToken);

        ArgumentCaptor<Exception> captor = ArgumentCaptor.forClass(Exception.class);
        verify(callback).failure(captor.capture());

        Exception exception = captor.getValue();
        assertTrue(exception instanceof BraintreeException);
        assertEquals("Braintree HTTP GET request without configuration cannot have a relative path.", exception.getMessage());
    }

    @Test
    public void post_withPathAndDataAndCallback_whenClientTokenAuthAndInvalidJSONPayload_postsCallbackError() throws InvalidArgumentException {
        Configuration configuration = mock(Configuration.class);
        when(configuration.getClientApiUrl()).thenReturn("https://example.com");

        Authorization clientToken = Authorization.fromString(base64Encode(Fixtures.CLIENT_TOKEN));
        HttpResponseCallback callback = mock(HttpResponseCallback.class);

        BraintreeHttpClient sut = new BraintreeHttpClient(httpClient);
        sut.post("sample/path", "not json", configuration, callback, clientToken);

        ArgumentCaptor<Exception> captor = ArgumentCaptor.forClass(Exception.class);
        verify(callback).failure(captor.capture());

        Exception exception = captor.getValue();
        assertTrue(exception instanceof JSONException);
        assertEquals("Value not of type java.lang.String cannot be converted to JSONObject",
                exception.getMessage());
    }
}
