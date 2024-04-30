package com.braintreepayments.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.pm.ActivityInfo;
import android.net.Uri;

import androidx.fragment.app.FragmentActivity;

import org.mockito.invocation.InvocationOnMock;
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
    private Exception authorizationError;

    private String sessionId;
    private String integration;
    private String returnUrlScheme;

    private Uri appLinkUri;

    private BrowserSwitchResult browserSwitchResult;
    private BrowserSwitchException browserSwitchAssertionError;

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

    public MockBraintreeClientBuilder authorizationError(Exception authorizationError) {
        this.authorizationError = authorizationError;
        return this;
    }

    public MockBraintreeClientBuilder deliverBrowserSwitchResult(BrowserSwitchResult browserSwitchResult) {
        this.browserSwitchResult = browserSwitchResult;
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

    public MockBraintreeClientBuilder appLinkUri(Uri appLinkUri) {
        this.appLinkUri = appLinkUri;
        return this;
    }

    public MockBraintreeClientBuilder browserSwitchAssertionError(BrowserSwitchException browserSwitchAssertionError) {
        this.browserSwitchAssertionError = browserSwitchAssertionError;
        return this;
    }

    public MockBraintreeClientBuilder launchesBrowserSwitchAsNewTask(boolean launchesBrowserSwitchAsNewTask) {
        this.launchesBrowserSwitchAsNewTask = launchesBrowserSwitchAsNewTask;
        return this;
    }

    public BraintreeClient build() {
        BraintreeClient braintreeClient = mock(BraintreeClient.class);
        when(braintreeClient.getSessionId()).thenReturn(sessionId);
        when(braintreeClient.getIntegrationType()).thenReturn(integration);

        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) {
                AuthorizationCallback callback = (AuthorizationCallback) invocation.getArguments()[0];
                if (authorization != null) {
                    callback.onAuthorizationResult(authorization, null);
                } else if (authorizationError != null) {
                    callback.onAuthorizationResult(null, authorizationError);
                }
                return null;
            }
        }).when(braintreeClient).getAuthorization(any(AuthorizationCallback.class));

        when(braintreeClient.getReturnUrlScheme()).thenReturn(returnUrlScheme);
        when(braintreeClient.getAppLinkReturnUri()).thenReturn(appLinkUri);

        if (browserSwitchAssertionError != null) {
            try {
                doThrow(browserSwitchAssertionError)
                        .when(braintreeClient).assertCanPerformBrowserSwitch(any(FragmentActivity.class), anyInt());
            } catch (BrowserSwitchException ignored) {}
        }

        when(braintreeClient.getManifestActivityInfo(any())).thenReturn(activityInfo);
        when(braintreeClient.deliverBrowserSwitchResult(any(FragmentActivity.class))).thenReturn(browserSwitchResult);
        when(braintreeClient.launchesBrowserSwitchAsNewTask()).thenReturn(launchesBrowserSwitchAsNewTask);

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
