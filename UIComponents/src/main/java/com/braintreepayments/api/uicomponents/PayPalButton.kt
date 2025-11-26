package com.braintreepayments.api.uicomponents

import android.content.Context
import android.net.Uri
import com.braintreepayments.api.paypal.PayPalClient
import com.braintreepayments.api.paypal.PayPalPaymentAuthResult
import com.braintreepayments.api.paypal.PayPalRequest
import com.braintreepayments.api.paypal.PayPalTokenizeCallback

/**
 * Class that handles the PayPal payment flow for a PayPalButton.
 *
 * @param button The PayPalButton UI component to manage
 * @param context Application or Activity context
 * @param authorization Braintree authorization
 * @param appLinkReturnUrl The app link return URL for PayPal flow
 * @param deepLinkFallbackUrlScheme The deep link fallback URL scheme
 */
class PayPalButton(
    private val button: PayPalButtonView,
    context: Context,
    authorization: String,
    appLinkReturnUrl: Uri,
    deepLinkFallbackUrlScheme: String? = null
) {
    private var payPalClient: PayPalClient = PayPalClient(
        context,
        authorization,
        appLinkReturnUrl,
        deepLinkFallbackUrlScheme
    )

    private var payPalRequest: PayPalRequest? = null

    fun setPayPalClient(payPalClient: PayPalClient) {
        this.payPalClient = payPalClient
    }

    fun tokenize(paymentAuthResult: PayPalPaymentAuthResult.Success, payPalResult: PayPalTokenizeCallback) {
        payPalClient.tokenize(paymentAuthResult, payPalResult)
    }

    fun updatePayPalRequest(request: PayPalRequest) {
        this.payPalRequest = request
    }
}
