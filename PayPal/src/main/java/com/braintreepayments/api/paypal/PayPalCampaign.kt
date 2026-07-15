package com.braintreepayments.api.paypal

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * A PayPal co-marketing campaign to associate with a checkout order.
 *
 * @property id The PayPal-assigned campaign identifier.
 */
@Parcelize
data class PayPalCampaign(val id: String) : Parcelable
