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

    private Configuration configuration;
    private Authorization authorization;
    private ActivityInfo activityInfo;

    public MockBraintreeClientBuilder configuration(Configuration configuration) {
        this.configuration = configuration;
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

    public BraintreeClient build() {
        BraintreeClient braintreeClient = mock(BraintreeClient.class);
        when(braintreeClient.getAuthorization()).thenReturn(authorization);
        when(braintreeClient.isUrlSchemeDeclaredInAndroidManifest(anyString(), any(Class.class))).thenReturn(true);
        when(braintreeClient.getManifestActivityInfo(any(Class.class))).thenReturn(activityInfo);

        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) {
                ConfigurationCallback callback = (ConfigurationCallback) invocation.getArguments()[0];
                if (configuration != null) {
                    callback.onResult(configuration, null);
                }
                return null;
            }
        }).when(braintreeClient).getConfiguration(any(ConfigurationCallback.class));

        return braintreeClient;
    }
}
