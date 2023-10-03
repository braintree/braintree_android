package com.braintreepayments.api

import androidx.annotation.RestrictTo
import org.json.JSONObject

/**
 * @suppress
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object PostalAddressParser {
    const val RECIPIENT_NAME_KEY = "recipientName"
    const val STREET_ADDRESS_KEY = "street1"
    const val EXTENDED_ADDRESS_KEY = "street2"
    const val LOCALITY_KEY = "city"
    const val COUNTRY_CODE_ALPHA_2_KEY = "country"
    const val POSTAL_CODE_KEY = "postalCode"
    const val REGION_KEY = "state"
    const val LINE_1_KEY = "line1"
    const val LINE_2_KEY = "line2"
    const val COUNTRY_CODE_KEY = "countryCode"
    const val USER_ADDRESS_NAME_KEY = "name"
    const val USER_ADDRESS_PHONE_NUMBER_KEY = "phoneNumber"
    const val USER_ADDRESS_ADDRESS_1_KEY = "address1"
    const val USER_ADDRESS_ADDRESS_2_KEY = "address2"
    const val USER_ADDRESS_ADDRESS_3_KEY = "address3"
    const val USER_ADDRESS_ADDRESS_4_KEY = "address4"
    const val USER_ADDRESS_ADDRESS_5_KEY = "address5"
    const val USER_ADDRESS_POSTAL_CODE_KEY = "postalCode"
    const val USER_ADDRESS_SORTING_CODE_KEY = "sortingCode"
    const val USER_ADDRESS_COUNTRY_CODE_KEY = "countryCode"
    const val USER_ADDRESS_LOCALITY_KEY = "locality"
    const val USER_ADDRESS_ADMINISTRATIVE_AREA_KEY = "administrativeArea"
    const val COUNTRY_CODE_UNDERSCORE_KEY = "country_code"
    const val POSTAL_CODE_UNDERSCORE_KEY = "postal_code"
    const val RECIPIENT_NAME_UNDERSCORE_KEY = "recipient_name"
    const val VENMO_GQL_RECIPIENT_KEY = "fullName"
    const val VENMO_GQL_ADDRESS1_KEY = "addressLine1"
    const val VENMO_GQL_ADDRESS2_KEY = "addressLine2"
    const val VENMO_GQL_LOCALITY_KEY = "adminArea2"
    const val VENMO_GQL_REGION_KEY = "adminArea1"

    @JvmStatic
    fun fromJson(accountAddress: JSONObject?): PostalAddress =
        // If we don't have an account address, return an empty PostalAddress.
        accountAddress?.let {
            var streetAddress = Json.optString(accountAddress, STREET_ADDRESS_KEY, null)
            var extendedAddress = Json.optString(accountAddress, EXTENDED_ADDRESS_KEY, null)
            var countryCodeAlpha2 = Json.optString(accountAddress, COUNTRY_CODE_ALPHA_2_KEY, null)

            // Check alternate keys
            streetAddress = streetAddress ?: Json.optString(accountAddress, LINE_1_KEY, null)
            extendedAddress = extendedAddress ?: Json.optString(accountAddress, LINE_2_KEY, null)
            countryCodeAlpha2 = countryCodeAlpha2 ?: Json.optString(accountAddress, COUNTRY_CODE_KEY, null)

            streetAddress = streetAddress ?: Json.optString(accountAddress, VENMO_GQL_ADDRESS1_KEY, null)
            extendedAddress = extendedAddress ?: Json.optString(accountAddress, VENMO_GQL_ADDRESS2_KEY, null)
            // If this is a UserAddress-like JSON, parse it as such
            if (streetAddress == null && Json.optString(
                    accountAddress,
                    USER_ADDRESS_NAME_KEY,
                    null
                ) != null
            ) {
                return@let fromUserAddressJson(accountAddress)
            }

            PostalAddress().apply {
                recipientName = Json.optString(accountAddress, RECIPIENT_NAME_KEY, null)
                this.streetAddress = streetAddress
                this.extendedAddress = extendedAddress
                locality = Json.optString(accountAddress, LOCALITY_KEY, null)
                region = Json.optString(accountAddress, REGION_KEY, null)
                postalCode = Json.optString(accountAddress, POSTAL_CODE_KEY, null)
                this.countryCodeAlpha2 = countryCodeAlpha2

                recipientName = recipientName ?: Json.optString(accountAddress, VENMO_GQL_RECIPIENT_KEY, null)
                locality = locality ?: Json.optString(accountAddress, VENMO_GQL_LOCALITY_KEY, null)
                region = region ?: Json.optString(accountAddress, VENMO_GQL_REGION_KEY, null)
            }
        } ?: PostalAddress()

    fun fromUserAddressJson(json: JSONObject): PostalAddress =
        PostalAddress().apply {
            recipientName = Json.optString(json, USER_ADDRESS_NAME_KEY, "")
            phoneNumber = Json.optString(json, USER_ADDRESS_PHONE_NUMBER_KEY, "")
            streetAddress = Json.optString(json, USER_ADDRESS_ADDRESS_1_KEY, "")
            extendedAddress = formatExtendedUserAddress(json)
            locality = Json.optString(json, USER_ADDRESS_LOCALITY_KEY, "")
            region = Json.optString(json, USER_ADDRESS_ADMINISTRATIVE_AREA_KEY, "")
            countryCodeAlpha2 = Json.optString(json, USER_ADDRESS_COUNTRY_CODE_KEY, "")
            postalCode = Json.optString(json, USER_ADDRESS_POSTAL_CODE_KEY, "")
            sortingCode = Json.optString(json, USER_ADDRESS_SORTING_CODE_KEY, "")
        }

    private fun formatExtendedUserAddress(address: JSONObject): String =
        (Json.optString(address, USER_ADDRESS_ADDRESS_2_KEY, "") + "\n" +
        Json.optString(address, USER_ADDRESS_ADDRESS_3_KEY, "") + "\n" +
        Json.optString(address, USER_ADDRESS_ADDRESS_4_KEY, "") + "\n" +
        Json.optString(address, USER_ADDRESS_ADDRESS_5_KEY, "")).trim()
}
