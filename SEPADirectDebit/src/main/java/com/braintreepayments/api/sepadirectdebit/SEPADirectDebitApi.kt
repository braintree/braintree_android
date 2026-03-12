package com.braintreepayments.api.sepadirectdebit

import com.braintreepayments.api.core.BraintreeClient
import com.braintreepayments.api.sepadirectdebit.SEPADirectDebitNonce.Companion.fromJSON
import org.json.JSONException
import org.json.JSONObject

internal class SEPADirectDebitApi(
    private val braintreeClient: BraintreeClient
) {

    suspend fun createMandate(
        sepaDirectDebitRequest: SEPADirectDebitRequest,
        returnUrlScheme: String
    ): CreateMandateResult {
        val jsonObject = buildCreateMandateRequest(sepaDirectDebitRequest, returnUrlScheme)
        val url = "/v1/sepa_debit"
        val responseBody = braintreeClient.sendPOST(url, jsonObject.toString())
        return parseCreateMandateResponse(responseBody)
    }

    suspend fun tokenize(
        ibanLastFour: String,
        customerId: String,
        bankReferenceToken: String,
        mandateType: String
    ): SEPADirectDebitNonce {
        val jsonObject = buildTokenizeRequest(ibanLastFour, customerId, bankReferenceToken, mandateType)
        val url = "/v1/payment_methods/sepa_debit_accounts"
        val responseBody = braintreeClient.sendPOST(url, jsonObject.toString())
        return parseTokenizeResponse(responseBody)
    }

    @Throws(JSONException::class)
    private fun parseTokenizeResponse(responseBody: String): SEPADirectDebitNonce {
        val jsonResponse = JSONObject(responseBody)
        return fromJSON(jsonResponse)
    }

    @Throws(JSONException::class)
    private fun buildTokenizeRequest(
        ibanLastFour: String, customerId: String,
        bankReferenceToken: String, mandateType: String
    ): JSONObject {
        val accountData = JSONObject()
            .put("last_4", ibanLastFour)
            .put("merchant_or_partner_customer_id", customerId)
            .put("bank_reference_token", bankReferenceToken)
            .put("mandate_type", mandateType)
        val requestData = JSONObject()
            .put("sepa_debit_account", accountData)

        return requestData
    }

    @Throws(JSONException::class)
    private fun parseCreateMandateResponse(responseBody: String): CreateMandateResult {
        val json = JSONObject(responseBody)
        val sepaDebitAccount = json.getJSONObject("message").getJSONObject("body")
            .getJSONObject("sepaDebitAccount")
        val approvalUrl = sepaDebitAccount.getString("approvalUrl")
        val ibanLastFour = sepaDebitAccount.getString("last4")
        val customerId = sepaDebitAccount.getString("merchantOrPartnerCustomerId")
        val bankReferenceToken = sepaDebitAccount.getString("bankReferenceToken")
        val mandateType = sepaDebitAccount.getString("mandateType")

        return CreateMandateResult(
            approvalUrl,
            ibanLastFour,
            customerId,
            bankReferenceToken,
            SEPADirectDebitMandateType.valueOf(mandateType)
        )
    }

    @Throws(JSONException::class)
    private fun buildCreateMandateRequest(
        sepaDirectDebitRequest: SEPADirectDebitRequest,
        returnUrlScheme: String
    ): JSONObject {
        val sepaDebitData = JSONObject()
            .putOpt("account_holder_name", sepaDirectDebitRequest.accountHolderName)
            .putOpt("merchant_or_partner_customer_id", sepaDirectDebitRequest.customerId)
            .putOpt("iban", sepaDirectDebitRequest.iban)
            .putOpt("mandate_type", sepaDirectDebitRequest.mandateType.toString())

        val requestBillingAddress = sepaDirectDebitRequest.billingAddress
        if (requestBillingAddress != null) {
            val billingAddress = JSONObject()
                .putOpt(
                    "address_line_1",
                    requestBillingAddress.streetAddress
                )
                .putOpt(
                    "address_line_2",
                    requestBillingAddress.extendedAddress
                )
                .putOpt(
                    "admin_area_1",
                    requestBillingAddress.locality
                )
                .putOpt("admin_area_2",
                    requestBillingAddress.region
                )
                .putOpt(
                    "postal_code",
                    requestBillingAddress.postalCode
                )
                .putOpt(
                    "country_code",
                    requestBillingAddress.countryCodeAlpha2
                )

            sepaDebitData.put("billing_address", billingAddress)
        }

        val cancelUrl = "$returnUrlScheme://sepa/cancel"
        val successUrl = "$returnUrlScheme://sepa/success"

        val requestData = JSONObject()
            .put("sepa_debit", sepaDebitData)
            .put("cancel_url", cancelUrl)
            .put("return_url", successUrl)

        if (sepaDirectDebitRequest.merchantAccountId != null) {
            requestData.putOpt(
                "merchant_account_id",
                sepaDirectDebitRequest.merchantAccountId
            )
        }

        if (sepaDirectDebitRequest.locale != null) {
            requestData.putOpt("locale", sepaDirectDebitRequest.locale)
        }

        return requestData
    }
}
