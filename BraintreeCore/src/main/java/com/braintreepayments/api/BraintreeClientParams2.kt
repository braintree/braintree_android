package com.braintreepayments.api

import android.content.Context
import androidx.annotation.VisibleForTesting
import com.braintreepayments.api.IntegrationType.Integration

internal data class BraintreeClientParams2 @VisibleForTesting constructor(
    val context: Context,
    val sessionId: String,
    val authorizationLoader: AuthorizationLoader,
    val returnUrlScheme: String,
    val httpClient: BraintreeHttpClient = BraintreeHttpClient(),
    val graphQLClient: BraintreeGraphQLClient = BraintreeGraphQLClient(),
    val analyticsClient: AnalyticsClient = AnalyticsClient(context),
    val browserSwitchClient: BrowserSwitchClient = BrowserSwitchClient(),
    val manifestValidator: ManifestValidator = ManifestValidator(),
    val uuidHelper: UUIDHelper = UUIDHelper(),
    val configurationLoader: ConfigurationLoader = ConfigurationLoader(context, httpClient),
    @Integration val integrationType: String = IntegrationType.CUSTOM,
) {

    constructor(options: BraintreeOptions) : this(
        context = options.context,
        authorizationLoader = options.run {
            AuthorizationLoader(initialAuthString, clientTokenProvider)
        },
        sessionId = options.sessionId,
        returnUrlScheme = options.returnUrlScheme
    )

    val applicationContext: Context = context.applicationContext
    val braintreeReturnUrlScheme =
        "${getAppPackageNameWithoutUnderscores(context)}.braintree.deeplinkhandler"

    companion object {

        private fun getAppPackageNameWithoutUnderscores(context: Context) =
            context.applicationContext.packageName.replace("_", "")
    }
}
