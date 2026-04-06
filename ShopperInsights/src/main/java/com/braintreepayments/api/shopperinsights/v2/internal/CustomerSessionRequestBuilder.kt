package com.braintreepayments.api.shopperinsights.v2.internal

import com.braintreepayments.api.core.ExperimentalBetaApi
import com.braintreepayments.api.shopperinsights.v2.CustomerSessionRequest
import com.braintreepayments.api.shopperinsights.v2.PayPalCampaign
import org.json.JSONArray
import org.json.JSONObject

/**
 * Builder for creating the JSON request objects for the Shopper Insights v2 APIs.
 */
@ExperimentalBetaApi
class CustomerSessionRequestBuilder {

    internal data class JsonRequestObjects(
        val customer: JSONObject,
        val purchaseUnits: JSONArray?,
        val payPalCampaigns: JSONArray? = null,
    )

    internal fun createRequestObjects(customerSessionRequest: CustomerSessionRequest): JsonRequestObjects {
        val customer = JSONObject().apply {
            putOpt(HASHED_EMAIL, customerSessionRequest.hashedEmail)
            putOpt(HASHED_PHONE_NUMBER, customerSessionRequest.hashedPhoneNumber)
            putOpt(PAYPAL_APP_INSTALLED, customerSessionRequest.payPalAppInstalled)
            putOpt(VENMO_APP_INSTALLED, customerSessionRequest.venmoAppInstalled)
        }

        val purchaseUnits = customerSessionRequest.purchaseUnits
            ?.takeIf { it.isNotEmpty() }
            ?.let { purchaseUnits ->
                JSONArray().apply {
                    purchaseUnits.forEach { purchaseUnit ->
                        put(
                            JSONObject().put(
                                AMOUNT,
                                JSONObject().apply {
                                    put(VALUE, purchaseUnit.amount)
                                    put(CURRENCY_CODE, purchaseUnit.currencyCode)
                                }
                            )
                        )
                    }
                }
            }

        val payPalCampaigns = payPalCampaignsToJson(customerSessionRequest.payPalCampaigns)

        return JsonRequestObjects(customer, purchaseUnits, payPalCampaigns)
    }

    /**
     * Builds the JSON array for `paypal_campaigns` (each element `{ "id": "..." }`, same as iOS `BTPayPalCampaign`).
     */
    internal fun payPalCampaignsToJson(payPalCampaigns: List<PayPalCampaign>?): JSONArray? {
        return payPalCampaigns?.takeIf { it.isNotEmpty() }?.let { campaigns ->
            JSONArray().apply {
                campaigns.forEach { campaign ->
                    put(JSONObject().put(CAMPAIGN_ID_KEY, campaign.id))
                }
            }
        }
    }

    private companion object {
        private const val HASHED_EMAIL = "hashedEmail"
        private const val HASHED_PHONE_NUMBER = "hashedPhoneNumber"
        private const val PAYPAL_APP_INSTALLED = "paypalAppInstalled"
        private const val VENMO_APP_INSTALLED = "venmoAppInstalled"
        private const val AMOUNT = "amount"
        private const val VALUE = "value"
        private const val CURRENCY_CODE = "currencyCode"
        private const val CAMPAIGN_ID_KEY = "id"
    }
}
