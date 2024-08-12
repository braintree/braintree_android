package com.braintreepayments.api.core

import androidx.annotation.RestrictTo

/**
 * Request codes that Braintree uses when communicating between Android Activities and Fragments.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Suppress("MagicNumber")
enum class BraintreeRequestCodes(val code: Int) {
    THREE_D_SECURE(13487),
    VENMO(13488),
    PAYPAL(13591),
    VISA_CHECKOUT(13592),
    GOOGLE_PAY(13593),
    LOCAL_PAYMENT(13596),
    SEPA_DEBIT(13597),
}
