package com.braintreepayments.api

internal class AuthorizationLoader(
    initialAuthString: String?,
    private val clientTokenProvider: ClientTokenProvider?
) {
    // cache initial auth if available
    var authorizationFromCache = initialAuthString?.let { Authorization.fromString(it) }

    fun loadAuthorization(callback: AuthorizationCallback) {
        if (authorizationFromCache != null) {
            callback.onAuthorizationResult(authorizationFromCache, null)
        } else if (clientTokenProvider != null) {
            clientTokenProvider.getClientToken(object : ClientTokenCallback {
                override fun onSuccess(clientToken: String) {
                    authorizationFromCache = Authorization.fromString(clientToken)
                    callback.onAuthorizationResult(authorizationFromCache, null)
                }

                override fun onFailure(error: Exception) {
                    callback.onAuthorizationResult(null, error)
                }
            })
        } else {
            val clientSDKSetupURL =
                "https://developer.paypal.com/braintree/docs/guides/client-sdk/setup/android/v4#initialization"
            val message = "Authorization required. See $clientSDKSetupURL for more info."
            callback.onAuthorizationResult(null, BraintreeException(message))
        }
    }

    fun invalidateClientToken() {
        // only invalidate client token cache if we can fetch a new one with a client token provider
        if (clientTokenProvider != null) {
            authorizationFromCache = null
        }
    }
}
