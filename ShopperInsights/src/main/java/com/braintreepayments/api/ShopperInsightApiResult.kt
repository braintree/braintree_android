import org.json.JSONObject

/**
 * Represents the result from the Shopper Insight API.
 *
 * @property eligibleMethods Contains the payment methods available to the shopper.
 */
data class ShopperInsightApiResult(
    val eligibleMethods: PaymentMethods
) {
    companion object {
        fun fromJson(jsonString: String): ShopperInsightApiResult {
            val jsonObject = JSONObject(jsonString)
            val eligibleMethodsJson = jsonObject.getJSONObject("eligible_methods")
            return ShopperInsightApiResult(PaymentMethods.fromJson(eligibleMethodsJson))
        }
    }
}

/**
 * Contains details about the available payment methods.
 *
 * @property paypal Details about the PayPal payment method.
 * @property venmo Details about the Venmo payment method.
 */
data class PaymentMethods(
    val paypal: PaymentMethodDetails,
    val venmo: PaymentMethodDetails
) {
    companion object {
        fun fromJson(jsonObject: JSONObject): PaymentMethods {
            val paypal = PaymentMethodDetails.fromJson(jsonObject.getJSONObject("paypal"))
            val venmo = PaymentMethodDetails.fromJson(jsonObject.getJSONObject("venmo"))
            return PaymentMethods(paypal, venmo)
        }
    }
}

/**
 * Details of a specific payment method.
 *
 * @property canBeVaulted Indicates if the payment method can be saved for future transactions.
 * @property eligibleInPaypalNetwork Indicates if the payment method is eligible in the PayPal network.
 * @property recommended Indicates if this payment method is recommended for the shopper.
 * @property recommendedPriority The priority ranking of this payment method if recommended; null if not applicable.
 */
data class PaymentMethodDetails(
    val canBeVaulted: Boolean?,
    val eligibleInPaypalNetwork: Boolean?,
    val recommended: Boolean?,
    val recommendedPriority: Int? = null
) {
    companion object {
        fun fromJson(jsonObject: JSONObject): PaymentMethodDetails {
            return PaymentMethodDetails(
                canBeVaulted = jsonObject.optBoolean("can_be_vaulted"),
                eligibleInPaypalNetwork = jsonObject.optBoolean("eligible_in_paypal_network"),
                recommended = jsonObject.optBoolean("recommended"),
                recommendedPriority = jsonObject.optInt("recommended_priority")
            )
        }
    }
}
