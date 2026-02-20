package com.braintreepayments.api.core

import androidx.annotation.RestrictTo

private const val THREE_D_SECURE_CODE = 13487
private const val VENMO_CODE = 13488
private const val PAYPAL_CODE = 13591
private const val VISA_CHECKOUT_CODE = 13592
private const val GOOGLE_PAY_CODE = 13593
private const val LOCAL_PAYMENT_CODE = 13596
private const val SEPA_DEBIT_CODE = 13597

/**
 * Request codes that Braintree uses when communicating between Android Activities and Fragments.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
enum class BraintreeRequestCodes(val code: Int) {
    THREE_D_SECURE(THREE_D_SECURE_CODE),
    VENMO(VENMO_CODE),
    PAYPAL(PAYPAL_CODE),
    VISA_CHECKOUT(VISA_CHECKOUT_CODE),
    GOOGLE_PAY(GOOGLE_PAY_CODE),
    LOCAL_PAYMENT(LOCAL_PAYMENT_CODE),
    SEPA_DEBIT(SEPA_DEBIT_CODE),
}
