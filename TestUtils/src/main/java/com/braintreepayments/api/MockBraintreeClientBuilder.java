package com.braintreepayments.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.pm.ActivityInfo;

import com.braintreepayments.api.sharedutils.HttpResponseCallback;

import org.mockito.stubbing.Answer;

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
    private String returnUrlScheme;


    private ActivityInfo activityInfo;
    private boolean launchesBrowserSwitchAsNewTask;

    public MockBraintreeClientBuilder configuration(Configuration configuration) {
        this.configuration = configuration;
        return this;
    }

    public MockBraintreeClientBuilder configurationError(Exception configurationError) {
        this.configurationError = configurationError;
        return this;
    }

    public MockBraintreeClientBuilder authorizationSuccess(Authorization authorization) {
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

    public MockBraintreeClientBuilder launchesBrowserSwitchAsNewTask(
            boolean launchesBrowserSwitchAsNewTask) {
        this.launchesBrowserSwitchAsNewTask = launchesBrowserSwitchAsNewTask;
        return this;
    }

    public BraintreeClient build() {
        BraintreeClient braintreeClient = mock(BraintreeClient.class);
        when(braintreeClient.getSessionId()).thenReturn(sessionId);
        when(braintreeClient.getIntegrationType()).thenReturn(integration);
        when(braintreeClient.getAuthorization()).thenReturn(authorization);
        when(braintreeClient.getReturnUrlScheme()).thenReturn(returnUrlScheme);
        when(braintreeClient.getManifestActivityInfo(any())).thenReturn(activityInfo);
        when(braintreeClient.launchesBrowserSwitchAsNewTask()).thenReturn(
                launchesBrowserSwitchAsNewTask);

        doAnswer((Answer<Void>) invocation -> {
            ConfigurationCallback callback = (ConfigurationCallback) invocation.getArguments()[0];
            if (configuration != null) {
                callback.onResult(configuration, null);
            } else if (configurationError != null) {
                callback.onResult(null, configurationError);
            }
            return null;
        }).when(braintreeClient).getConfiguration(any(ConfigurationCallback.class));

        doAnswer((Answer<Void>) invocation -> {
            HttpResponseCallback callback = (HttpResponseCallback) invocation.getArguments()[1];
            if (sendGETSuccess != null) {
                callback.onResult(sendGETSuccess, null);
            } else if (sendGETError != null) {
                callback.onResult(null, sendGETError);
            }
            return null;
        }).when(braintreeClient).sendGET(anyString(), any(HttpResponseCallback.class));

        doAnswer((Answer<Void>) invocation -> {
            HttpResponseCallback callback = (HttpResponseCallback) invocation.getArguments()[2];
            if (sendPOSTSuccess != null) {
                callback.onResult(sendPOSTSuccess, null);
            } else if (sendPOSTError != null) {
                callback.onResult(null, sendPOSTError);
            }
            return null;
        }).when(braintreeClient)
                .sendPOST(anyString(), anyString(), any(HttpResponseCallback.class));

        doAnswer((Answer<Void>) invocation -> {
            HttpResponseCallback callback = (HttpResponseCallback) invocation.getArguments()[1];
            if (sendGraphQLPOSTSuccess != null) {
                callback.onResult(sendGraphQLPOSTSuccess, null);
            } else if (sendGraphQLPOSTError != null) {
                callback.onResult(null, sendGraphQLPOSTError);
            }
            return null;
        }).when(braintreeClient).sendGraphQLPOST(anyString(), any(HttpResponseCallback.class));

        return braintreeClient;
    }
}
