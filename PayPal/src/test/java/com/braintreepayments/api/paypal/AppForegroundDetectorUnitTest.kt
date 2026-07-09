package com.braintreepayments.api.paypal

import androidx.lifecycle.LifecycleOwner
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppForegroundDetectorUnitTest {

    private val owner = mockk<LifecycleOwner>(relaxed = true)

    @Test
    fun `onStop transitions SWITCH_LAUNCHED to AWAITING_RETURN`() {
        val store = PendingPaymentStore().apply { state = PendingPaymentStore.State.SWITCH_LAUNCHED }
        val sut = AppForegroundDetector(store)

        sut.onStop(owner)

        assertEquals(PendingPaymentStore.State.AWAITING_RETURN, store.state)
    }

    @Test
    fun `onStop is a no-op when not SWITCH_LAUNCHED`() {
        val store = PendingPaymentStore().apply { state = PendingPaymentStore.State.IDLE }
        val sut = AppForegroundDetector(store)

        sut.onStop(owner)

        assertEquals(PendingPaymentStore.State.IDLE, store.state)
    }

    @Test
    fun `onStart invokes callback when AWAITING_RETURN`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        var invoked = false
        val store = PendingPaymentStore().apply {
            state = PendingPaymentStore.State.AWAITING_RETURN
            onReturnFromAppSwitch = { invoked = true }
        }
        val sut = AppForegroundDetector(store, CoroutineScope(dispatcher))

        sut.onStart(owner)
        advanceUntilIdle()

        assertTrue(invoked)
    }

    @Test
    fun `onStart does not invoke callback when state is not AWAITING_RETURN`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        var invoked = false
        val store = PendingPaymentStore().apply {
            state = PendingPaymentStore.State.SWITCH_LAUNCHED
            onReturnFromAppSwitch = { invoked = true }
        }
        val sut = AppForegroundDetector(store, CoroutineScope(dispatcher))

        sut.onStart(owner)
        advanceUntilIdle()

        assertFalse(invoked)
    }

    @Test
    fun `onStart no-ops when store cleared before queued attempt runs`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        var invoked = false
        val store = PendingPaymentStore().apply {
            state = PendingPaymentStore.State.AWAITING_RETURN
            onReturnFromAppSwitch = { invoked = true }
        }
        val sut = AppForegroundDetector(store, CoroutineScope(dispatcher))

        sut.onStart(owner)
        // Simulate a URL return's Success clearing the store before the queued attempt runs.
        store.clear()
        advanceUntilIdle()

        assertFalse(invoked)
    }
}
