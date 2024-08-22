package com.braintreepayments.api.threedsecure

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.json.JSONException
import org.json.JSONObject

/**
 * A class containing 3DS information about a postal address
 *
 * @property givenName Optional. Given name associated with the address.
 * @property surname Optional. Surname associated with the address.
 * @property streetAddress Optional. Line 1 of theAddress (eg. number, street, etc).
 * @property extendedAddress Optional. Line 2 of the Address (eg. suite, apt #, etc.).
 * @property line3 Optional. Line 3 of the Address (eg. suite, apt #, etc.).
 * @property locality Optional. City name.
 * @property region Optional. Either a two-letter state code (for the US), or an ISO-3166-2 country
 * subdivision code of up to three letters.
 * @property postalCode Optional. Zip code or equivalent is usually required for countries that have them.
 * For a list of countries that do not have postal codes please refer to
 * http://en.wikipedia.org/wiki/Postal_code
 * @property countryCodeAlpha2 Optional. 2 letter country code.
 * @property phoneNumber Optional. The phone number associated with the address. Only numbers. Remove dashes,
 * parentheses and other characters.
 */
@Parcelize
data class ThreeDSecurePostalAddress(
    var givenName: String? = null,
    var surname: String? = null,
    var streetAddress: String? = null,
    var extendedAddress: String? = null,
    var line3: String? = null,
    var locality: String? = null,
    var region: String? = null,
    var postalCode: String? = null,
    var countryCodeAlpha2: String? = null,
    var phoneNumber: String? = null
) : Parcelable {

    companion object {
        private const val FIRST_NAME_KEY = "firstName"
        private const val LAST_NAME_KEY = "lastName"
        private const val STREET_ADDRESS_KEY = "line1"
        private const val EXTENDED_ADDRESS_KEY = "line2"
        private const val LINE_3_KEY = "line3"
        private const val LOCALITY_KEY = "city"
        private const val REGION_KEY = "state"
        private const val POSTAL_CODE_KEY = "postalCode"
        private const val COUNTRY_CODE_ALPHA_2_KEY = "countryCode"
        private const val PHONE_NUMBER_KEY = "phoneNumber"
        private const val BILLING_ADDRESS_KEY = "billingAddress"
    }

    /**
     * @return JSONObject representation of [ThreeDSecurePostalAddress].
     */
    fun toJson(): JSONObject {
        val base = JSONObject()
        val billingAddress = JSONObject()
        try {
            base.putOpt(FIRST_NAME_KEY, givenName)
            base.putOpt(LAST_NAME_KEY, surname)
            base.putOpt(PHONE_NUMBER_KEY, phoneNumber)
            billingAddress.putOpt(STREET_ADDRESS_KEY, streetAddress)
            billingAddress.putOpt(EXTENDED_ADDRESS_KEY, extendedAddress)
            billingAddress.putOpt(LINE_3_KEY, line3)
            billingAddress.putOpt(LOCALITY_KEY, locality)
            billingAddress.putOpt(REGION_KEY, region)
            billingAddress.putOpt(POSTAL_CODE_KEY, postalCode)
            billingAddress.putOpt(COUNTRY_CODE_ALPHA_2_KEY, countryCodeAlpha2)
            if (billingAddress.length() != 0) {
                base.putOpt(BILLING_ADDRESS_KEY, billingAddress)
            }
        } catch (ignored: JSONException) {
        }
        return base
    }
}