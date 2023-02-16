package com.braintreepayments.api

import android.os.Parcelable
import android.text.TextUtils
import android.os.Parcel
import com.braintreepayments.api.PostalAddress

// NEXT_MAYOR_VERSION: transform to data class and check for @Parcelize annotation
/**
 * Java object representing a postal address
 */
open class PostalAddress : Parcelable {
    var recipientName: String? = null
    var phoneNumber: String? = null
    var streetAddress: String? = null
    var extendedAddress: String? = null
    var locality: String? = null
    var region: String? = null
    var postalCode: String? = null
    var sortingCode: String? = null
    var countryCodeAlpha2: String? = null

    constructor()

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

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(streetAddress)
        dest.writeString(extendedAddress)
        dest.writeString(locality)
        dest.writeString(region)
        dest.writeString(postalCode)
        dest.writeString(countryCodeAlpha2)
        dest.writeString(recipientName)
        dest.writeString(phoneNumber)
        dest.writeString(sortingCode)
    }

    private constructor(`in`: Parcel) {
        streetAddress = `in`.readString()
        extendedAddress = `in`.readString()
        locality = `in`.readString()
        region = `in`.readString()
        postalCode = `in`.readString()
        countryCodeAlpha2 = `in`.readString()
        recipientName = `in`.readString()
        phoneNumber = `in`.readString()
        sortingCode = `in`.readString()
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<PostalAddress?> =
            object : Parcelable.Creator<PostalAddress?> {
                override fun createFromParcel(source: Parcel): PostalAddress? {
                    return PostalAddress(source)
                }

                override fun newArray(size: Int): Array<PostalAddress?> {
                    return arrayOfNulls(size)
                }
            }
    }
}