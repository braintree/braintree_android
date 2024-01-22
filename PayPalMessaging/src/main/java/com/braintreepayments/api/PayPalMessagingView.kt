package com.braintreepayments.api

import android.content.Context
import com.paypal.messages.PayPalMessageView
import com.paypal.messages.config.message.PayPalMessageData
import com.paypal.messages.config.PayPalEnvironment
import com.paypal.messages.config.message.PayPalMessageConfig
import com.paypal.messages.config.message.PayPalMessageEventsCallbacks
import com.paypal.messages.config.message.PayPalMessageStyle
import com.paypal.messages.config.message.PayPalMessageViewStateCallbacks

/**
 *  Use [PayPalMessagingView] to display PayPal messages to promote offers such as Pay Later and PayPal Credit to customers.
 * Note: **This module is in beta. It's public API may change or be removed in future releases.**
 * @property braintreeClient a {@link BraintreeClient}
 */
class PayPalMessagingView(
    private val braintreeClient: BraintreeClient,
    private val context: Context
) {
    var payPalMessagingListener: PayPalMessagingListener? = null

    /**
     * Creates a view to be displayed to promote offers such as Pay Later and PayPal Credit to customers.
     * @property request An optional [PayPalMessagingRequest]
     * Note: **This module is in beta. It's public API may change or be removed in future releases.**
     */
    fun start(request: PayPalMessagingRequest = PayPalMessagingRequest()) {
        braintreeClient.getConfiguration { configuration, configError ->
            if (configError != null) {
                payPalMessagingListener?.onFailure(configError)
            } else if (configuration != null) {
                val clientId = configuration.payPalClientId
                if (clientId == null) {
                    // TODO: return null client ID error here
                } else {
                    val messageView = constructMessageView(clientId, configuration, request)
                }
            } else {
                // TODO: return unknown error
            }
        }
    }

    private fun constructMessageView(
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
            offerType = request.offerType.offerTypeRawValue,
            placement = request.placement.placementRawValue,
            environment = environment
        )

        val messageStyle = PayPalMessageStyle(
            color = request.color.messageColorRawValue,
            logoType = request.logoType.logoTypeRawValue,
            textAlign = request.textAlignment.textAlignmentRawValue
        )

        val viewStateCallbacks = PayPalMessageViewStateCallbacks(
            onLoading = {
                payPalMessagingListener?.onLoading()
            },
            onError = { error ->
                payPalMessagingListener?.onFailure(error)
            },
            onSuccess = {
                payPalMessagingListener?.onSuccess()
            }
        )

        val eventsCallbacks = PayPalMessageEventsCallbacks(
            onClick = {
                payPalMessagingListener?.onClick()
            },
            onApply = {
                payPalMessagingListener?.onApply()
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
}
