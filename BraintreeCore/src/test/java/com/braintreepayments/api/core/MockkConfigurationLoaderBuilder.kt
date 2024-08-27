package com.braintreepayments.api.core

import io.mockk.every
import io.mockk.mockk

internal class MockkConfigurationLoaderBuilder {

    private var configuration: Configuration? = null
    private var configurationError: Exception? = null

    fun configuration(configuration: Configuration): MockkConfigurationLoaderBuilder {
        this.configuration = configuration
        return this
    }

    fun configurationError(configurationError: Exception): MockkConfigurationLoaderBuilder {
        this.configurationError = configurationError
        return this
    }

    fun build(): ConfigurationLoader {
        val configurationLoader = mockk<ConfigurationLoader>(relaxed = true)
        every { configurationLoader.loadConfiguration(any(), any()) } answers {
            val callback = secondArg<ConfigurationLoaderCallback>()
            if (configuration != null) {
                callback.onResult(ConfigurationLoaderResponse(configuration = configuration))
            } else if (configurationError != null) {
                callback.onResult(ConfigurationLoaderResponse(error = configurationError))
            }
        }
        return configurationLoader
    }
}
