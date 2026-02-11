package com.braintreepayments.api.core

import io.mockk.coEvery
import io.mockk.mockk

internal class MockkConfigurationLoaderBuilder {

    private var configuration: Configuration? = null
    private lateinit var configurationError: Exception

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
        coEvery { configurationLoader.loadConfiguration() } answers {
            configuration?.let {
                ConfigurationLoaderResult.Success(it)
            } ?: ConfigurationLoaderResult.Failure(configurationError)
        }
        return configurationLoader
    }
}
