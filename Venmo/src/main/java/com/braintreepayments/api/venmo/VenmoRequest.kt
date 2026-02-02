package com.braintreepayments.api.venmo

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * A VenmoRequest specifies options that contribute to the Venmo flow.
 *
 * @property paymentMethodUsage - [VenmoPaymentMethodUsage] for the tokenized Venmo account: either
 * multi-use or single use.
 *
 * @property riskCorrelationId - Optional - A unique identifier that is used to correlate risk information for this payment.
 *
 * @property lineItems - Optional - The line items for this transaction. Can include up to 249 line
 * items. If this value is set, `totalAmount` must also be set.
 *
 * @property shouldVault Optional - Whether or not to automatically vault the Venmo Account.
 * Whether or not to automatically vault the Venmo Account.
 * Vaulting will only occur if a client token with a customer ID is being used.
 * Defaults to false.
 *
 * Also when shouldVault is true, [VenmoPaymentMethodUsage] on the [VenmoRequest] must be set to
 * [VenmoPaymentMethodUsage.MULTI_USE].
 *
 * @property profileId Optional - The Venmo profile ID to be used during payment authorization.
 * The Venmo profile ID to be used during payment authorization.
 * Customers will see the business name and logo associated with this Venmo
 * profile, and it will show up in the Venmo app as a "Connected Merchant".
 * Venmo profile IDs can be found in the Braintree Control Panel. Leaving this
 * null` will use the default Venmo profile.
 *
 * @property displayName Optional - The business name that will be displayed in the Venmo app
 * payment approval screen. Only used by merchants onboarded as PayFast channel partners.
 *
 *
 * @property collectCustomerShippingAddress Optional - The boolean value of the flag that
 * signifies whether customer shipping address will be collected. Whether or not shipping address
 * should be collected and displayed on Venmo pay sheet.
 *
 * @property collectCustomerBillingAddress Optional - The boolean value of the flag that
 * signifies whether customer billing address will be collected. Whether or not billing address
 * should be collected and displayed on Venmo pay sheet.
 *
 * @property totalAmount Optional - The grand total amount of the transaction that will be
 * displayed on the pay sheet. The grand total amount of the transaction that will be displayed
 * on the pay sheet.
 *
 * @property subTotalAmount Optional - The subtotal amount of the transaction, excluding taxes,
 * discounts, and shipping. The subtotal amount of the transaction, excluding taxes, discounts,
 * and shipping.
 *
 * @property discountAmount Optional - The total discount amount applied on the transaction. The
 * total discount amount applied on the transaction. If this value is set, `totalAmount` must
 * also be set.
 *
 * @property taxAmount Optional - The total tax amount applied to the transaction. The total tax
 * amount applied to the transaction. If this value is set, `totalAmount` must also be set.
 *
 * @property shippingAmount Optional - he shipping amount charged for the transaction. The
 * shipping amount charged for the transaction. If this value is set, `totalAmount` must also be
 * set.
 *
 * @property isFinalAmount Optional - The boolean value of the flag that signifies whether the purchase
 * amount is the final amount. Indicates whether the purchase amount is the final amount.
 * Defaults to false.
 */
@Parcelize
class VenmoRequest @JvmOverloads constructor(
    val paymentMethodUsage: VenmoPaymentMethodUsage,
    val riskCorrelationId: String? = null,
    var lineItems: ArrayList<VenmoLineItem>? = null,
    var shouldVault: Boolean = false,
    var profileId: String? = null,
    var displayName: String? = null,
    var collectCustomerShippingAddress: Boolean = false,
    var collectCustomerBillingAddress: Boolean = false,
    var totalAmount: String? = null,
    var subTotalAmount: String? = null,
    var discountAmount: String? = null,
    var taxAmount: String? = null,
    var shippingAmount: String? = null,
    var isFinalAmount: Boolean = false
) : Parcelable
