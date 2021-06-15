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
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class BraintreeHTTPClientUnitTest {

    private HTTPClient httpClient;
    HTTPResponseCallback httpResponseCallback;

    @Before
    public void beforeEach() {
        httpClient = mock(HTTPClient.class);
        httpResponseCallback = mock(HTTPResponseCallback.class);
    }

    @Test
    public void get_withNullConfiguration_requiresRequiresRequestToHaveAnAbsolutePath() {
        Authorization tokenizationKey = mock(Authorization.class);
        BraintreeHTTPClient sut = new BraintreeHTTPClient(tokenizationKey, httpClient);

        HTTPResponseCallback callback = mock(HTTPResponseCallback.class);
        sut.get("sample/path", null, callback);

        ArgumentCaptor<Exception> captor = ArgumentCaptor.forClass(Exception.class);
        verify(callback).onResult((String) isNull(), captor.capture());

        Exception exception = captor.getValue();
        assertTrue(exception instanceof BraintreeException);
        assertEquals("Braintree HTTP GET request without configuration cannot have a relative path.", exception.getMessage());
    }

    @Test
    public void get_withNullConfigurationAndAbsoluteURL_doesNotSetABaseURLOnTheRequest() throws Exception {
        Authorization tokenizationKey = TokenizationKey.fromString(Fixtures.TOKENIZATION_KEY);
        BraintreeHTTPClient sut = new BraintreeHTTPClient(tokenizationKey, httpClient);

        HTTPResponseCallback callback = mock(HTTPResponseCallback.class);
        sut.get("https://example.com/sample/path", null, callback);

        ArgumentCaptor<HTTPRequest> captor = ArgumentCaptor.forClass(HTTPRequest.class);
        verify(httpClient).sendRequest(captor.capture(), eq(HTTPClient.NO_RETRY), same(callback));

        HTTPRequest httpRequest = captor.getValue();
        assertEquals(new URL("https://example.com/sample/path"), httpRequest.getURL());
    }

    @Test
    public void get_withTokenizationKey_forwardsHTTPRequestToHTTPClient() throws MalformedURLException, URISyntaxException {
        Authorization tokenizationKey = TokenizationKey.fromString(Fixtures.TOKENIZATION_KEY);
        BraintreeHTTPClient sut = new BraintreeHTTPClient(tokenizationKey, httpClient);

        Configuration configuration = mock(Configuration.class);
        when(configuration.getClientApiUrl()).thenReturn("https://example.com");

        HTTPResponseCallback callback = mock(HTTPResponseCallback.class);
        sut.get("sample/path", configuration, callback);

        ArgumentCaptor<HTTPRequest> captor = ArgumentCaptor.forClass(HTTPRequest.class);
        verify(httpClient).sendRequest(captor.capture(), eq(HTTPClient.NO_RETRY), same(callback));

        HTTPRequest httpRequest = captor.getValue();
        Map<String, String> headers = httpRequest.getHeaders();
        assertEquals(new URL("https://example.com/sample/path"), httpRequest.getURL());
        assertEquals("braintree/android/" + BuildConfig.VERSION_NAME, headers.get("User-Agent"));
        assertEquals(Fixtures.TOKENIZATION_KEY, headers.get("Client-Key"));
        assertEquals("GET", httpRequest.getMethod());
    }

    @Test
    public void get_withClientToken_forwardsHTTPRequestToHTTPClient() throws MalformedURLException, URISyntaxException {
        Authorization clientToken = Authorization.fromString(base64Encode(Fixtures.CLIENT_TOKEN));
        BraintreeHTTPClient sut = new BraintreeHTTPClient(clientToken, httpClient);

        Configuration configuration = mock(Configuration.class);
        when(configuration.getClientApiUrl()).thenReturn("https://example.com");

        HTTPResponseCallback callback = mock(HTTPResponseCallback.class);
        sut.get("sample/path", configuration, callback);

        ArgumentCaptor<HTTPRequest> captor = ArgumentCaptor.forClass(HTTPRequest.class);
        verify(httpClient).sendRequest(captor.capture(), eq(HTTPClient.NO_RETRY), same(callback));

        HTTPRequest httpRequest = captor.getValue();
        Map<String, String> headers = httpRequest.getHeaders();
        String expectedUrlString = String.format("https://example.com/sample/path?authorizationFingerprint=%s", clientToken.getBearer());
        assertEquals(new URL(expectedUrlString), httpRequest.getURL());
        assertEquals("braintree/android/" + BuildConfig.VERSION_NAME, headers.get("User-Agent"));
        assertNull(headers.get("Client-Key"));
        assertEquals("GET", httpRequest.getMethod());
    }

    @Test
    public void get_withPayPalUAT_forwardsHTTPRequestToHTTPClient() throws MalformedURLException, URISyntaxException {
        Authorization payPalUAT = Authorization.fromString(Fixtures.BASE64_PAYPAL_UAT);
        BraintreeHTTPClient sut = new BraintreeHTTPClient(payPalUAT, httpClient);

        Configuration configuration = mock(Configuration.class);
        when(configuration.getClientApiUrl()).thenReturn("https://example.com");

        HTTPResponseCallback callback = mock(HTTPResponseCallback.class);
        sut.get("sample/path", configuration, callback);

        ArgumentCaptor<HTTPRequest> captor = ArgumentCaptor.forClass(HTTPRequest.class);
        verify(httpClient).sendRequest(captor.capture(), eq(HTTPClient.NO_RETRY), same(callback));

        HTTPRequest httpRequest = captor.getValue();
        Map<String, String> headers = httpRequest.getHeaders();
        String expectedUrlString = String.format("https://example.com/sample/path?authorizationFingerprint=%s", payPalUAT.getBearer());
        assertEquals(new URL(expectedUrlString), httpRequest.getURL());
        assertEquals("braintree/android/" + BuildConfig.VERSION_NAME, headers.get("User-Agent"));
        assertNull(headers.get("Client-Key"));
        assertEquals("GET", httpRequest.getMethod());
    }

    @Test
    public void get_withInvalidToken_forwardsExceptionToCallback() {
        Authorization authorization = new InvalidAuthorization("invalid", "token invalid");
        BraintreeHTTPClient sut = new BraintreeHTTPClient(authorization, httpClient);

        Configuration configuration = mock(Configuration.class);
        HTTPResponseCallback callback = mock(HTTPResponseCallback.class);
        sut.get("sample/path", configuration, callback);

        ArgumentCaptor<BraintreeException> captor = ArgumentCaptor.forClass(BraintreeException.class);
        verify(callback).onResult((String) isNull(), captor.capture());

        BraintreeException exception = captor.getValue();
        assertEquals("token invalid", exception.getMessage());
    }

    @Test
    public void postSync_withTokenizationKey_forwardsHTTPRequestToHTTPClient() throws Exception {
        Authorization tokenizationKey = TokenizationKey.fromString(Fixtures.TOKENIZATION_KEY);
        BraintreeHTTPClient sut = new BraintreeHTTPClient(tokenizationKey, httpClient);

        Configuration configuration = mock(Configuration.class);
        when(configuration.getClientApiUrl()).thenReturn("https://example.com");

        when(httpClient.sendRequest(any(HTTPRequest.class))).thenReturn("sample result");

        String result = sut.post("sample/path", "{}", configuration);
        assertEquals("sample result", result);

        ArgumentCaptor<HTTPRequest> captor = ArgumentCaptor.forClass(HTTPRequest.class);
        verify(httpClient).sendRequest(captor.capture());

        HTTPRequest httpRequest = captor.getValue();
        Map<String, String> headers = httpRequest.getHeaders();
        assertEquals(new URL("https://example.com/sample/path"), httpRequest.getURL());
        assertEquals("braintree/android/" + BuildConfig.VERSION_NAME, headers.get("User-Agent"));
        assertEquals(Fixtures.TOKENIZATION_KEY, headers.get("Client-Key"));
        assertEquals("POST", httpRequest.getMethod());
        assertEquals("{}", httpRequest.getData());
    }

    @Test
    public void postSync_withClientToken_forwardsHTTPRequestToHTTPClient() throws Exception {
        ClientToken clientToken = (ClientToken) Authorization.fromString(base64Encode(Fixtures.CLIENT_TOKEN));
        BraintreeHTTPClient sut = new BraintreeHTTPClient(clientToken, httpClient);

        Configuration configuration = mock(Configuration.class);
        when(configuration.getClientApiUrl()).thenReturn("https://example.com");

        when(httpClient.sendRequest(any(HTTPRequest.class))).thenReturn("sample result");

        String result = sut.post("sample/path", "{}", configuration);
        assertEquals("sample result", result);

        ArgumentCaptor<HTTPRequest> captor = ArgumentCaptor.forClass(HTTPRequest.class);
        verify(httpClient).sendRequest(captor.capture());

        HTTPRequest httpRequest = captor.getValue();
        Map<String, String> headers = httpRequest.getHeaders();
        assertEquals(new URL("https://example.com/sample/path"), httpRequest.getURL());
        assertEquals("braintree/android/" + BuildConfig.VERSION_NAME, headers.get("User-Agent"));
        assertNull(headers.get("Client-Key"));
        assertEquals("POST", httpRequest.getMethod());

        String expectedData = String.format("{\"authorizationFingerprint\":\"%s\"}", clientToken.getAuthorizationFingerprint());
        assertEquals(expectedData, httpRequest.getData());
    }

    @Test
    public void postSync_withNullConfiguration_andRelativeUrl_throwsError() {
        ClientToken clientToken = (ClientToken) Authorization.fromString(base64Encode(Fixtures.CLIENT_TOKEN));
        BraintreeHTTPClient sut = new BraintreeHTTPClient(clientToken, httpClient);

        try {
            sut.post("sample/path", "{}", null);
        } catch (Exception e) {
            assertTrue(e instanceof BraintreeException);
            assertEquals("Braintree HTTP GET request without configuration cannot have a relative path.", e.getMessage());
        }
    }

    @Test
    public void postSync_withNullConfiguration_andAbsoluteURL_doesNotSetABaseURLOnTheRequest() throws Exception {
        ClientToken clientToken = (ClientToken) Authorization.fromString(base64Encode(Fixtures.CLIENT_TOKEN));
        BraintreeHTTPClient sut = new BraintreeHTTPClient(clientToken, httpClient);

        sut.post("https://example.com/sample/path", "{}", null);

        ArgumentCaptor<HTTPRequest> captor = ArgumentCaptor.forClass(HTTPRequest.class);
        verify(httpClient).sendRequest(captor.capture());

        HTTPRequest httpRequest = captor.getValue();
        assertEquals(new URL("https://example.com/sample/path"), httpRequest.getURL());
    }

    @Test
    public void postSync_withInvalidToken_throwsBraintreeException() {
        Authorization authorization = new InvalidAuthorization("invalid", "token invalid");
        BraintreeHTTPClient sut = new BraintreeHTTPClient(authorization, httpClient);

        Configuration configuration = mock(Configuration.class);
        try {
            sut.post("https://example.com/sample/path", "{}", configuration);
        } catch (Exception e) {
            assertTrue(e instanceof BraintreeException);
            assertEquals("token invalid", e.getMessage());
        }
    }

    @Test
    public void postAsync_withTokenizationKey_forwardsHTTPRequestToHTTPClient() throws MalformedURLException, URISyntaxException {
        Authorization tokenizationKey = TokenizationKey.fromString(Fixtures.TOKENIZATION_KEY);
        BraintreeHTTPClient sut = new BraintreeHTTPClient(tokenizationKey, httpClient);

        Configuration configuration = mock(Configuration.class);
        when(configuration.getClientApiUrl()).thenReturn("https://example.com");

        HTTPResponseCallback callback = mock(HTTPResponseCallback.class);
        sut.post("sample/path", "{}", configuration, callback);

        ArgumentCaptor<HTTPRequest> captor = ArgumentCaptor.forClass(HTTPRequest.class);
        verify(httpClient).sendRequest(captor.capture(), same(callback));

        HTTPRequest httpRequest = captor.getValue();
        Map<String, String> headers = httpRequest.getHeaders();
        assertEquals(new URL("https://example.com/sample/path"), httpRequest.getURL());
        assertEquals("braintree/android/" + BuildConfig.VERSION_NAME, headers.get("User-Agent"));
        assertEquals(Fixtures.TOKENIZATION_KEY, headers.get("Client-Key"));
        assertEquals("POST", httpRequest.getMethod());
        assertEquals("{}", httpRequest.getData());
    }

    @Test
    public void postAsync_withClientToken_forwardsHTTPRequestToHTTPClient() throws MalformedURLException, URISyntaxException {
        ClientToken clientToken = (ClientToken) Authorization.fromString(base64Encode(Fixtures.CLIENT_TOKEN));
        BraintreeHTTPClient sut = new BraintreeHTTPClient(clientToken, httpClient);

        Configuration configuration = mock(Configuration.class);
        when(configuration.getClientApiUrl()).thenReturn("https://example.com");

        HTTPResponseCallback callback = mock(HTTPResponseCallback.class);

        sut.post("sample/path", "{}", configuration, callback);

        ArgumentCaptor<HTTPRequest> captor = ArgumentCaptor.forClass(HTTPRequest.class);
        verify(httpClient).sendRequest(captor.capture(), same(callback));

        HTTPRequest httpRequest = captor.getValue();
        Map<String, String> headers = httpRequest.getHeaders();
        assertEquals(new URL("https://example.com/sample/path"), httpRequest.getURL());
        assertEquals("braintree/android/" + BuildConfig.VERSION_NAME, headers.get("User-Agent"));
        assertNull(headers.get("Client-Key"));
        assertEquals("POST", httpRequest.getMethod());

        String expectedData = String.format("{\"authorizationFingerprint\":\"%s\"}", clientToken.getAuthorizationFingerprint());
        assertEquals(expectedData, httpRequest.getData());
    }

    @Test
    public void postAsync_withNullConfiguration_andRelativeUrl_postsCallbackError() {
        ClientToken clientToken = (ClientToken) Authorization.fromString(base64Encode(Fixtures.CLIENT_TOKEN));
        BraintreeHTTPClient sut = new BraintreeHTTPClient(clientToken, httpClient);

        HTTPResponseCallback callback = mock(HTTPResponseCallback.class);

        sut.post("sample/path", "{}", null, callback);

        ArgumentCaptor<Exception> captor = ArgumentCaptor.forClass(Exception.class);
        verify(callback).onResult((String) isNull(), captor.capture());

        Exception exception = captor.getValue();
        assertTrue(exception instanceof BraintreeException);
        assertEquals("Braintree HTTP GET request without configuration cannot have a relative path.", exception.getMessage());
    }

    @Test
    public void postAsync_withNullConfiguration_andAbsoluteURL_doesNotSetABaseURLOnTheRequest() throws Exception {
        ClientToken clientToken = (ClientToken) Authorization.fromString(base64Encode(Fixtures.CLIENT_TOKEN));
        BraintreeHTTPClient sut = new BraintreeHTTPClient(clientToken, httpClient);

        HTTPResponseCallback callback = mock(HTTPResponseCallback.class);
        sut.post("https://example.com/sample/path", "{}", null, callback);

        ArgumentCaptor<HTTPRequest> captor = ArgumentCaptor.forClass(HTTPRequest.class);
        verify(httpClient).sendRequest(captor.capture(), same(callback));

        HTTPRequest httpRequest = captor.getValue();
        assertEquals(new URL("https://example.com/sample/path"), httpRequest.getURL());
    }

    @Test
    public void postAsync_withPathAndDataAndCallback_whenClientTokenAuthAndInvalidJSONPayload_postsCallbackError() {
        Configuration configuration = mock(Configuration.class);
        when(configuration.getClientApiUrl()).thenReturn("https://example.com");

        Authorization clientToken = Authorization.fromString(base64Encode(Fixtures.CLIENT_TOKEN));
        HTTPResponseCallback callback = mock(HTTPResponseCallback.class);

        BraintreeHTTPClient sut = new BraintreeHTTPClient(clientToken, httpClient);
        sut.post("sample/path", "not json", configuration, callback);

        ArgumentCaptor<Exception> captor = ArgumentCaptor.forClass(Exception.class);
        verify(callback).onResult((String) isNull(), captor.capture());

        Exception exception = captor.getValue();
        assertTrue(exception instanceof JSONException);
        assertEquals("Value not of type java.lang.String cannot be converted to JSONObject",
                exception.getMessage());
    }

    @Test
    public void postAsync_withInvalidToken_forwardsExceptionToCallback() {
        Authorization authorization = new InvalidAuthorization("invalid", "token invalid");
        BraintreeHTTPClient sut = new BraintreeHTTPClient(authorization, httpClient);

        Configuration configuration = mock(Configuration.class);
        HTTPResponseCallback callback = mock(HTTPResponseCallback.class);
        sut.post("sample/path", "{}", configuration, callback);

        ArgumentCaptor<BraintreeException> captor = ArgumentCaptor.forClass(BraintreeException.class);
        verify(callback).onResult((String) isNull(), captor.capture());

        BraintreeException exception = captor.getValue();
        assertEquals("token invalid", exception.getMessage());
    }
}
