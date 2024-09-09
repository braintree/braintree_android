package com.braintreepayments.api.paypalmessaging

import com.braintreepayments.api.core.ExperimentalBetaApi

/**
 * Callback used to communicate [PayPalMessagingView] events
 * Note: **This module is in beta. It's public API may change or be removed in future releases.**
 */
@ExperimentalBetaApi
interface PayPalMessagingListener {

    /**
     * Called when the user has clicked the [PayPalMessagingView].
     */
    fun onPayPalMessagingClick()

    /**
     * Called when the user has begun the PayPal Credit application.
     */
    fun onPayPalMessagingApply()

    /**
     * Called when the [PayPalMessagingView] is loading.
     */
    fun onPayPalMessagingLoading()

    /**
     * Called when the [PayPalMessagingView] has rendered successfully.
     */
    fun onPayPalMessagingSuccess()

    /**
     * Called when there is a failure rendering the [PayPalMessagingView]
     * @param error [Exception] explaining the reason for failure.
     */
    fun onPayPalMessagingFailure(error: Exception)
}
