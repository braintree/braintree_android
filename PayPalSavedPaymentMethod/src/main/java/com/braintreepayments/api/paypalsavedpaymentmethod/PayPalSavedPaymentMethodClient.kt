package com.braintreepayments.api.paypalsavedpaymentmethod

import android.content.Context
import com.braintreepayments.api.core.BraintreeClient
import com.braintreepayments.api.core.BraintreeException
import com.braintreepayments.api.core.ClientToken
import com.braintreepayments.api.core.ExperimentalBetaApi
import com.braintreepayments.api.core.GraphQLConstants
import com.braintreepayments.api.core.MerchantRepository
import com.braintreepayments.api.paypal.PayPalCheckoutRequest
import com.braintreepayments.api.paypal.PayPalClient
import com.braintreepayments.api.paypal.PayPalPaymentAuthRequest
import com.braintreepayments.api.paypal.PayPalPaymentAuthResult
import com.braintreepayments.api.paypal.PayPalResult
import com.braintreepayments.api.paypalsavedpaymentmethod.model.PayPalCreditMessagingResult
import com.braintreepayments.api.paypalsavedpaymentmethod.model.PayPalSavedPaymentMethodSummary
import com.braintreepayments.api.sharedutils.Json
import org.json.JSONException
import org.json.JSONObject

/**
 * Fetches what to display for a buyer's vaulted PayPal payment method: the buyer's saved funding instrument, and the
 * Pay Later message that accompanies it. Also runs the PayPal side of the edit flow; the views launch the paysheet
 * with `PayPalLauncher` between [createEditFundingInstrumentAuthRequest] and [tokenize].
 *
 * Requires a client token generated with the buyer's payment method ID. A tokenization key carries no
 * `paymentMethodIdJwt`, so the saved funding instrument cannot be resolved.
 */
