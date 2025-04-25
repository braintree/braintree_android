package com.braintreepayments.api.paypal

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

/**
 * Class to parse and contain a recurring billing amount breakdown.
 *
 * @property itemTotal - Total amount of the items before any taxes or discounts
 * @property shipping - Cost of shipping the items
 * @property handling - Cost associated with handling the items (e.g., packaging or processing)
 * @property taxTotal - Total tax amount applied to the transaction
 * @property insurance - Cost of insurance applied to the shipment or items
 * @property shippingDiscount - Discount amount applied specifically to shipping
 * @property discount - General discount applied to the total transaction
 */
@Parcelize
data class RecurringBillingAmountBreakdown(
    val itemTotal: Double,
    val shipping: Double,
    val handling: Double,
    val taxTotal: Double,
    val insurance: Double,
    val shippingDiscount: Double,
    val discount: Double
) : Parcelable