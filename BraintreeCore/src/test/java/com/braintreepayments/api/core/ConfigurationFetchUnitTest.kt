package com.braintreepayments.api.core

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ConfigurationFetchUnitTest {

    private val configurationLoader: ConfigurationLoader = mockk(relaxed = true)
    private val authorization: Authorization = mockk(relaxed = true)

    @Test
    fun `fetch on success calls back with configuration`() = runTest {
        val expectedConfiguration: Configuration = mockk()
        coEvery {
            configurationLoader.loadConfiguration(authorization)
        } returns ConfigurationLoaderResult.Success(expectedConfiguration)

        var capturedConfiguration: Configuration? = null
        var capturedError: Exception? = null

        Configuration.fetch(
            configurationLoader,
            authorization,
            TestScope(UnconfinedTestDispatcher(testScheduler)),
            ConfigurationCallback { configuration, error ->
                capturedConfiguration = configuration
                capturedError = error
            }
        )

        assertEquals(expectedConfiguration, capturedConfiguration)
        assertNull(capturedError)
    }

    @Test
    fun `fetch on failure calls back with error`() = runTest {
        val expectedError = ConfigurationException("configuration fetch failed")
        coEvery {
            configurationLoader.loadConfiguration(authorization)
        } returns ConfigurationLoaderResult.Failure(expectedError)

        var capturedConfiguration: Configuration? = null
        var capturedError: Exception? = null

        Configuration.fetch(
            configurationLoader,
            authorization,
            TestScope(UnconfinedTestDispatcher(testScheduler)),
            ConfigurationCallback { configuration, error ->
                capturedConfiguration = configuration
                capturedError = error
            }
        )

        assertNull(capturedConfiguration)
        assertEquals(expectedError, capturedError)
    }

    @Test
    fun `fetch on cancellation does not call back`() = runTest {
        coEvery {
            configurationLoader.loadConfiguration(authorization)
        } throws CancellationException()

        var callbackInvoked = false
        Configuration.fetch(
            configurationLoader,
            authorization,
            TestScope(UnconfinedTestDispatcher(testScheduler)),
            ConfigurationCallback { _, _ -> callbackInvoked = true }
        )

        assertFalse(callbackInvoked)
    }
}