@OptIn(ExperimentalBetaApi::class)
internal class PayPalSavedPaymentMethodClient internal constructor(
    private val braintreeClient: BraintreeClient,
    private val payPalClient: PayPalClient,
    private val merchantRepository: MerchantRepository = MerchantRepository.instance
) {

    /**
     * Starts the edit flow by creating the PayPal auth request, seeded with the buyer's billing agreement. Launch it
     * with `PayPalLauncher`, then pass the result to [tokenize].
     *
     * @param context an Android Context.
     * @param payPalCheckoutRequest the checkout the edited funding instrument applies to.
     * @return the [PayPalPaymentAuthRequest] to launch, or a failure from PayPal.
     * @throws PayPalSavedPaymentMethodException.InvalidAuthorization if the authorization is not a client token.
     * @throws PayPalSavedPaymentMethodException.MissingPaymentMethodIdJwt if the client token carries no payment method
     * ID JWT. Both are thrown before any request, so the paysheet never opens as a regular checkout.
     */
    suspend fun createEditFundingInstrumentAuthRequest(
        context: Context,
        payPalCheckoutRequest: PayPalCheckoutRequest
    ): PayPalPaymentAuthRequest {
        if (requireClientToken().paymentMethodIdJwt == null) {
            throw PayPalSavedPaymentMethodException.MissingPaymentMethodIdJwt()
        }
        return payPalClient.createPaymentAuthRequestForEditFi(context, payPalCheckoutRequest)
    }

    /**
     * Tokenizes the PayPal account once the buyer returns from the edit flow. On success, the nonce's `paymentId` is
     * the approved order ID to pass to [fetchPaymentMethod] with
     * [PayPalFundingInstrumentFetchType.BUYER_UPDATED_BILLING_AGREEMENT].
     *
     * @param paymentAuthResult the success returned by `PayPalLauncher.handleReturnToApp`.
     */
    suspend fun tokenize(paymentAuthResult: PayPalPaymentAuthResult.Success): PayPalResult =
        payPalClient.tokenize(paymentAuthResult)

    /**
     * Fetches the funding instrument details for a vaulted PayPal payment method.
     *
     * @param fundingInstrumentType Which funding instrument to resolve.
     * [PayPalFundingInstrumentFetchType.BUYER_DEFAULT_BILLING_AGREEMENT] uses the payment method ID JWT carried by the
     * client token; [PayPalFundingInstrumentFetchType.BUYER_UPDATED_BILLING_AGREEMENT] requires [orderId].
     * @param orderId The approved checkout order ID. Required for a buyer updated billing agreement and ignored
     * otherwise.
     * @param merchantAccountId Optional. A non-default merchant account to resolve the funding instrument against.
     * Applies to both fetch types and is omitted from the request when null.
     * @return The funding instrument details to display for the buyer.
     * @throws PayPalSavedPaymentMethodException if the authorization is not a client token, the identity field for
     * [fundingInstrumentType] is missing or blank, or the response is empty, is not valid JSON, or cannot be parsed.
     * @throws BraintreeException if the GraphQL response contains errors.
     * @throws java.io.IOException if the request fails.
     */
    @Suppress("ThrowsCount")
    suspend fun fetchPaymentMethod(
        fundingInstrumentType: PayPalFundingInstrumentFetchType,
        orderId: String? = null,
        merchantAccountId: String? = null
    ): PayPalSavedPaymentMethodSummary {
        // TODO: send the default and updated billing agreement analytics events once the catalog is approved.
        val clientToken = requireClientToken()

        // The API rejects the request unless exactly the identity field matching the fetch type is sent.
        val body = when (fundingInstrumentType) {
            PayPalFundingInstrumentFetchType.BUYER_DEFAULT_BILLING_AGREEMENT -> {
                val paymentMethodIdJwt = clientToken.paymentMethodIdJwt
                    ?: throw PayPalSavedPaymentMethodException.MissingPaymentMethodIdJwt()

                PayPalFundingInstrumentDetailsGraphQLBody(
                    fundingInstrumentType = fundingInstrumentType,
                    paymentMethodIdJwt = paymentMethodIdJwt,
                    orderId = null,
                    merchantAccountId = merchantAccountId
                )
            }

            PayPalFundingInstrumentFetchType.BUYER_UPDATED_BILLING_AGREEMENT -> {
                if (orderId.isNullOrBlank()) throw PayPalSavedPaymentMethodException.MissingOrderId()

                PayPalFundingInstrumentDetailsGraphQLBody(
                    fundingInstrumentType = fundingInstrumentType,
                    paymentMethodIdJwt = null,
                    orderId = orderId,
                    merchantAccountId = merchantAccountId
                )
            }
        }

        val responseBody = braintreeClient.sendGraphQLPOST(body.toJson())
        if (responseBody.isEmpty()) throw PayPalSavedPaymentMethodException.EmptyBodyReturned()

        // GraphQL returns errors in a 200 response, so they are checked here.
        val response = try {
            JSONObject(responseBody)
        } catch (e: JSONException) {
            throw PayPalSavedPaymentMethodException.FailedToParseSummary(e)
        }
        val errors = response.optJSONArray(GraphQLConstants.Keys.ERRORS)
        if (errors != null && errors.length() > 0) {
            val message = Json.optString(errors.optJSONObject(0), GraphQLConstants.Keys.MESSAGE, null)
                ?.takeIf { it.isNotBlank() }
            throw BraintreeException(message ?: GRAPHQL_ERROR_FALLBACK_MESSAGE)
        }

        val details = response
            .optJSONObject(DATA_KEY)
            ?.optJSONObject(PAYPAL_FUNDING_INSTRUMENT_DETAILS_KEY)
        return PayPalSavedPaymentMethodSummary.fromJson(details)
            ?: throw PayPalSavedPaymentMethodException.FailedToParseSummary()
    }

    /**
     * Fetches the PayPal Pay Later message to display alongside the funding instrument. The message is optional, so
     * when this throws, callers should hide only the Pay Later message and keep showing the funding instrument; it
     * must not fail checkout.
     *
     * @param amount The order amount the message is calculated from, for example `"55.00"`.
     * @param currencyCode The ISO-4217 currency code for [amount], for example `"USD"`.
     * @return The Pay Later message to display.
     * @throws PayPalSavedPaymentMethodException if the authorization is not a client token, the response is empty, or
     * PayPal returns no message.
     * @throws org.json.JSONException if the response is not valid JSON.
     * @throws java.io.IOException if the request fails.
     */
    suspend fun fetchCreditPresentmentMessages(
        amount: String,
        currencyCode: String
    ): PayPalCreditMessagingResult {
        // TODO: send the credit messaging analytics events once the catalog is approved.
        requireClientToken()

        val configuration = braintreeClient.getConfiguration()
        val baseUrl = if (configuration.environment == PRODUCTION) PRODUCTION_BASE_URL else SANDBOX_BASE_URL
        val responseBody = braintreeClient.sendPOST(
            url = "$baseUrl$CREDIT_PRESENTMENT_MESSAGES_PATH",
            data = PayPalCreditMessagingRequest(amount = amount, currencyCode = currencyCode).toJson().toString()
        )
        if (responseBody.isEmpty()) throw PayPalSavedPaymentMethodException.EmptyBodyReturned()

        return PayPalCreditMessagingResult.fromJson(JSONObject(responseBody))
            ?: throw PayPalSavedPaymentMethodException.MissingPreferredMessage()
    }

    private fun requireClientToken(): ClientToken =
        merchantRepository.authorization as? ClientToken
            ?: throw PayPalSavedPaymentMethodException.InvalidAuthorization()

    private companion object {
        const val DATA_KEY = "data"
        const val PAYPAL_FUNDING_INSTRUMENT_DETAILS_KEY = "paypalFundingInstrumentDetails"
        const val GRAPHQL_ERROR_FALLBACK_MESSAGE = "Unable to fetch the funding instrument details."

        const val PRODUCTION = "production"
        const val PRODUCTION_BASE_URL = "https://api.paypal.com"
        const val SANDBOX_BASE_URL = "https://api.sandbox.paypal.com"
        const val CREDIT_PRESENTMENT_MESSAGES_PATH = "/v2/credit/fetch-presentment-messages"
    }
}
