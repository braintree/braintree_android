package com.braintreepayments.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class AuthorizationLoaderUnitTest {

    private AuthorizationLoader sut;

    @Test
    public void loadAuthorization_whenInitialAuthExists_callsBackAuth() {
        String initialAuthString = Fixtures.TOKENIZATION_KEY;
        sut = new AuthorizationLoader(initialAuthString, null);

        AuthorizationCallback callback = mock(AuthorizationCallback.class);
        sut.loadAuthorization(callback);

        ArgumentCaptor<Authorization> captor = ArgumentCaptor.forClass(Authorization.class);
        verify(callback).onAuthorizationResult(captor.capture(), (Exception) isNull());

        Authorization authorization = captor.getValue();
        assertEquals(initialAuthString, authorization.toString());
    }

    @Test
    public void loadAuthorization_whenInitialAuthDoesNotExist_callsBackSuccessfulClientTokenFetch() {
        String clientToken = Fixtures.BASE64_CLIENT_TOKEN;
        AuthorizationProvider authorizationProvider = new MockAuthorizationProviderBuilder()
                .clientToken(clientToken)
                .build();
        sut = new AuthorizationLoader(null, authorizationProvider);

        AuthorizationCallback callback = mock(AuthorizationCallback.class);
        sut.loadAuthorization(callback);

        ArgumentCaptor<Authorization> captor = ArgumentCaptor.forClass(Authorization.class);
        verify(callback).onAuthorizationResult(captor.capture(), (Exception) isNull());

        Authorization authorization = captor.getValue();
        assertEquals(clientToken, authorization.toString());
    }

    @Test
    public void loadAuthorization_whenInitialAuthDoesNotExist_cachesClientTokenInMemory() {
        String clientToken = Fixtures.BASE64_CLIENT_TOKEN;
        AuthorizationProvider authorizationProvider = new MockAuthorizationProviderBuilder()
                .clientToken(clientToken)
                .build();
        sut = new AuthorizationLoader(null, authorizationProvider);

        AuthorizationCallback callback = mock(AuthorizationCallback.class);
        sut.loadAuthorization(callback);
        sut.loadAuthorization(callback);

        verify(authorizationProvider, times(1)).getClientToken(any(ClientTokenCallback.class));
    }

    @Test
    public void loadAuthorization_whenInitialAuthDoesNotExist_forwardsClientTokenFetchError() {
        Exception clientTokenFetchError = new Exception("error");
        AuthorizationProvider authorizationProvider = new MockAuthorizationProviderBuilder()
                .error(clientTokenFetchError)
                .build();
        sut = new AuthorizationLoader(null, authorizationProvider);

        AuthorizationCallback callback = mock(AuthorizationCallback.class);
        sut.loadAuthorization(callback);

        verify(callback).onAuthorizationResult(null, clientTokenFetchError);
    }

    @Test
    public void loadAuthorization_whenInitialAuthDoesNotExistAndNoClientTokenProvider_callsBackException() {
        sut = new AuthorizationLoader(null, null);

        AuthorizationCallback callback = mock(AuthorizationCallback.class);
        sut.loadAuthorization(callback);

        ArgumentCaptor<Exception> captor = ArgumentCaptor.forClass(Exception.class);
        verify(callback).onAuthorizationResult((Authorization)isNull(), captor.capture());

        Exception error = captor.getValue();
        assertTrue(error instanceof BraintreeException);
        String expectedMessage =
                "Authorization required. See https://developer.paypal.com/braintree/docs/guides/client-sdk/setup/android/v4#initialization for more info.";
        assertEquals(expectedMessage, error.getMessage());
    }

    @Test
    public void getAuthorizationFromCache_returnsInitialAuthorization() {
        String initialAuthString = Fixtures.TOKENIZATION_KEY;
        sut = new AuthorizationLoader(initialAuthString, null);

        Authorization cachedAuth = sut.getAuthorizationFromCache();
        assertNotNull(cachedAuth);
        assertEquals(initialAuthString, cachedAuth.toString());
    }

    @Test
    public void getAuthorizationFromCache_returnsAuthorizationFromClientTokenProvider() {
        String clientToken = Fixtures.BASE64_CLIENT_TOKEN;
        AuthorizationProvider authorizationProvider = new MockAuthorizationProviderBuilder()
                .clientToken(clientToken)
                .build();
        sut = new AuthorizationLoader(null, authorizationProvider);

        AuthorizationCallback callback = mock(AuthorizationCallback.class);
        sut.loadAuthorization(callback);

        Authorization cachedAuth = sut.getAuthorizationFromCache();
        assertNotNull(cachedAuth);
        assertEquals(clientToken, cachedAuth.toString());
    }
}
