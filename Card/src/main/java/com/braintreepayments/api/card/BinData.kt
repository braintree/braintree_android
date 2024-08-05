package com.braintreepayments.api.card

import android.os.Parcelable
import androidx.annotation.RestrictTo
import androidx.annotation.StringDef
import com.braintreepayments.api.sharedutils.Json
import kotlinx.parcelize.Parcelize
import org.json.JSONObject

/**
 * A class to contain BIN data for the card number
 */
@Parcelize
data class BinData(
    private var prepaid: String = UNKNOWN,
    private var healthcare: String = UNKNOWN,
    private var debit: String = UNKNOWN,
    private var durbinRegulated: String = UNKNOWN,
    private var commercial: String = UNKNOWN,
    private var payroll: String = UNKNOWN,
    private var issuingBank: String = UNKNOWN,
    private var countryOfIssuance: String = UNKNOWN,
    private var productId: String = UNKNOWN
) : Parcelable {

    @Retention(AnnotationRetention.SOURCE)
    @StringDef(YES, NO, UNKNOWN)
    internal annotation class BinType

    /**
     * @return Whether the card is a prepaid card. Possible [BinType] values are [BinData.YES], [BinData.NO] or [BinData.UNKNOWN]
     */
    @BinType
    fun getPrepaid(): String {
        return prepaid
    }

    /**
     * @return Whether the card is a healthcare card. Possible [BinType] values are [BinData.YES], [BinData.NO] or [BinData.UNKNOWN]
     */
    @BinType
    fun getHealthcare(): String {
        return healthcare
    }

    /**
     * @return Whether the card is a debit card. Possible [BinType] values are [BinData.YES], [BinData.NO] or [BinData.UNKNOWN]
     */
    @BinType
    fun getDebit(): String {
        return debit
    }

    /**
     * @return A value indicating whether the issuing bank's card range is regulated by the Durbin Amendment due to the bank's assets. Possible [BinType] values are [BinData.YES], [BinData.NO] or [BinData.UNKNOWN]
     */
    @BinType
    fun getDurbinRegulated(): String {
        return durbinRegulated
    }

    /**
     * @return Whether the card type is a commercial card and is capable of processing Level 2 transactions. Possible [BinType] values are [BinData.YES], [BinData.NO] or [BinData.UNKNOWN]
     */
    @BinType
    fun getCommercial(): String {
        return commercial
    }

    /**
     * @return Whether the card is a payroll card. Possible [BinType] values are [BinData.YES], [BinData.NO] or [BinData.UNKNOWN]
     */
    @BinType
    fun getPayroll(): String {
        return payroll
    }

    /**
     * @return The bank that issued the credit card.
     */
    fun getIssuingBank(): String {
        return issuingBank
    }

    /**
     * @return The country that issued the credit card.
     */
    fun getCountryOfIssuance(): String {
        return countryOfIssuance
    }

    /**
     * @return The code for the product type of the card (e.g. `D` (Visa Signature Preferred), `G` (Visa Business)).
     */
    fun getProductId(): String {
        return productId
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object {
        const val BIN_DATA_KEY: String = "binData"

        const val YES: String = "Yes"
        const val NO: String = "No"
        const val UNKNOWN: String = "Unknown"

        private const val PREPAID_KEY = "prepaid"
        private const val HEALTHCARE_KEY = "healthcare"
        private const val DEBIT_KEY = "debit"
        private const val DURBIN_REGULATED_KEY = "durbinRegulated"
        private const val COMMERCIAL_KEY = "commercial"
        private const val PAYROLL_KEY = "payroll"
        private const val ISSUING_BANK_KEY = "issuingBank"
        private const val COUNTRY_OF_ISSUANCE_KEY = "countryOfIssuance"
        private const val PRODUCT_ID_KEY = "productId"

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @JvmStatic
        fun fromJson(jsonObject: JSONObject?): BinData {

            val json = jsonObject ?: JSONObject()

            return BinData(
                prepaid = Json.optString(json, PREPAID_KEY, UNKNOWN),
                healthcare = Json.optString(json, HEALTHCARE_KEY, UNKNOWN),
                debit = Json.optString(json, DEBIT_KEY, UNKNOWN),
                durbinRegulated = Json.optString(json, DURBIN_REGULATED_KEY, UNKNOWN),
                commercial = Json.optString(json, COMMERCIAL_KEY, UNKNOWN),
                payroll = Json.optString(json, PAYROLL_KEY, UNKNOWN),
                issuingBank = convertNullToUnknown(json, ISSUING_BANK_KEY),
                countryOfIssuance = convertNullToUnknown(json, COUNTRY_OF_ISSUANCE_KEY),
                productId = convertNullToUnknown(json, PRODUCT_ID_KEY)
            )
        }

        private fun convertNullToUnknown(json: JSONObject, key: String): String {
            return if (json.has(key) && json.isNull(key)) {
                UNKNOWN
            } else {
                Json.optString(json, key, "")
            }
        }
    }
}
