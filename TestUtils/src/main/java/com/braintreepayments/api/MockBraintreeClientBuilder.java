package com.braintreepayments.api;

import android.content.pm.ActivityInfo;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MockBraintreeClientBuilder {

    private String sendGETSuccess;
    private Exception sendGETError;

    private String sendPOSTSuccess;
    private Exception sendPOSTError;

    private String sendGraphQLPOSTSuccess;
    private Exception sendGraphQLPOSTError;

    private Configuration configuration;
    private Exception configurationError;

    private Authorization authorization;

    private String sessionId;
    private String integration;

    private ActivityInfo activityInfo;

    private boolean urlSchemeInAndroidManifest = true;

    public MockBraintreeClientBuilder configuration(Configuration configuration) {
        this.configuration = configuration;
        return this;
    }

    public MockBraintreeClientBuilder configurationError(Exception configurationError) {
        this.configurationError = configurationError;
        return this;
    }

    public MockBraintreeClientBuilder authorization(Authorization authorization) {
        this.authorization = authorization;
        return this;
    }

    public MockBraintreeClientBuilder sendGETSuccessfulResponse(String response) {
        sendGETSuccess = response;
        return this;
    }

    public MockBraintreeClientBuilder sendGETErrorResponse(Exception error) {
        sendGETError = error;
        return this;
    }

    public MockBraintreeClientBuilder sendPOSTSuccessfulResponse(String response) {
        sendPOSTSuccess = response;
        return this;
    }

    public MockBraintreeClientBuilder sendPOSTErrorResponse(Exception error) {
        sendPOSTError = error;
        return this;
    }
    public MockBraintreeClientBuilder sendGraphQLPOSTSuccessfulResponse(String response) {
        sendGraphQLPOSTSuccess = response;
        return this;
    }

    public MockBraintreeClientBuilder sendGraphQLPOSTErrorResponse(Exception error) {
        sendGraphQLPOSTError = error;
        return this;
    }

    public MockBraintreeClientBuilder sessionId(String sessionId) {
        this.sessionId = sessionId;
        return this;
    }

    public MockBraintreeClientBuilder integration(String integration) {
        this.integration = integration;
        return this;
    }

    public MockBraintreeClientBuilder urlSchemeDeclaredInManifest(boolean urlSchemeInAndroidManifest) {
        this.urlSchemeInAndroidManifest = urlSchemeInAndroidManifest;
        return this;
    }

    public MockBraintreeClientBuilder activityInfo(ActivityInfo activityInfo) {
        this.activityInfo = activityInfo;
        return this;
    }

    public BraintreeClient build() {
        BraintreeClient braintreeClient = mock(BraintreeClient.class);
        when(braintreeClient.getAuthorization()).thenReturn(authorization);
        when(braintreeClient.getSessionId()).thenReturn(sessionId);
        when(braintreeClient.getIntegrationType()).thenReturn(integration);
        when(braintreeClient.isUrlSchemeDeclaredInAndroidManifest(anyString(), any(Class.class))).thenReturn(urlSchemeInAndroidManifest);
        when(braintreeClient.getManifestActivityInfo(any(Class.class))).thenReturn(activityInfo);

        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                ConfigurationCallback callback = (ConfigurationCallback) invocation.getArguments()[0];
                if (configuration != null) {
                    callback.onResult(configuration, null);
                } else if (configurationError != null) {
                    callback.onResult(null, configurationError);
                }
                return null;
            }
        }).when(braintreeClient).getConfiguration(any(ConfigurationCallback.class));

        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) {
                HttpResponseCallback callback = (HttpResponseCallback) invocation.getArguments()[1];
                if (sendGETSuccess != null) {
                    callback.success(sendGETSuccess);
                } else if (sendGETError != null) {
                    callback.failure(sendGETError);
                }
                return null;
            }
        }).when(braintreeClient).sendGET(anyString(), any(HttpResponseCallback.class));

        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) {
                HttpResponseCallback callback = (HttpResponseCallback) invocation.getArguments()[2];
                if (sendPOSTSuccess != null) {
                    callback.success(sendPOSTSuccess);
                } else if (sendPOSTError != null) {
                    callback.failure(sendPOSTError);
                }
                return null;
            }
        }).when(braintreeClient).sendPOST(anyString(), anyString(), any(HttpResponseCallback.class));

        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                HttpResponseCallback callback = (HttpResponseCallback) invocation.getArguments()[1];
                if (sendGraphQLPOSTSuccess != null) {
                    callback.success(sendGraphQLPOSTSuccess);
                } else if (sendGraphQLPOSTError != null) {
                    callback.failure(sendGraphQLPOSTError);
                }
                return null;
            }
        }).when(braintreeClient).sendGraphQLPOST(anyString(), any(HttpResponseCallback.class));

        return braintreeClient;
    }
}
