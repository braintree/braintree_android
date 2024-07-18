package com.braintreepayments.api.core

import androidx.annotation.IntDef

/**
 * Request codes that Braintree uses when communicating between Android Activities and Fragments.
 */
@Retention(AnnotationRetention.SOURCE)
@IntDef(
    BraintreeRequestCodes.THREE_D_SECURE,
    BraintreeRequestCodes.VENMO,
    BraintreeRequestCodes.PAYPAL,
    BraintreeRequestCodes.GOOGLE_PAY,
    BraintreeRequestCodes.LOCAL_PAYMENT,
    BraintreeRequestCodes.SEPA_DEBIT
)
annotation class BraintreeRequestCodes {
    companion object {
        const val THREE_D_SECURE = 13487
        const val VENMO = 13488
        const val PAYPAL = 13591
        const val GOOGLE_PAY = 13593
        const val LOCAL_PAYMENT = 13596
        const val SEPA_DEBIT = 13597
    }
}
