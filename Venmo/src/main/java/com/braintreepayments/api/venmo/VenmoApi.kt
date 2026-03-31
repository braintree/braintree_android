package com.braintreepayments.api.venmo

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
    private val merchantRepository: MerchantRepository = MerchantRepository.instance
) {

    @Suppress("LongMethod")
    suspend fun createPaymentContext(
        request: VenmoRequest,
        venmoProfileId: String?
    ): String? {
        val params = JSONObject()
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

        val responseBody = braintreeClient.sendGraphQLPOST(params)
        return paymentContextResponse(responseBody)
    }

    private fun paymentContextResponse(
        responseBody: String
    ): String {
        val paymentContextId =
            parsePaymentContextId(responseBody)
        if (paymentContextId.isNullOrEmpty()) {
            throw BraintreeException(
                "Failed to fetch a Venmo paymentContextId while constructing the requestURL."
            )
        }
        return paymentContextId
    }

    suspend fun createNonceFromPaymentContext(
        paymentContextId: String
    ): VenmoAccountNonce {
        val params = JSONObject()
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

        val responseBody = braintreeClient.sendGraphQLPOST(params)
        val data = JSONObject(responseBody).getJSONObject("data")
        val nonce = fromJSON(data.getJSONObject("node"))
        return nonce
    }

    suspend fun vaultVenmoAccountNonce(nonce: String): VenmoAccountNonce {
        val venmoAccount = VenmoAccount(nonce)

        val tokenizationResponse = apiClient.tokenizeREST(venmoAccount)
        val venmoAccountNonce = fromJSON(tokenizationResponse)
        return venmoAccountNonce
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
