package com.braintreepayments.api.sepadirectdebit

import com.braintreepayments.api.core.BraintreeClient
import com.braintreepayments.api.sepadirectdebit.SEPADirectDebitNonce.Companion.fromJSON
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException

internal class SEPADirectDebitApi(
    private val braintreeClient: BraintreeClient,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Main,
    private val coroutineScope: CoroutineScope = CoroutineScope(dispatcher)
) {

    fun createMandate(
        sepaDirectDebitRequest: SEPADirectDebitRequest,
        returnUrlScheme: String,
        callback: CreateMandateCallback
    ) {
        try {
            val jsonObject =
                buildCreateMandateRequest(sepaDirectDebitRequest, returnUrlScheme)
            val url = "/v1/sepa_debit"
            coroutineScope.launch {
                try {
                    val responseBody = braintreeClient.sendPOST(
                        url,
                        jsonObject.toString()
                    )
                    try {
                        val result = parseCreateMandateResponse(responseBody)
                        callback.onResult(result, null)
                    } catch (e: JSONException) {
                        callback.onResult(null, e)
                    }
                } catch (httpError: IOException) {
                    callback.onResult(null, httpError)
                }
            }
        } catch (e: JSONException) {
            callback.onResult(null, e)
        }
    }

    fun tokenize(
        ibanLastFour: String,
        customerId: String,
        bankReferenceToken: String,
        mandateType: String,
        callback: SEPADirectDebitInternalTokenizeCallback
    ) {
        try {
            val jsonObject =
                buildTokenizeRequest(ibanLastFour, customerId, bankReferenceToken, mandateType)
            val url = "/v1/payment_methods/sepa_debit_accounts"
            coroutineScope.launch {
                try {
                    val responseBody = braintreeClient.sendPOST(
                        url,
                        jsonObject.toString()
                    )
                    try {
                        val nonce = parseTokenizeResponse(responseBody)
                        callback.onResult(nonce, null)
                    } catch (e: JSONException) {
                        callback.onResult(null, e)
                    }
                } catch (httpError: IOException) {
                    callback.onResult(null, httpError)
                }
            }
        } catch (e: JSONException) {
            callback.onResult(null, e)
        }
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
