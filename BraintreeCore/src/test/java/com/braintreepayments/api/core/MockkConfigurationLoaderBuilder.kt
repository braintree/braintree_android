package com.braintreepayments.api.core

import io.mockk.coEvery
import io.mockk.mockk

internal class MockkConfigurationLoaderBuilder {

    private var configuration: Configuration? = null
    private lateinit var configurationError: Exception
    private var configurationsByAuthorization: Map<Authorization, Configuration> = emptyMap()

    fun configuration(configuration: Configuration): MockkConfigurationLoaderBuilder {
        this.configuration = configuration
        return this
    }

    fun configurationsByAuthorization(
        configurations: Map<Authorization, Configuration>
    ): MockkConfigurationLoaderBuilder {
        configurationsByAuthorization = configurations
        return this
    }

    fun configurationError(configurationError: Exception): MockkConfigurationLoaderBuilder {
        this.configurationError = configurationError
        return this
    }

    fun build(): ConfigurationLoader {
        val configurationLoader = mockk<ConfigurationLoader>(relaxed = true)
        coEvery { configurationLoader.loadConfiguration(any()) } answers {
            resultFor(firstArg())
        }
        coEvery { configurationLoader.loadConfiguration(any(), any()) } answers {
            resultFor(firstArg())
        }
        return configurationLoader
    }

    private fun resultFor(authorization: Authorization): ConfigurationLoaderResult {
        val matchedConfiguration = configurationsByAuthorization[authorization] ?: configuration
        return matchedConfiguration?.let { ConfigurationLoaderResult.Success(it) }
            ?: ConfigurationLoaderResult.Failure(configurationError)
    }
}
