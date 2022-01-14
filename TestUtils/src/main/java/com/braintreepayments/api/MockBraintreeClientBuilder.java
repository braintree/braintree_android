package com.braintreepayments.api;

import android.content.pm.ActivityInfo;

import androidx.fragment.app.FragmentActivity;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
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

    private ActivityInfo activityInfo;
    private Authorization authorization;

    private String sessionId;
    private String integration;
    private String returnUrlScheme;

    private boolean urlSchemeInAndroidManifest = true;
    private boolean canPerformBrowserSwitch = true;

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

    public MockBraintreeClientBuilder activityInfo(ActivityInfo activityInfo) {
        this.activityInfo = activityInfo;
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

    public MockBraintreeClientBuilder returnUrlScheme(String returnUrlScheme) {
        this.returnUrlScheme = returnUrlScheme;
         return this;
    }

    public MockBraintreeClientBuilder urlSchemeDeclaredInManifest(boolean urlSchemeInAndroidManifest) {
        this.urlSchemeInAndroidManifest = urlSchemeInAndroidManifest;
        return this;
    }

    public MockBraintreeClientBuilder canPerformBrowserSwitch(boolean canPerformBrowserSwitch) {
        this.canPerformBrowserSwitch = canPerformBrowserSwitch;
        return this;
    }

    public BraintreeClient build() {
        BraintreeClient braintreeClient = mock(BraintreeClient.class);
        when(braintreeClient.getSessionId()).thenReturn(sessionId);
        when(braintreeClient.getIntegrationType()).thenReturn(integration);

        // HACK: some google pay tests fail when getReturnUrlScheme is stubbed but not invoked
        // TODO: create a wrapper around google wallet api to avoid having to use Powermock and Robolectric at the same time, which seems to be causing this issue
        if (returnUrlScheme != null) {
            when(braintreeClient.getReturnUrlScheme()).thenReturn(returnUrlScheme);
        }

        when(braintreeClient.isUrlSchemeDeclaredInAndroidManifest(anyString(), any(Class.class))).thenReturn(urlSchemeInAndroidManifest);
        when(braintreeClient.canPerformBrowserSwitch(any(FragmentActivity.class), anyInt())).thenReturn(canPerformBrowserSwitch);
        when(braintreeClient.getManifestActivityInfo(any(Class.class))).thenReturn(activityInfo);

        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) {
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
                    callback.onResult(sendGETSuccess, null);
                } else if (sendGETError != null) {
                    callback.onResult(null, sendGETError);
                }
                return null;
            }
        }).when(braintreeClient).sendGET(anyString(), any(HttpResponseCallback.class));

        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) {
                HttpResponseCallback callback = (HttpResponseCallback) invocation.getArguments()[2];
                if (sendPOSTSuccess != null) {
                    callback.onResult(sendPOSTSuccess, null);
                } else if (sendPOSTError != null) {
                    callback.onResult(null, sendPOSTError);
                }
                return null;
            }
        }).when(braintreeClient).sendPOST(anyString(), anyString(), any(HttpResponseCallback.class));

        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) {
                HttpResponseCallback callback = (HttpResponseCallback) invocation.getArguments()[1];
                if (sendGraphQLPOSTSuccess != null) {
                    callback.onResult(sendGraphQLPOSTSuccess, null);
                } else if (sendGraphQLPOSTError != null) {
                    callback.onResult(null, sendGraphQLPOSTError);
                }
                return null;
            }
        }).when(braintreeClient).sendGraphQLPOST(anyString(), any(HttpResponseCallback.class));

        return braintreeClient;
    }
}
