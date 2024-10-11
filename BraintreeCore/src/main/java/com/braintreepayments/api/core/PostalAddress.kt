package com.braintreepayments.api.core

import kotlinx.parcelize.Parcelize
import android.text.TextUtils
import android.os.Parcelable

/**
 * Object representing a postal address
 */
@Parcelize
data class PostalAddress(
    var recipientName: String? = null,
    var phoneNumber: String? = null,
    var streetAddress: String? = null,
    var extendedAddress: String? = null,
    var locality: String? = null,
    var region: String? = null,
    var postalCode: String? = null,
    var sortingCode: String? = null,
    var countryCodeAlpha2: String? = null
) : Parcelable {
    /**
     * A [PostalAddress] is considered empty if it does not have a country code.
     *
     * @return `true` if the country code is `null` or empty, `false` otherwise.
     */
    val isEmpty: Boolean
        get() = TextUtils.isEmpty(countryCodeAlpha2)

    override fun toString(): String {
        return "$recipientName\n$streetAddress\n$extendedAddress\n$locality," +
                " $region\n$postalCode $countryCodeAlpha2"
    }
}
