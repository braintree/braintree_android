package com.braintreepayments.api.venmo

import android.text.TextUtils
import com.braintreepayments.api.core.AnalyticsParamRepository
import com.braintreepayments.api.core.ApiClient
import com.braintreepayments.api.core.BraintreeClient
import com.braintreepayments.api.core.BraintreeException
import com.braintreepayments.api.core.MerchantRepository
import com.braintreepayments.api.core.MetadataBuilder
import com.braintreepayments.api.venmo.VenmoAccountNonce.Companion.fromJSON
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

internal class VenmoApi(
    private val braintreeClient: BraintreeClient,
    private val apiClient: ApiClient,
    private val analyticsParamRepository: AnalyticsParamRepository = AnalyticsParamRepository.instance,
    private val merchantRepository: MerchantRepository = MerchantRepository.instance,
) {

    @Suppress("LongMethod")
    fun createPaymentContext(
        request: VenmoRequest,
        venmoProfileId: String?,
        callback: VenmoApiCallback
    ) {
        val params = JSONObject()
        try {
            params.put(
                "query", """
                mutation CreateVenmoPaymentContext(${'$'}input: CreateVenmoPaymentContextInput!) { 
                    createVenmoPaymentContext(input: ${'$'}input) { 
                        venmoPaymentContext { id } 
                    } 
                }
                """.trimIndent()

            )
            val input = JSONObject()
            input.put("paymentMethodUsage", request.paymentMethodUsage.name)
            input.put("venmoRiskCorrelationId", request.riskCorrelationId)
            input.put("merchantProfileId", venmoProfileId)
            input.put("customerClient", "MOBILE_APP")
            input.put("intent", "CONTINUE")
            input.put("isFinalAmount", request.isFinalAmount.toString())
            val paysheetDetails = JSONObject()
            paysheetDetails.put(
                "collectCustomerShippingAddress",
                request.collectCustomerShippingAddress.toString()
            )
            paysheetDetails.put(
                "collectCustomerBillingAddress",
                request.collectCustomerBillingAddress.toString()
            )

            val transactionDetails = JSONObject()
            transactionDetails.put("subTotalAmount", request.subTotalAmount)
            transactionDetails.put("discountAmount", request.discountAmount)
            transactionDetails.put("taxAmount", request.taxAmount)
            transactionDetails.put("shippingAmount", request.shippingAmount)
            transactionDetails.put("totalAmount", request.totalAmount)

            if (!request.lineItems.isNullOrEmpty()) {
                val lineItems = JSONArray()
                for (lineItem in request.lineItems!!) {
                    if (lineItem.unitTaxAmount == null || lineItem.unitTaxAmount == "") {
                        lineItem.unitTaxAmount = "0"
                    }
                    lineItems.put(lineItem.toJson())
                }
                transactionDetails.put("lineItems", lineItems)
            }

            if (transactionDetails.length() > 0) {
                paysheetDetails.put("transactionDetails", transactionDetails)
            }
            input.put("paysheetDetails", paysheetDetails)

            input.putOpt("displayName", request.displayName)

            val variables = JSONObject()
            variables.put("input", input)
            params.put("variables", variables)

            val braintreeData = MetadataBuilder()
                .sessionId(analyticsParamRepository.sessionId)
                .integration(merchantRepository.integrationType)
                .version()
                .build()

            params.put("clientSdkMetadata", braintreeData)
        } catch (e: JSONException) {
            callback.onResult(null, BraintreeException("unexpected error"))
        }

        braintreeClient.sendGraphQLPOST(params) { responseBody: String?, httpError: Exception? ->
            paymentContextResponse(responseBody, callback, httpError)
        }
    }

    private fun paymentContextResponse(
        responseBody: String?,
        callback: VenmoApiCallback,
        httpError: Exception?
    ) {
        if (responseBody != null) {
            val paymentContextId = parsePaymentContextId(responseBody)
            if (TextUtils.isEmpty(paymentContextId)) {
                callback.onResult(
                    null, BraintreeException(
                        "Failed to fetch a Venmo paymentContextId while constructing the requestURL."
                    )
                )
                return
            }
            callback.onResult(paymentContextId, null)
        } else {
            callback.onResult(null, httpError)
        }
    }

    fun createNonceFromPaymentContext(
        paymentContextId: String?,
        callback: VenmoInternalCallback
    ) {
        val params = JSONObject()
        try {
            params.put(
                "query",
                """
                query PaymentContext(${'$'}id: ID!) { 
                    node(id: ${'$'}id) { 
                        ... on VenmoPaymentContext { 
                            paymentMethodId 
                            userName 
                            payerInfo { 
                                firstName lastName phoneNumber email externalId userName  
                                shippingAddress { 
                                    fullName addressLine1 addressLine2 adminArea1 adminArea2 
                                    postalCode countryCode 
                                } 
                                billingAddress { 
                                    fullName addressLine1 addressLine2 adminArea1 adminArea2 
                                    postalCode countryCode 
                                } 
                            } 
                        } 
                    } 
                }
                """.trimIndent()

            )
            val variables = JSONObject()
            variables.put("id", paymentContextId)
            params.put("variables", variables)

            braintreeClient.sendGraphQLPOST(params) { responseBody: String?, httpError: Exception? ->
                if (responseBody != null) {
                    try {
                        val data = JSONObject(responseBody).getJSONObject("data")
                        val nonce =
                            fromJSON(data.getJSONObject("node"))

                        callback.onResult(nonce, null)
                    } catch (exception: JSONException) {
                        callback.onResult(null, exception)
                    }
                } else {
                    callback.onResult(null, httpError)
                }
            }
        } catch (exception: JSONException) {
            callback.onResult(null, exception)
        }
    }

    fun vaultVenmoAccountNonce(nonce: String?, callback: VenmoInternalCallback) {
        val venmoAccount = VenmoAccount(nonce)

        apiClient.tokenizeREST(venmoAccount) { tokenizationResponse: JSONObject?, exception: Exception? ->
            if (tokenizationResponse != null) {
                try {
                    val venmoAccountNonce =
                        fromJSON(tokenizationResponse)
                    callback.onResult(venmoAccountNonce, null)
                } catch (e: JSONException) {
                    callback.onResult(null, e)
                }
            } else {
                callback.onResult(null, exception)
            }
        }
    }

    companion object {
        private fun parsePaymentContextId(createPaymentContextResponse: String): String? {
            var paymentContextId: String? = null
            try {
                val data = JSONObject(createPaymentContextResponse).getJSONObject("data")
                val createVenmoPaymentContext = data.getJSONObject("createVenmoPaymentContext")
                val venmoPaymentContext =
                    createVenmoPaymentContext.getJSONObject("venmoPaymentContext")
                paymentContextId = venmoPaymentContext.getString("id")
            } catch (ignored: JSONException) { /* do nothing */
            }

            return paymentContextId
        }
    }
}
