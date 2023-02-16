package com.braintreepayments.api

import android.content.Context

data class BraintreeOptions(
    val context: Context,
    val sessionId: String = createUniqueSessionId(),
    val returnUrlScheme: String = createDefaultReturnUrlScheme(context),
    val initialAuthString: String? = null,
    val clientTokenProvider: ClientTokenProvider? = null,
    @IntegrationType.Integration val integrationType: String = IntegrationType.CUSTOM,
) {
    companion object {
        private fun createUniqueSessionId() = UUIDHelper().formattedUUID

        private fun getAppPackageNameWithoutUnderscores(context: Context) =
            context.applicationContext.packageName.replace("_", "")

        private fun createDefaultReturnUrlScheme(context: Context) =
            "${getAppPackageNameWithoutUnderscores(context)}.braintree"
    }
}
