package com.braintreepayments.api

import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.RestrictTo
import org.json.JSONObject

/**
 * Representation of a user phone number.
 *
 * @property countryCode The international country code for the shopper's phone number
 * (e.g., "1" for the United States).
 * @property nationalNumber The national segment of the shopper's phone number
 * (excluding the country code).
 */
data class PayPalPhoneNumber(
    var countryCode: String,
    var nationalNumber: String,
) : Parcelable {

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put(COUNTRY_CODE_KEY, countryCode)
            put(NATIONAL_NUMBER_KEY, nationalNumber)
        }
    }

    companion object {
        private const val COUNTRY_CODE_KEY: String = "country_code"
        private const val NATIONAL_NUMBER_KEY: String = "national_number"

        @JvmField
        val CREATOR: Parcelable.Creator<PayPalPhoneNumber> = object : Parcelable.Creator<PayPalPhoneNumber> {
            override fun createFromParcel(parcel: Parcel): PayPalPhoneNumber {
                return PayPalPhoneNumber(
                    parcel.readString() ?: "",
                    parcel.readString() ?: "",
                )
            }

            override fun newArray(size: Int): Array<PayPalPhoneNumber?> {
                return arrayOfNulls(size)
            }
        }
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(countryCode)
        parcel.writeString(nationalNumber)
    }

    override fun describeContents(): Int {
        return 0
    }
}
