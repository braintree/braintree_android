package com.braintreepayments.api;

import android.content.Context;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

public class MockConfigurationLoaderBuilder {

    private Configuration configuration;
    private Exception configurationError;
    private Exception loadFromCacheError;
    private Exception saveToCacheError;

    public MockConfigurationLoaderBuilder configuration(Configuration configuration) {
        this.configuration = configuration;
        return this;
    }

    public MockConfigurationLoaderBuilder configurationError(Exception configurationError) {
        this.configurationError = configurationError;
        return this;
    }

    public MockConfigurationLoaderBuilder loadFromCacheError(Exception loadFromCacheError) {
        this.loadFromCacheError = loadFromCacheError;
        return this;
    }

    public MockConfigurationLoaderBuilder saveToCacheError(Exception saveToCacheError) {
        this.saveToCacheError = saveToCacheError;
        return this;
    }

    public ConfigurationLoader build() {
        ConfigurationLoader configurationLoader = mock(ConfigurationLoader.class);

        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) {
                ConfigurationLoaderCallback callback = (ConfigurationLoaderCallback) invocation.getArguments()[2];
                if (configuration != null) {
                    ConfigurationLoaderResult result =
                        new ConfigurationLoaderResult(configuration, loadFromCacheError, saveToCacheError);
                    callback.onResult(result, null);
                } else if (configurationError != null) {
                    callback.onResult(null, configurationError);
                }
                return null;
            }
        }).when(configurationLoader).loadConfiguration(any(Context.class), any(Authorization.class), any(ConfigurationLoaderCallback.class));

        return configurationLoader;
    }

}
