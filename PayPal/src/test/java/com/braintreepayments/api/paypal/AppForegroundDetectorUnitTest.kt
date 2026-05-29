package com.braintreepayments.api.paypal

import androidx.lifecycle.LifecycleOwner
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import android.os.Looper

@RunWith(RobolectricTestRunner::class)
class AppForegroundDetectorUnitTest {

    private lateinit var store: PendingPaymentStore
    private lateinit var sut: AppForegroundDetector
    private val lifecycleOwner: LifecycleOwner = mockk(relaxed = true)

    @Before
    fun setup() {
        store = PendingPaymentStore()
        sut = AppForegroundDetector(store)
    }

    @Test
    fun `onStop transitions SWITCH_LAUNCHED to AWAITING_RETURN`() {
        store.state = PendingPaymentStore.State.SWITCH_LAUNCHED

        sut.onStop(lifecycleOwner)

        assertEquals(PendingPaymentStore.State.AWAITING_RETURN, store.state)
    }

    @Test
    fun `onStop does not transition IDLE state`() {
        store.state = PendingPaymentStore.State.IDLE

        sut.onStop(lifecycleOwner)

        assertEquals(PendingPaymentStore.State.IDLE, store.state)
    }

    @Test
    fun `onStop does not transition AWAITING_RETURN state`() {
        store.state = PendingPaymentStore.State.AWAITING_RETURN

        sut.onStop(lifecycleOwner)

        assertEquals(PendingPaymentStore.State.AWAITING_RETURN, store.state)
    }

    @Test
    fun `onStart invokes callback when state is AWAITING_RETURN`() {
        var callbackInvoked = false
        store.state = PendingPaymentStore.State.AWAITING_RETURN
        store.onReturnFromAppSwitch = { callbackInvoked = true }

        sut.onStart(lifecycleOwner)
        shadowOf(Looper.getMainLooper()).idle()

        assertEquals(true, callbackInvoked)
    }

    @Test
    fun `onStart does not invoke callback when state is IDLE`() {
        var callbackInvoked = false
        store.state = PendingPaymentStore.State.IDLE
        store.onReturnFromAppSwitch = { callbackInvoked = true }

        sut.onStart(lifecycleOwner)

        assertEquals(false, callbackInvoked)
    }

    @Test
    fun `onStart does not invoke callback when state is SWITCH_LAUNCHED`() {
        var callbackInvoked = false
        store.state = PendingPaymentStore.State.SWITCH_LAUNCHED
        store.onReturnFromAppSwitch = { callbackInvoked = true }

        sut.onStart(lifecycleOwner)

        assertEquals(false, callbackInvoked)
    }

    @Test
    fun `onStart is no-op when callback is null`() {
        store.state = PendingPaymentStore.State.AWAITING_RETURN
        store.onReturnFromAppSwitch = null

        sut.onStart(lifecycleOwner)

        assertEquals(PendingPaymentStore.State.AWAITING_RETURN, store.state)
    }
}
