import com.google.gson.annotations.SerializedName

/**
 * Represents the result from the Shopper Insight API.
 *
 * @property eligibleMethods Contains the payment methods available to the shopper.
 */
data class ShopperInsightApiResult(
    @SerializedName("eligible_methods")
    val eligibleMethods: PaymentMethods
)

/**
 * Contains details about the available payment methods.
 *
 * @property paypal Details about the PayPal payment method.
 * @property venmo Details about the Venmo payment method.
 */
data class PaymentMethods(
    @SerializedName("paypal")
    val paypal: PaymentMethodDetails,

    @SerializedName("venmo")
    val venmo: PaymentMethodDetails
)

/**
 * Details of a specific payment method.
 *
 * @property canBeVaulted Indicates if the payment method can be saved for future transactions.
 * @property eligibleInPaypalNetwork Indicates if the payment method is eligible in the PayPal network.
 * @property recommended Indicates if this payment method is recommended for the shopper.
 * @property recommendedPriority The priority ranking of this payment method if recommended; null if not applicable.
 */
data class PaymentMethodDetails(
    @SerializedName("can_be_vaulted")
    val canBeVaulted: Boolean?,

    @SerializedName("eligible_in_paypal_network")
    val eligibleInPaypalNetwork: Boolean?,

    @SerializedName("recommended")
    val recommended: Boolean?,

    @SerializedName("recommended_priority")
    val recommendedPriority: Int? = null
)
