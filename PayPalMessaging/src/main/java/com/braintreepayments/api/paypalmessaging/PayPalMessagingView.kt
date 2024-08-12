package com.braintreepayments.api.paypalmessaging

import android.content.Context
import android.view.ViewGroup
import android.widget.FrameLayout
import com.braintreepayments.api.core.ExperimentalBetaApi
import com.braintreepayments.api.core.BraintreeClient
import com.braintreepayments.api.core.BraintreeException
import com.braintreepayments.api.core.Configuration
import com.paypal.messages.PayPalMessageView
import com.paypal.messages.config.PayPalEnvironment
import com.paypal.messages.config.message.PayPalMessageConfig
import com.paypal.messages.config.message.PayPalMessageData
import com.paypal.messages.config.message.PayPalMessageEventsCallbacks
import com.paypal.messages.config.message.PayPalMessageStyle
import com.paypal.messages.config.message.PayPalMessageViewStateCallbacks

/**
 *  Use [PayPalMessagingView] to display PayPal messages to promote offers such as Pay Later
 *  and PayPal Credit to customers.
 * Note: **This module is in beta. It's public API may change or be removed in future releases.**
 */
@ExperimentalBetaApi
class PayPalMessagingView internal constructor(
    private val braintreeClient: BraintreeClient,
    context: Context
) : FrameLayout(context) {

    /**
     * @param context: an Android context
     * @param authorization: a Tokenization Key or Client Token used to authenticate
     */
    constructor(context: Context, authorization: String) : this(BraintreeClient(context, authorization), context)

    private var listener: PayPalMessagingListener? = null

    private var messageView: PayPalMessageView? = null

    /**
     * Add a {@link PayPalMessagingListener} to your client to receive results or errors from the PayPal Messaging flow.
     *
     * @param listener a {@link PayPalMessagingListener}
     */
     fun setListener(listener: PayPalMessagingListener) {
        this.listener = listener
     }

    /**
     * Creates a view to be displayed to promote offers such as Pay Later and PayPal Credit to customers.
     * @property request An optional [PayPalMessagingRequest]
     * Note: **This module is in beta. It's public API may change or be removed in future releases.**
     */
    fun start(request: PayPalMessagingRequest = PayPalMessagingRequest()) {
        PayPalMessageConfig.setGlobalAnalytics(
            integrationName = "BT_SDK",
            integrationVersion = BuildConfig.VERSION_NAME
        )

        braintreeClient.sendAnalyticsEvent(PayPalMessagingAnalytics.STARTED)

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

                    val messageConfig = constructPayPalMessageViewConfig(clientId, configuration, request)

                    if (messageView != null) {
                        messageView?.setConfig(messageConfig)
                    } else {
                        val payPalMessageView = PayPalMessageView(context = context, config = messageConfig)
                        payPalMessageView.layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                        )

                        addView(payPalMessageView)
                        messageView = payPalMessageView
                    }
                }
            }
        }
    }

    private fun constructPayPalMessageViewConfig(
        clientId: String,
        configuration: Configuration,
        request: PayPalMessagingRequest
    ): PayPalMessageConfig {
        val environment = if (configuration.environment == "production") {
            PayPalEnvironment.LIVE
        } else {
            PayPalEnvironment.SANDBOX
        }
        val messageData = PayPalMessageData(
            clientID = clientId,
            amount = request.amount,
            buyerCountry = request.buyerCountry,
            offerType = request.offerType?.internalValue,
            pageType = request.pageType?.internalValue,
            environment = environment
        )

        val messageStyle = PayPalMessageStyle(
            color = request.color.internalValue,
            logoType = request.logoType.internalValue,
            textAlignment = request.textAlignment.internalValue
        )

        val viewStateCallbacks = PayPalMessageViewStateCallbacks(
            onLoading = {
                listener?.onPayPalMessagingLoading()
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
                listener?.onPayPalMessagingClick()
            },
            onApply = {
                listener?.onPayPalMessagingApply()
            }
        )

        return PayPalMessageConfig(
            data = messageData,
            style = messageStyle,
            viewStateCallbacks = viewStateCallbacks,
            eventsCallbacks = eventsCallbacks
        )
    }

    private fun notifySuccess() {
        braintreeClient.sendAnalyticsEvent(PayPalMessagingAnalytics.SUCCEEDED)
        listener?.onPayPalMessagingSuccess()
    }

    private fun notifyFailure(error: Exception) {
        braintreeClient.sendAnalyticsEvent(PayPalMessagingAnalytics.FAILED)
        listener?.onPayPalMessagingFailure(error)
    }
}
