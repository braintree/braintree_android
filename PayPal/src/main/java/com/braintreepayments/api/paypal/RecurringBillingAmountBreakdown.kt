package com.braintreepayments.api.paypal

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

/**
 * A recurring billing amount breakdown.
 *
 * @property shipping - Cost of shipping the items
 * @property taxTotal - Total tax amount applied to the transaction
 * @property itemTotal - Total amount of the items before any taxes or discounts
 */
@Parcelize
data class RecurringBillingAmountBreakdown(
    val shipping: String,
    val taxTotal: String,
    val itemTotal: String,
) : Parcelable
