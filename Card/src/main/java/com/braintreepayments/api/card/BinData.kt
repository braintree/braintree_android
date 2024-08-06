package com.braintreepayments.api.card

import android.os.Parcelable
import androidx.annotation.RestrictTo
import com.braintreepayments.api.sharedutils.Json
import kotlinx.parcelize.Parcelize
import org.json.JSONObject
import java.util.Locale

/**
 * A class to contain BIN data for the card number
 *
 * @property prepaid Whether the card is a prepaid card.
 * @property healthcare Whether the card is a healthcare card.
 * @property debit Whether the card is a debit card.
 * @property durbinRegulated A value indicating whether the issuing bank's card range is regulated
 * by the Durbin Amendment due to the bank's assets.
 * @property commercial Whether the card type is a commercial card and is capable of processing
 * Level 2 transactions.
 * @property payroll Whether the card is a payroll card.
 * @property issuingBank The bank that issued the credit card.
 * @property countryOfIssuance The country that issued the credit card.
 * @property productId The code for the product type of the card
 * (e.g. `D` (Visa Signature Preferred), `G` (Visa Business)).
 */
@Parcelize
data class BinData(
    val prepaid: BinType = BinType.Unknown,
    val healthcare: BinType = BinType.Unknown,
    val debit: BinType = BinType.Unknown,
    val durbinRegulated: BinType = BinType.Unknown,
    val commercial: BinType = BinType.Unknown,
    val payroll: BinType = BinType.Unknown,
    val issuingBank: String = BinType.Unknown.name,
    val countryOfIssuance: String = BinType.Unknown.name,
    val productId: String = BinType.Unknown.name
) : Parcelable {

    enum class BinType {
        Yes,
        No,
        Unknown
    }

    companion object {

        const val BIN_DATA_KEY: String = "binData"

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
                prepaid = BinType.valueOf(Json.optString(json, PREPAID_KEY, BinType.Unknown.name)),
                healthcare = BinType.valueOf(Json.optString(json, HEALTHCARE_KEY, BinType.Unknown.name)),
                debit = BinType.valueOf(Json.optString(json, DEBIT_KEY, BinType.Unknown.name)),
                durbinRegulated = BinType.valueOf(Json.optString(json, DURBIN_REGULATED_KEY, BinType.Unknown.name)),
                commercial = BinType.valueOf(Json.optString(json, COMMERCIAL_KEY, BinType.Unknown.name)),
                payroll = BinType.valueOf(Json.optString(json, PAYROLL_KEY, BinType.Unknown.name)),
                issuingBank = convertNullToUnknown(json, ISSUING_BANK_KEY),
                countryOfIssuance = convertNullToUnknown(json, COUNTRY_OF_ISSUANCE_KEY),
                productId = convertNullToUnknown(json, PRODUCT_ID_KEY)
            )
        }

        private fun convertNullToUnknown(json: JSONObject, key: String): String {
            return if (json.has(key) && json.isNull(key)) {
                BinType.Unknown.name
            } else {
                Json.optString(json, key, "")
            }
        }
    }
}
