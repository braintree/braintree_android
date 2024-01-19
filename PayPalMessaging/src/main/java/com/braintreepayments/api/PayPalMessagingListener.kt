package com.braintreepayments.api

/**
 * Callback used to communicate [PayPalMessagingView] events
 * Note: **This module is in beta. It's public API may change or be removed in future releases.**
 */
interface PayPalMessagingListener {

    /**
     * Called when the user has clicked the [PayPalMessagingView].
     */
    fun onClick()

    /**
     * Called when the user has begun the PayPal Credit application.
     */
    fun onApply()

    /**
     * Called when the [PayPalMessagingView] is loading.
     */
    fun onLoading()

    /**
     * Called when the [PayPalMessagingView] has rendered successfully.
     */
    fun onSuccess()

    /**
     * Called when there is a failure rendering the [PayPalMessagingView]
     * @param error [Exception] explaining the reason for failure.
     */
    fun onFailure(error: Exception)
}
