package com.braintreepayments.api.core

import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ConfigurationFetchUnitTest {

    private val braintreeClient: BraintreeClient = mockk(relaxed = true)

    @Test
    fun `fetch on success calls back with configuration`() = runTest {
        val expectedConfiguration: Configuration = mockk()
        coEvery { braintreeClient.getConfiguration() } returns expectedConfiguration

        val configurationSlot = slot<Configuration?>()
        val errorSlot = slot<Exception?>()

        Configuration.fetch(
            braintreeClient,
            TestScope(UnconfinedTestDispatcher(testScheduler)),
            ConfigurationCallback { configuration, error ->
                configurationSlot.captured = configuration
                errorSlot.captured = error
            }
        )

        assertEquals(expectedConfiguration, configurationSlot.captured)
        assertNull(errorSlot.captured)
    }

    @Test
    fun `fetch on failure calls back with error`() = runTest {
        val expectedError = IOException("configuration fetch failed")
        coEvery { braintreeClient.getConfiguration() } throws expectedError

        val configurationSlot = slot<Configuration?>()
        val errorSlot = slot<Exception?>()

        Configuration.fetch(
            braintreeClient,
            TestScope(UnconfinedTestDispatcher(testScheduler)),
            ConfigurationCallback { configuration, error ->
                configurationSlot.captured = configuration
                errorSlot.captured = error
            }
        )

        assertNull(configurationSlot.captured)
        assertEquals(expectedError, errorSlot.captured)
    }

    @Test
    fun `fetch on cancellation does not call back`() = runTest {
        coEvery { braintreeClient.getConfiguration() } throws CancellationException()

        var callbackInvoked = false
        Configuration.fetch(
            braintreeClient,
            TestScope(UnconfinedTestDispatcher(testScheduler)),
            ConfigurationCallback { _, _ -> callbackInvoked = true }
        )

        assertTrue(!callbackInvoked)
    }
}
