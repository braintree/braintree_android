import com.braintreepayments.api.ShopperInsightsPaymentMethods
import org.json.JSONObject

/**
 * Represents the result from the Shopper Insight API.
 *
 * @property eligibleMethods Contains the payment methods available to the shopper.
 */
internal data class ShopperInsightApiResult(
    val eligibleMethods: ShopperInsightsPaymentMethods
) {
    companion object {
        fun fromJson(jsonString: String): ShopperInsightApiResult {
            val jsonObject = JSONObject(jsonString)
            val eligibleMethodsJson = jsonObject.getJSONObject("eligible_methods")
            return ShopperInsightApiResult(ShopperInsightsPaymentMethods.fromJson(eligibleMethodsJson))
        }
    }
}
