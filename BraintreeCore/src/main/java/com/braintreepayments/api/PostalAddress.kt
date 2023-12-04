package com.braintreepayments.api

import kotlinx.parcelize.Parcelize
import android.text.TextUtils
import android.os.Parcelable

/**
 * Java object representing a postal address
 */
@Parcelize
data class PostalAddress(
    var recipientName: String?,
    var phoneNumber: String?,
    var streetAddress: String?,
    var extendedAddress: String?,
    var locality: String?,
    var region: String?,
    var postalCode: String?,
    var sortingCode: String?,
    var countryCodeAlpha2: String?) : Parcelable {

    constructor() : this(null, null, null, null, null, null, null, null, null)

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
