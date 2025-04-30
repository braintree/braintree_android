package com.braintreepayments.api.paypal

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

/**
 * A recurring billing amount breakdown.
 *
 * @property itemTotal - Total amount of the items before any taxes or discounts
 * @property insurance - (optional) Cost of insurance applied to the shipment or items
 * @property discount - (optional) General discount applied to the total transaction
 * @property handling - (optional) Cost associated with handling the items (e.g., packaging or processing)
 * @property shippingDiscount - (optional) Discount amount applied specifically to shipping
 * @property shippingTotal - (optional) Cost of shipping the items
 * @property taxTotal - (optional) Total tax amount applied to the transaction
 */
@Parcelize
data class RecurringBillingAmountBreakdown(
    val itemTotal: String,
    val insurance: String?,
    val discount: String?,
    val handling: String?,
    val shippingDiscount: String?,
    val shippingTotal: String?,
    val taxTotal: String?,
) : Parcelable
