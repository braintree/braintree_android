package com.braintreepayments.api

import android.content.Context
import com.braintreepayments.api.IntegrationType.Integration

internal data class BraintreeClientParams2(
    val context: Context,
    val sessionId: String = createUniqueSessionId(),
    val initialAuthString: String? = null,
    val clientTokenProvider: ClientTokenProvider? = null,
    val returnUrlScheme: String = createDefaultReturnUrlScheme(context),
    val httpClient: BraintreeHttpClient = BraintreeHttpClient(),
    val graphQLClient: BraintreeGraphQLClient = BraintreeGraphQLClient(),
    val analyticsClient: AnalyticsClient = AnalyticsClient(context),
    val browserSwitchClient: BrowserSwitchClient = BrowserSwitchClient(),
    val manifestValidator: ManifestValidator = ManifestValidator(),
    val uuidHelper: UUIDHelper = UUIDHelper(),
    val configurationLoader: ConfigurationLoader = ConfigurationLoader(context, httpClient),
    @Integration val integrationType: String = IntegrationType.CUSTOM,
) {
    val applicationContext: Context = context.applicationContext
    val authorizationLoader = AuthorizationLoader(initialAuthString, clientTokenProvider)

    val braintreeReturnUrlScheme =
        "${getAppPackageNameWithoutUnderscores(context)}.braintree.deeplinkhandler"

    companion object {
        private fun createUniqueSessionId() = UUIDHelper().formattedUUID

        private fun getAppPackageNameWithoutUnderscores(context: Context) =
            context.applicationContext.packageName.replace("_", "")

        private fun createDefaultReturnUrlScheme(context: Context) =
            "${getAppPackageNameWithoutUnderscores(context)}.braintree"
    }
}
