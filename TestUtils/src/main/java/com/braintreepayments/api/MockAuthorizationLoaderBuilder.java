package com.braintreepayments.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import android.content.Context;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class MockAuthorizationLoaderBuilder {

    private Authorization authorization;
    private Exception authorizationError;

    public MockAuthorizationLoaderBuilder authorization(Authorization authorization) {
        this.authorization = authorization;
        return this;
    }

    public MockAuthorizationLoaderBuilder authorizationError(Exception authorizationError) {
        this.authorizationError = authorizationError;
        return this;
    }

    public AuthorizationLoader build() {
        AuthorizationLoader authorizationLoader = mock(AuthorizationLoader.class);

        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) {
                AuthorizationCallback callback = (AuthorizationCallback) invocation.getArguments()[2];
                if (authorization != null) {
                    callback.onAuthorizationResult(authorization, null);
                } else if (authorizationError != null) {
                    callback.onAuthorizationResult(null, authorizationError);
                }
                return null;
            }
        }).when(authorizationLoader).loadAuthorization(any(AuthorizationCallback.class));

        return authorizationLoader;
    }
}
