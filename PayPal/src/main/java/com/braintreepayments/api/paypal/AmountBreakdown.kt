package com.braintreepayments.api.paypal

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

/**
 * A recurring billing amount breakdown.
 *
 * This object is only used in `PayPalCheckoutRequest` to customize how the transaction amount is
 * broken down. If provided, `itemTotal` becomes required. Some fields are conditionally required or
 * not accepted based depending on the checkout flow (e.g., one-time vs subscription).
 *
 * @property itemTotal - Total amount of the items before any taxes or discounts.
 *                       Required when `amountBreakdown` is provided.
 *
 * @property taxTotal - (optional) Total tax amount applied to the transaction.
 *                      Required only if both `lineItems.taxAmount` and `amountBreakdown` are
 *                      provided. Should match the sum of tax amounts from all line items.
 *
 * @property shippingTotal - (optional) Cost of shipping the items.
 *                            Optional in all flows.
 *
 * @property handling - (optional) Cost associated with handling the items
 *                      (e.g., packaging or processing).
 *                      Not accepted in one-time checkout flows; allowed in other flows like
 *                      subscriptions.
 *
 * @property insurance - (optional) Cost of insurance applied to the shipment or items.
 *                       Not accepted in one-time checkout flows; allowed in other flows like
 *                       subscriptions.
 *
 * @property shippingDiscount - (optional) Discount amount applied specifically to shipping.
 *                               Not accepted in one-time checkout flows; allowed in other flows.
 *
 * @property discount - (optional) General discount applied to the total transaction.
 *                      Not accepted in one-time checkout flows; allowed in other flows.
 */


@Parcelize
data class AmountBreakdown(
    val itemTotal: String,
    val taxTotal: String?,
    val shippingTotal: String?,
    val handling: String?,
    val insurance: String?,
    val shippingDiscount: String?,
    val discount: String?
) : Parcelable
