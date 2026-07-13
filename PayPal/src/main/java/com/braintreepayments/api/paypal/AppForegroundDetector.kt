package com.braintreepayments.api.paypal

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Observes process-level lifecycle events via [ProcessLifecycleOwner] to detect when the
 * merchant app returns to the foreground after a PayPal app switch. This is the
 * merchant-independent trigger for the auto-link on manual return flow — it fires regardless
 * of whether the merchant wires `handleReturnToApp`, covering `onNewIntent`/`singleTop`
 * integrations that receive no new intent on a manual return.
 *
 * Registered once in [PendingPaymentStore.instance] initialization — not per [PayPalClient]
 * instance — to prevent observer accumulation.
 *
 * State transitions:
 * - [onStop]: if state is [SWITCH_LAUNCHED][PendingPaymentStore.State.SWITCH_LAUNCHED],
 *   transitions to [AWAITING_RETURN][PendingPaymentStore.State.AWAITING_RETURN].
 * - [onStart]: if state is [AWAITING_RETURN][PendingPaymentStore.State.AWAITING_RETURN],
 *   posts [PendingPaymentStore.onReturnFromAppSwitch] to the main-queue tail.
 *
 * ### Timer-free race resolution
 * A real URL return arriving in the same foreground pass calls `handleReturnToApp`, whose
 * `Success` clears the store (state → [IDLE][PendingPaymentStore.State.IDLE]). By posting the
 * auto-link attempt to the tail of the main queue and re-checking the state inside the
 * coroutine, a synchronous `handleReturnToApp(Success)` wins the race without any suppression
 * timer — the posted attempt observes [IDLE] and no-ops.
 *
 * @param store the process-level [PendingPaymentStore] singleton
 * @param scope coroutine scope used to post the deferred foreground trigger; injectable for tests
 */
internal class AppForegroundDetector(
    private val store: PendingPaymentStore,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) : DefaultLifecycleObserver {

    override fun onStop(owner: LifecycleOwner) {
        if (store.state == PendingPaymentStore.State.SWITCH_LAUNCHED) {
            store.state = PendingPaymentStore.State.AWAITING_RETURN
        }
    }

    override fun onStart(owner: LifecycleOwner) {
        if (store.state != PendingPaymentStore.State.AWAITING_RETURN) return
        val callback = store.onReturnFromAppSwitch ?: return
        scope.launch {
            // Re-check after the queue-tail hop: a URL return's Success may have cleared the store.
            if (store.state == PendingPaymentStore.State.AWAITING_RETURN) {
                callback()
            }
        }
    }
}
