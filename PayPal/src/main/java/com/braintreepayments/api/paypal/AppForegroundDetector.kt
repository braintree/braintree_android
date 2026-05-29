package com.braintreepayments.api.paypal

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Observes process-level lifecycle events via [ProcessLifecycleOwner] to detect when the
 * merchant app returns to the foreground after a PayPal app switch.
 *
 * Registered once in [PendingPaymentStore.instance] initialization — not per [PayPalClient]
 * instance — to prevent observer accumulation.
 *
 * State transitions:
 * - [onStop]: if state is [SWITCH_LAUNCHED][PendingPaymentStore.State.SWITCH_LAUNCHED],
 *   transitions to [AWAITING_RETURN][PendingPaymentStore.State.AWAITING_RETURN]
 * - [onStart]: if state is [AWAITING_RETURN][PendingPaymentStore.State.AWAITING_RETURN],
 *   invokes [PendingPaymentStore.onReturnFromAppSwitch] to attempt auto-link tokenization
 *
 * @param store the process-level [PendingPaymentStore] singleton
 */
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
