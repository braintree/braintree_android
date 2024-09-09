package com.braintreepayments.api.americanexpress

import android.os.Parcelable
import androidx.annotation.RestrictTo
import com.braintreepayments.api.sharedutils.Json
import kotlinx.parcelize.Parcelize
import org.json.JSONException
import org.json.JSONObject

/**
 * Class to parse and contain American Express rewards balance
 *
 * @property errorCode - An error code when there was an issue fetching the rewards balance
 * @property errorMessage - An error message when there was an issue fetching the rewards balance
 * @property conversionRate - The conversion rate associated with the rewards balance
 * @property currencyAmount - The currency amount associated with the rewards balance
 * @property currencyIsoCode - The currency ISO code associated with the rewards balance
 * @property requestId - The request ID used when fetching the rewards balance
 * @property rewardsAmount - The rewards amount associated with the rewards balance
 * @property rewardsUnit - The rewards unit associated with the rewards balance
 */
@Parcelize
data class AmericanExpressRewardsBalance(
    var errorCode: String? = null,
    var errorMessage: String? = null,
    var conversionRate: String? = null,
    var currencyAmount: String? = null,
    var currencyIsoCode: String? = null,
    var requestId: String? = null,
    var rewardsAmount: String? = null,
    var rewardsUnit: String? = null
) : Parcelable {

    companion object {
        private const val ERROR_KEY = "error"
        private const val ERROR_CODE_KEY = "code"
        private const val ERROR_MESSAGE_KEY = "message"
        private const val CONVERSION_RATE_KEY = "conversionRate"
        private const val CURRENCY_AMOUNT_KEY = "currencyAmount"
        private const val CURRENCY_ISO_CODE_KEY = "currencyIsoCode"
        private const val REQUEST_ID_KEY = "requestId"
        private const val REWARDS_AMOUNT_KEY = "rewardsAmount"
        private const val REWARDS_UNIT_KEY = "rewardsUnit"

        /**
         * Used to parse a response from the Braintree Gateway to be used for American Express rewards balance.
         *
         * @param jsonString The json response from the Braintree Gateway American Express rewards balance route.
         * @return The [AmericanExpressRewardsBalance] with rewards balance data.
         * @throws JSONException when parsing fails.
         */
        @JvmStatic
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @Throws(JSONException::class)
        fun fromJson(jsonString: String): AmericanExpressRewardsBalance {
            val json = JSONObject(jsonString)

            val rewardsBalance = AmericanExpressRewardsBalance()

            if (json.has(ERROR_KEY)) {
                val errorJson = json.getJSONObject(ERROR_KEY)
                rewardsBalance.errorMessage = errorJson.getString(ERROR_MESSAGE_KEY)
                rewardsBalance.errorCode = errorJson.getString(ERROR_CODE_KEY)
            }

            rewardsBalance.conversionRate = Json.optString(json, CONVERSION_RATE_KEY, null)
            rewardsBalance.currencyAmount = Json.optString(json, CURRENCY_AMOUNT_KEY, null)
            rewardsBalance.currencyIsoCode = Json.optString(json, CURRENCY_ISO_CODE_KEY, null)
            rewardsBalance.requestId = Json.optString(json, REQUEST_ID_KEY, null)
            rewardsBalance.rewardsAmount = Json.optString(json, REWARDS_AMOUNT_KEY, null)
            rewardsBalance.rewardsUnit = Json.optString(json, REWARDS_UNIT_KEY, null)

            return rewardsBalance
        }
    }
}
