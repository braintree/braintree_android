package com.braintreepayments.api.paypal

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

internal class AppForegroundDetector(
    private val store: PendingPaymentStore
) : DefaultLifecycleObserver {

    override fun onStop(owner: LifecycleOwner) {
        if (store.state == PendingPaymentStore.State.SWITCH_LAUNCHED) {
            store.state = PendingPaymentStore.State.AWAITING_RETURN
        }
    }

    override fun onStart(owner: LifecycleOwner) {
        if (store.state == PendingPaymentStore.State.AWAITING_RETURN) {
            store.onReturnFromAppSwitch?.let { callback ->
                CoroutineScope(Dispatchers.Main).launch { callback() }
            }
        }
    }
}
