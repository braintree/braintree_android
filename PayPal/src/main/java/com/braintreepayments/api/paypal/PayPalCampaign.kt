package com.braintreepayments.api.paypal

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.json.JSONObject

/**
 * A PayPal co-marketing campaign to associate with a checkout order.
 *
 * @property id The PayPal-assigned campaign identifier.
 */
@Parcelize
data class PayPalCampaign(val id: String) : Parcelable {

    internal fun toJson(): JSONObject = JSONObject().put(CAMPAIGN_ID_KEY, id)

    companion object {
        private const val CAMPAIGN_ID_KEY = "id"
    }
}
