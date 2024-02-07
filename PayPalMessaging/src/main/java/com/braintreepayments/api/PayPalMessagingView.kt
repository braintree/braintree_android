package com.braintreepayments.api

import android.content.Context
import android.view.ViewGroup
import com.paypal.messages.PayPalMessageView
import com.paypal.messages.config.message.PayPalMessageData
import com.paypal.messages.config.PayPalEnvironment
import com.paypal.messages.config.message.PayPalMessageConfig
import com.paypal.messages.config.message.PayPalMessageEventsCallbacks
import com.paypal.messages.config.message.PayPalMessageStyle
import com.paypal.messages.config.message.PayPalMessageViewStateCallbacks

/**
 *  Use [PayPalMessagingView] to display PayPal messages to promote offers such as Pay Later
 *  and PayPal Credit to customers.
 * Note: **This module is in beta. It's public API may change or be removed in future releases.**
 * @property braintreeClient a {@link BraintreeClient}
 */
class PayPalMessagingView(private val braintreeClient: BraintreeClient) {
    var payPalMessagingListener: PayPalMessagingListener? = null

    /**
     * Creates a view to be displayed to promote offers such as Pay Later and PayPal Credit to customers.
     * @property context the Android Context
     * @property request An optional [PayPalMessagingRequest]
     * Note: **This module is in beta. It's public API may change or be removed in future releases.**
     */
    fun start(context: Context, request: PayPalMessagingRequest = PayPalMessagingRequest()) {
        braintreeClient.sendAnalyticsEvent(PayPalMessagingAnalytics.STARTED.value)

        braintreeClient.getConfiguration { configuration, configError ->
            if (configError != null) {
                notifyFailure(error = configError)
            } else if (configuration != null) {
                val clientId = configuration.payPalClientId
                if (clientId == null) {
                    val clientIdError = BraintreeException(
                        "Could not find PayPal client ID in Braintree configuration."
                    )
                    notifyFailure(error = clientIdError)
                } else {
                    val payPalMessageView = constructPayPalMessageView(context, clientId, configuration, request)
                    payPalMessageView.layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            } else {
                val unknownError = BraintreeException(
                    "Fetching Braintree configuration resulted in no error or configuration returned."
                )
                notifyFailure(error = unknownError)
            }
        }
    }

    private fun constructPayPalMessageView(
        context: Context,
        clientId: String,
        configuration: Configuration,
        request: PayPalMessagingRequest
    ): PayPalMessageView {
        val environment = if (configuration.environment == "production") {
            PayPalEnvironment.LIVE
        } else {
            PayPalEnvironment.SANDBOX
        }
        val messageData = PayPalMessageData(
            clientID = clientId,
            amount = request.amount,
            buyerCountry = request.buyerCountry,
            offerType = request.offerType?.offerTypeRawValue,
            placement = request.placement?.rawValue,
            environment = environment
        )

        val messageStyle = PayPalMessageStyle(
            color = request.color?.messageColorRawValue,
            logoType = request.logoType?.logoTypeRawValue,
            textAlign = request.textAlignment?.textAlignmentRawValue
        )

        val viewStateCallbacks = PayPalMessageViewStateCallbacks(
            onLoading = {
                payPalMessagingListener?.onPayPalMessagingLoading()
            },
            onError = { error ->
                notifyFailure(error)
            },
            onSuccess = {
                notifySuccess()
            }
        )

        val eventsCallbacks = PayPalMessageEventsCallbacks(
            onClick = {
                payPalMessagingListener?.onPayPalMessagingClick()
            },
            onApply = {
                payPalMessagingListener?.onPayPalMessagingApply()
            }
        )

        val messageConfig = PayPalMessageConfig(
            data = messageData,
            style = messageStyle,
            viewStateCallbacks = viewStateCallbacks,
            eventsCallbacks = eventsCallbacks
        )

        return PayPalMessageView(context = context, config = messageConfig)
    }

    private fun notifySuccess() {
        braintreeClient.sendAnalyticsEvent(PayPalMessagingAnalytics.SUCCEEDED.value)
        payPalMessagingListener?.onPayPalMessagingSuccess()
    }

    private fun notifyFailure(error: Exception) {
        braintreeClient.sendAnalyticsEvent(PayPalMessagingAnalytics.FAILED.value)
        payPalMessagingListener?.onPayPalMessagingFailure(error)
    }
}
