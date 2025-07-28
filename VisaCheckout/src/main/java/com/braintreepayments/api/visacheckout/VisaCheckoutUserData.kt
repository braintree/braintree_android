package com.braintreepayments.api.visacheckout

import android.os.Parcelable
import androidx.annotation.RestrictTo
import com.braintreepayments.api.sharedutils.Json
import kotlinx.parcelize.Parcelize
import org.json.JSONObject

/**
 * A class containing Visa Checkout information about the user.
 *
 * @property userFirstName The user's first name.
 * @property userLastName The user's last name.
 * @property userFullName The user's full name.
 * @property username The user's username.
 * @property userEmail The user's email.
 */
@Parcelize
data class VisaCheckoutUserData internal constructor(
    val userFirstName: String?,
    val userLastName: String?,
    val userFullName: String?,
    val username: String?,
    val userEmail: String?,
) : Parcelable {
    companion object {

        @JvmStatic
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        fun fromJson(jsonIn: JSONObject?): VisaCheckoutUserData {
            val json = jsonIn ?: JSONObject()
            return VisaCheckoutUserData(
                userFirstName = Json.optString(json, "userFirstName", ""),
                userLastName = Json.optString(json, "userLastName", ""),
                userFullName = Json.optString(json, "userFullName", ""),
                username = Json.optString(json, "userName", ""),
                userEmail = Json.optString(json, "userEmail", ""),
            )
        }
    }
}
