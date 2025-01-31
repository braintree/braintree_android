package com.braintreepayments.api.paypal

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Representation of a recipient Contact Information for the order.
 *
 * @property recipientEmail Email address of the recipient.
 * @property recipentPhoneNumber Phone number of the recipient.
 */
@Parcelize
data class PayPalContactInformation(
    val recipientEmail: String? = null,
    val recipentPhoneNumber: PayPalPhoneNumber? = null
) : Parcelable
