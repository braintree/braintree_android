/**
 * Represents the result from the Shopper Insight API.
 *
 * @property eligible_methods Contains the payment methods available to the shopper.
 */
data class ShopperInsightApiResult (
    val eligible_methods: PaymentMethods
)

/**
 * Contains details about the available payment methods.
 *
 * @property paypal Details about the PayPal payment method.
 * @property venmo Details about the Venmo payment method.
 */
data class PaymentMethods(
    val paypal: PaymentMethodDetails,
    val venmo: PaymentMethodDetails
)

/**
 * Details of a specific payment method.
 *
 * @property can_be_vaulted Indicates if the payment method can be saved for future transactions.
 * @property eligible_in_paypal_network Indicates if the payment method is eligible in the PayPal network.
 * @property recommended Indicates if this payment method is recommended for the shopper.
 * @property recommended_priority The priority ranking of this payment method if recommended; null if not applicable.
 */
data class PaymentMethodDetails(
    val can_be_vaulted: Boolean?,
    val eligible_in_paypal_network: Boolean?,
    val recommended: Boolean?,
    val recommended_priority: Int? = null
)
