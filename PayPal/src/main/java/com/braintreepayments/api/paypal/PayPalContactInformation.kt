package com.braintreepayments.api.paypal

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.json.JSONObject

/**
 * Representation of a recipient Contact Information for the order.
 *
 * @property recipientEmail Email address of the recipient.
 * @property recipentPhoneNumber Phone number of the recipient.
 */
@Parcelize
data class PayPalContactInformation(
    val recipientEmail: String?,
    val recipentPhoneNumber: PayPalPhoneNumber?
) : Parcelable {

    internal fun toJson(): JSONObject {
        return JSONObject().apply {
            put(RECIPIENT_EMAIL_KEY, recipientEmail)
            put(RECIPIENT_PHONE_NUMBER_KEY, recipentPhoneNumber)
        }
    }

    companion object {
        private const val RECIPIENT_EMAIL_KEY: String = "recipient_email"
        private const val RECIPIENT_PHONE_NUMBER_KEY: String = "international_phone"
    }
}
