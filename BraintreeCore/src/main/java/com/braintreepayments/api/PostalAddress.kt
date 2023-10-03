package com.braintreepayments.api

import android.os.Parcelable
import android.text.TextUtils
import android.os.Parcel

// NEXT_MAYOR_VERSION: transform to data class and check for @Parcelize annotation
/**
 * Java object representing a postal address
 */
open class PostalAddress : Parcelable {
    open var recipientName: String? = null
    open var phoneNumber: String? = null
    open var streetAddress: String? = null
    open var extendedAddress: String? = null
    open var locality: String? = null
    open var region: String? = null
    open var postalCode: String? = null
    open var sortingCode: String? = null
    open var countryCodeAlpha2: String? = null

    constructor()

    /**
     * A [PostalAddress] is considered empty if it does not have a country code.
     *
     * @return `true` if the country code is `null` or empty, `false` otherwise.
     */
    open val isEmpty: Boolean
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

    private constructor(parcel: Parcel) {
        streetAddress = parcel.readString()
        extendedAddress = parcel.readString()
        locality = parcel.readString()
        region = parcel.readString()
        postalCode = parcel.readString()
        countryCodeAlpha2 = parcel.readString()
        recipientName = parcel.readString()
        phoneNumber = parcel.readString()
        sortingCode = parcel.readString()
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<PostalAddress> =
            object : Parcelable.Creator<PostalAddress> {
                override fun createFromParcel(source: Parcel): PostalAddress {
                    return PostalAddress(source)
                }

                override fun newArray(size: Int): Array<PostalAddress?> {
                    return arrayOfNulls(size)
                }
            }
    }
}
