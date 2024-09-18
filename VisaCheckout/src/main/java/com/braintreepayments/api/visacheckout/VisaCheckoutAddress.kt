package com.braintreepayments.api.visacheckout

import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.RestrictTo
import com.braintreepayments.api.sharedutils.Json
import kotlinx.parcelize.Parcelize
import org.json.JSONObject

/**
 * A class containing Visa Checkout information about the user's address.
 *
 * @property firstName The user's first name.
 * @property lastName The user's last name.
 * @property streetAddress The user's street address.
 * @property extendedAddress The user's extended address.
 * @property locality The user's locality.
 * @property region The user's region.
 * @property postalCode The user's postal code.
 * @property countryCode The user's country code.
 * @property phoneNumber The user's phone number.
 */
@Parcelize
data class VisaCheckoutAddress internal constructor(
    val firstName: String?,
    val lastName: String?,
    val streetAddress: String?,
    val extendedAddress: String?,
    val locality: String?,
    val region: String?,
    val postalCode: String?,
    val countryCode: String?,
    val phoneNumber: String?,
) : Parcelable {
    companion object {
        
        @JvmStatic
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        fun fromJson(jsonIn: JSONObject?): VisaCheckoutAddress {
            val json = jsonIn ?: JSONObject()
            return VisaCheckoutAddress(
                firstName = Json.optString(json, "firstName", ""),
                lastName = Json.optString(json, "lastName", ""),
                streetAddress = Json.optString(json, "streetAddress", ""),
                extendedAddress = Json.optString(json, "extendedAddress", ""),
                locality = Json.optString(json, "locality", ""),
                region = Json.optString(json, "region", ""),
                postalCode = Json.optString(json, "postalCode", ""),
                countryCode = Json.optString(json, "countryCode", ""),
                phoneNumber = Json.optString(json, "phoneNumber", ""),
            )
        }
    }
}
