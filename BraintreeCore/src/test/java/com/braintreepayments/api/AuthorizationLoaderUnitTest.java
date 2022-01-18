package com.braintreepayments.api;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class AuthorizationLoaderUnitTest {

    private AuthorizationLoader sut;

    @Test
    public void loadAuthorization_whenInitialAuthStringExists_callsBackAuth() {
        String initialAuthString = Fixtures.TOKENIZATION_KEY;
        sut = new AuthorizationLoader(initialAuthString, null);

        AuthorizationCallback callback = mock(AuthorizationCallback.class);
        sut.loadAuthorization(callback);

        ArgumentCaptor<Authorization> captor = ArgumentCaptor.forClass(Authorization.class);
        verify(callback).onAuthorizationResult(captor.capture(), (Exception) isNull());

        Authorization authorization = captor.getValue();
        assertEquals(initialAuthString, authorization.toString());
    }
}
