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
        ClientTokenProvider clientTokenProvider = new MockClientTokenProviderBuilder()
                .clientToken(clientToken)
                .build();
        sut = new AuthorizationLoader(null, clientTokenProvider);

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
        ClientTokenProvider clientTokenProvider = new MockClientTokenProviderBuilder()
                .clientToken(clientToken)
                .build();
        sut = new AuthorizationLoader(null, clientTokenProvider);

        AuthorizationCallback callback = mock(AuthorizationCallback.class);
        sut.loadAuthorization(callback);
        sut.loadAuthorization(callback);

        verify(clientTokenProvider, times(1)).getClientToken(any(ClientTokenCallback.class));
    }

    @Test
    public void loadAuthorization_whenInitialAuthDoesNotExist_forwardsClientTokenFetchError() {
        Exception clientTokenFetchError = new Exception("error");
        ClientTokenProvider clientTokenProvider = new MockClientTokenProviderBuilder()
                .error(clientTokenFetchError)
                .build();
        sut = new AuthorizationLoader(null, clientTokenProvider);

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
        assertEquals("Unable to fetch client token", error.getMessage());
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
        ClientTokenProvider clientTokenProvider = new MockClientTokenProviderBuilder()
                .clientToken(clientToken)
                .build();
        sut = new AuthorizationLoader(null, clientTokenProvider);

        AuthorizationCallback callback = mock(AuthorizationCallback.class);
        sut.loadAuthorization(callback);

        Authorization cachedAuth = sut.getAuthorizationFromCache();
        assertNotNull(cachedAuth);
        assertEquals(clientToken, cachedAuth.toString());
    }

    @Test
    public void getAuthorizationType_whenAuthIsTokenizationKey_returnsTOKENIZATION() {
        String initialAuthString = Fixtures.TOKENIZATION_KEY;
        sut = new AuthorizationLoader(initialAuthString, null);
        assertEquals(AuthorizationType.TOKENIZATION_KEY, sut.getAuthorizationType());
    }

    @Test
    public void getAuthorizationType_whenAuthIsInvalid_returnsINVALID() {
        String initialAuthString = "invalid string";
        sut = new AuthorizationLoader(initialAuthString, null);
        assertEquals(AuthorizationType.INVALID, sut.getAuthorizationType());
    }

    @Test
    public void getAuthorizationType_whenInitialAuthDoesNotExistAndClientTokenProviderExists_returnsCLIENT_TOKEN() {
        ClientTokenProvider clientTokenProvider = new MockClientTokenProviderBuilder().build();
        sut = new AuthorizationLoader(null, clientTokenProvider);
        assertEquals(AuthorizationType.CLIENT_TOKEN, sut.getAuthorizationType());
    }

    @Test
    public void getAuthorizationType_whenInitialAuthAndClientTokenProviderDoNoExist_returnsINVALID() {
        sut = new AuthorizationLoader(null, null);
        assertEquals(AuthorizationType.INVALID, sut.getAuthorizationType());
    }
}
