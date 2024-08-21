package com.braintreepayments.api.localpayment

import com.braintreepayments.api.BrowserSwitchOptions

/**
 * Local payment result information.
 */
class LocalPaymentAuthRequestParams internal constructor(
    /**
     * @return The original request used to create the local payment transaction.
     */
    val request: LocalPaymentRequest,
    /**
     * @return The URL used for payment approval.
     */
    val approvalUrl: String,
    /**
     * @return The ID of the local payment after creation.
     */
    val paymentId: String
) {

    var browserSwitchOptions: BrowserSwitchOptions? = null
}
