package com.braintreepayments.api.localpayment

import com.braintreepayments.api.BrowserSwitchOptions

/**
 * Local payment result information.
 *
 * @property request used to create the local payment transaction.
 * @property approvalUrl used for payment approval.
 * @property paymentId of the local payment after creation.
 */
data class LocalPaymentAuthRequestParams @JvmOverloads internal constructor(
    val request: LocalPaymentRequest,
    val approvalUrl: String,
    val paymentId: String,
    var browserSwitchOptions: BrowserSwitchOptions? = null
)
