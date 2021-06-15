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

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class BraintreeGraphQLHttpClientUnitTest {

    private HttpClient httpClient;
    private HttpResponseCallback httpResponseCallback;

    private Configuration configuration;
    private Authorization authorization;

    @Before
    public void beforeEach() throws JSONException {
        httpClient = mock(HttpClient.class);
        httpResponseCallback = mock(HttpResponseCallback.class);

        authorization = Authorization.fromString(Fixtures.BASE64_CLIENT_TOKEN);
        configuration = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_GRAPHQL);
    }

    @Test
    public void post_withPathAndDataAndConfigurationAndCallback_sendsHttpRequest() throws MalformedURLException, URISyntaxException {
        BraintreeGraphQLHttpClient sut = new BraintreeGraphQLHttpClient(authorization, httpClient);
        sut.post("sample/path", "data", configuration, httpResponseCallback);

        ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient).sendRequest(captor.capture(), same(httpResponseCallback));

        HttpRequest httpRequest = captor.getValue();
        assertEquals(new URL("https://example-graphql.com/graphql/sample/path"), httpRequest.getURL());
        assertEquals("data", httpRequest.getData());
        assertEquals("POST", httpRequest.getMethod());

        Map<String, String> headers = httpRequest.getHeaders();
        assertEquals("braintree/android/" + BuildConfig.VERSION_NAME, headers.get("User-Agent"));
        assertEquals("Bearer encoded_auth_fingerprint", headers.get("Authorization"));
        assertEquals("2018-03-06", headers.get("Braintree-Version"));
    }

    @Test
    public void post_withDataAndConfigurationAndCallback_sendsHttpRequest() throws MalformedURLException, URISyntaxException {
        BraintreeGraphQLHttpClient sut = new BraintreeGraphQLHttpClient(authorization, httpClient);
        sut.post("data", configuration, httpResponseCallback);

        ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient).sendRequest(captor.capture(), same(httpResponseCallback));

        HttpRequest httpRequest = captor.getValue();
        assertEquals(new URL("https://example-graphql.com/graphql"), httpRequest.getURL());
        assertEquals("data", httpRequest.getData());
        assertEquals("POST", httpRequest.getMethod());

        Map<String, String> headers = httpRequest.getHeaders();
        assertEquals("braintree/android/" + BuildConfig.VERSION_NAME, headers.get("User-Agent"));
        assertEquals("Bearer encoded_auth_fingerprint", headers.get("Authorization"));
        assertEquals("2018-03-06", headers.get("Braintree-Version"));
    }

    @Test
    public void post_withPathAndDataAndConfiguration_sendsHttpRequest() throws Exception {
        when(httpClient.sendRequest(any(HttpRequest.class))).thenReturn("sample response");

        BraintreeGraphQLHttpClient sut = new BraintreeGraphQLHttpClient(authorization, httpClient);
        String result = sut.post("sample/path", "data", configuration);
        assertEquals("sample response", result);

        ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient).sendRequest(captor.capture());

        HttpRequest httpRequest = captor.getValue();
        assertEquals(new URL("https://example-graphql.com/graphql/sample/path"), httpRequest.getURL());
        assertEquals("data", httpRequest.getData());
        assertEquals("POST", httpRequest.getMethod());

        Map<String, String> headers = httpRequest.getHeaders();
        assertEquals("braintree/android/" + BuildConfig.VERSION_NAME, headers.get("User-Agent"));
        assertEquals("Bearer encoded_auth_fingerprint", headers.get("Authorization"));
        assertEquals("2018-03-06", headers.get("Braintree-Version"));
    }

    @Test
    public void post_withPathAndDataAndConfigurationAndCallback_withInvalidToken_forwardsExceptionToCallback() {
        Authorization authorization = new InvalidAuthorization("invalid", "token invalid");

        BraintreeGraphQLHttpClient sut = new BraintreeGraphQLHttpClient(authorization, httpClient);
        sut.post("sample/path", "data", configuration, httpResponseCallback);

        ArgumentCaptor<BraintreeException> captor = ArgumentCaptor.forClass(BraintreeException.class);
        verify(httpResponseCallback).onResult((String) isNull(), captor.capture());

        BraintreeException exception = captor.getValue();
        assertEquals("token invalid", exception.getMessage());
    }

    @Test
    public void post_withDataAndConfigurationAndCallback_withInvalidToken_forwardsExceptionToCallback() {
        Authorization authorization = new InvalidAuthorization("invalid", "token invalid");

        BraintreeGraphQLHttpClient sut = new BraintreeGraphQLHttpClient(authorization, httpClient);
        sut.post("sample/path",  configuration, httpResponseCallback);

        ArgumentCaptor<BraintreeException> captor = ArgumentCaptor.forClass(BraintreeException.class);
        verify(httpResponseCallback).onResult((String) isNull(), captor.capture());

        BraintreeException exception = captor.getValue();
        assertEquals("token invalid", exception.getMessage());
    }

    @Test
    public void post_withPathAndDataAndConfiguration_withInvalidToken_throwsBraintreeException() throws Exception {
        Authorization authorization = new InvalidAuthorization("invalid", "token invalid");

        BraintreeGraphQLHttpClient sut = new BraintreeGraphQLHttpClient(authorization, httpClient);

        try {
            sut.post("sample/path", "data", configuration);
        } catch (BraintreeException e) {
            assertEquals("token invalid", e.getMessage());
        }
    }

}