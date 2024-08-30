package com.braintreepayments.api.venmo

import androidx.annotation.IntDef
import com.braintreepayments.api.venmo.VenmoPaymentMethodUsage

/**
 * Usage type for the tokenized Venmo account.
 */
@Retention(AnnotationRetention.SOURCE)
@IntDef(*[VenmoPaymentMethodUsage.SINGLE_USE, VenmoPaymentMethodUsage.MULTI_USE])
annotation class VenmoPaymentMethodUsage {
    companion object {
        const val SINGLE_USE: Int = 1
        const val MULTI_USE: Int = 2
    }
}
