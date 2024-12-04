package com.braintreepayments.api.core

import io.mockk.every
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
        every { configurationLoader.loadConfiguration(any()) } answers {
            val callback = firstArg<ConfigurationLoaderCallback>()
            configuration?.let {
                callback.onResult(ConfigurationLoaderResult.Success(it))
            } ?: run {
                callback.onResult(ConfigurationLoaderResult.Failure(configurationError))
            }
        }
        return configurationLoader
    }
}
