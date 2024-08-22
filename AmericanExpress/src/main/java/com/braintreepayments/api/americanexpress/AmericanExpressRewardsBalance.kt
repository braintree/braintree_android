package com.braintreepayments.api.americanexpress

import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.RestrictTo
import com.braintreepayments.api.sharedutils.Json
import org.json.JSONException
import org.json.JSONObject

/**
 * Class to parse and contain American Express rewards balance
 */
class AmericanExpressRewardsBalance : Parcelable {
    /**
     * @return An error code when there was an issue fetching the rewards balance
     */
    var errorCode: String? = null
        private set

    /**
     * @return An error message when there was an issue fetching the rewards balance
     */
    var errorMessage: String? = null
        private set

    /**
     * @return The conversion rate associated with the rewards balance
     */
    var conversionRate: String? = null
        private set

    /**
     * @return The currency amount associated with the rewards balance
     */
    var currencyAmount: String? = null
        private set

    /**
     * @return The currency ISO code associated with the rewards balance
     */
    var currencyIsoCode: String? = null
        private set

    /**
     * @return The request ID used when fetching the rewards balance
     */
    var requestId: String? = null
        private set

    /**
     * @return The rewards amount associated with the rewards balance
     */
    var rewardsAmount: String? = null
        private set

    /**
     * @return The rewards unit associated with the rewards balance
     */
    var rewardsUnit: String? = null
        private set

    private constructor()

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(errorCode)
        dest.writeString(errorMessage)
        dest.writeString(conversionRate)
        dest.writeString(currencyAmount)
        dest.writeString(currencyIsoCode)
        dest.writeString(requestId)
        dest.writeString(rewardsAmount)
        dest.writeString(rewardsUnit)
    }

    private constructor(`in`: Parcel) {
        errorCode = `in`.readString()
        errorMessage = `in`.readString()
        conversionRate = `in`.readString()
        currencyAmount = `in`.readString()
        currencyIsoCode = `in`.readString()
        requestId = `in`.readString()
        rewardsAmount = `in`.readString()
        rewardsUnit = `in`.readString()
    }

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
        fun fromJson(jsonString: String?): AmericanExpressRewardsBalance {
            val json = JSONObject(jsonString!!)

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

        @JvmField
        val CREATOR: Parcelable.Creator<AmericanExpressRewardsBalance?> =
            object : Parcelable.Creator<AmericanExpressRewardsBalance?> {
                override fun createFromParcel(source: Parcel): AmericanExpressRewardsBalance {
                    return AmericanExpressRewardsBalance(source)
                }

                override fun newArray(size: Int): Array<AmericanExpressRewardsBalance?> {
                    return arrayOfNulls(size)
                }
            }
    }
}
