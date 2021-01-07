package com.braintreepayments.api;

import android.content.Context;

import com.braintreepayments.api.models.Authorization;
import com.braintreepayments.api.models.Configuration;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

public class MockConfigurationManagerBuilder {

    private Configuration configuration;
    private Exception configurationError;

    public MockConfigurationManagerBuilder configuration(Configuration configuration) {
        this.configuration = configuration;
        return this;
    }

    public MockConfigurationManagerBuilder configurationError(Exception configurationError) {
        this.configurationError = configurationError;
        return this;
    }

    public ConfigurationManager build() {
        ConfigurationManager configurationManager = mock(ConfigurationManager.class);

        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) {
                ConfigurationCallback callback = (ConfigurationCallback) invocation.getArguments()[2];
                if (configuration != null) {
                    callback.onResult(configuration, null);
                } else if (configurationError != null) {
                    callback.onResult(null, configurationError);
                }
                return null;
            }
        }).when(configurationManager).loadConfiguration(any(Context.class), any(Authorization.class), any(ConfigurationCallback.class));

        return configurationManager;
    }

}
