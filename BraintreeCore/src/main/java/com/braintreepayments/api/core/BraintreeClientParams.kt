package com.braintreepayments.api.core

import android.content.Context
import android.net.Uri
import com.braintreepayments.api.BrowserSwitchClient
import com.braintreepayments.api.core.IntegrationType.Integration
import com.braintreepayments.api.sharedutils.ManifestValidator

internal data class BraintreeClientParams(
    val context: Context,
    val sessionId: String,
    val authorization: Authorization,
    val returnUrlScheme: String,
    val appLinkReturnUri: Uri?,
    val httpClient: BraintreeHttpClient = BraintreeHttpClient(),
    val graphQLClient: BraintreeGraphQLClient = BraintreeGraphQLClient(),
    val analyticsClient: AnalyticsClient = AnalyticsClient(context),
    val browserSwitchClient: BrowserSwitchClient = BrowserSwitchClient(),
    val manifestValidator: ManifestValidator = ManifestValidator(),
    val uuidHelper: UUIDHelper = UUIDHelper(),
    val configurationLoader: ConfigurationLoader = ConfigurationLoader(context, httpClient),
    @Integration val integrationType: String,
) {

    constructor(options: BraintreeOptions) : this(
        context = options.context,
        authorization = options.authorization,
        sessionId = options.sessionId ?: createUniqueSessionId(),
        returnUrlScheme = options.returnUrlScheme ?: createDefaultReturnUrlScheme(options.context),
        appLinkReturnUri = options.appLinkReturnUri,
        integrationType = options.integrationType ?: IntegrationType.CUSTOM
    )

    val applicationContext: Context = context.applicationContext
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
