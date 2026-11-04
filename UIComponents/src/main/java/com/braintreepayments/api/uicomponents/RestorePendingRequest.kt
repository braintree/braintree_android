package com.braintreepayments.api.uicomponents

import com.braintreepayments.api.BrowserSwitchException
import com.braintreepayments.api.paypal.PayPalLauncher

/**
 * Gives [payPalLauncher] the pending request that was stored when the flow started. The launcher only keeps it in
 * memory, so a launcher created after the Activity was recreated while the browser was open needs it before the
 * result can be completed.
 *
 * @return the error if the stored request could not be restored, `null` otherwise.
 */
internal fun restorePendingRequest(
    payPalLauncher: PayPalLauncher,
    pendingRequestString: String
): BrowserSwitchException? {
    if (pendingRequestString.isEmpty()) return null
    return try {
        payPalLauncher.restorePendingRequest(pendingRequestString)
        null
    } catch (e: BrowserSwitchException) {
        e
    }
}
