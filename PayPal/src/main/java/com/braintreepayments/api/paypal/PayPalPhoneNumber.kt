package com.braintreepayments.api.paypal

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.json.JSONObject

/**
 * Representation of a user phone number.
 *
 * @property countryCode The international country code for the shopper's phone number
 * (must be 1 to 3 digits, no symbols or spaces allowed; e.g., "1" for the United States).
 *
 * @property nationalNumber The national segment of the shopper's phone number
 * (must be 4 to 12 digits, no symbols or spaces allowed; excludes the country code).
 */
@Parcelize
data class PayPalPhoneNumber(
    var countryCode: String,
    var nationalNumber: String,
) : Parcelable {

    internal fun toJson(): JSONObject {
        return JSONObject().apply {
            put(COUNTRY_CODE_KEY, countryCode)
            put(NATIONAL_NUMBER_KEY, nationalNumber)
        }
    }

    companion object {
        private const val COUNTRY_CODE_KEY: String = "country_code"
        private const val NATIONAL_NUMBER_KEY: String = "national_number"
    }
}
