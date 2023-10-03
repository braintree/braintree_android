package com.braintreepayments.api

import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting

/**
 * Base class representing a method of payment for a customer. [PaymentMethodNonce] represents the
 * common interface of all payment method nonces, and can be handled by a server interchangeably.
 *
 * @property string The nonce generated for this payment method by the Braintree gateway. The nonce will
 * represent this PaymentMethod for the purposes of creating transactions and other monetary
 * actions.
 * @property isDefault `true` if this payment method is the default for the current customer, `false` otherwise
 */
open class PaymentMethodNonce : Parcelable {

    companion object {
        @JvmField
        val CREATOR = object : Parcelable.Creator<PaymentMethodNonce> {
            override fun createFromParcel(`in`: Parcel) = PaymentMethodNonce(`in`)
            override fun newArray(size: Int) = arrayOfNulls<PaymentMethodNonce>(size)
        }
    }

    open val string: String
    open val isDefault: Boolean

    /**
     * @suppress
     */
    @VisibleForTesting
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    constructor(nonce: String, isDefault: Boolean) {
        string = nonce
        this.isDefault = isDefault
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(string)
        dest.writeByte(if (isDefault) 1.toByte() else 0.toByte())
    }

    protected constructor(inParcel: Parcel) {
        string = inParcel.readString() ?: ""
        isDefault = inParcel.readByte() > 0
    }
}
