package com.braintreepayments.api.core

import android.content.Context
import android.net.Uri
import com.braintreepayments.api.BrowserSwitchClient
import com.braintreepayments.api.sharedutils.ManifestValidator
import com.braintreepayments.api.sharedutils.Scheduler
import com.braintreepayments.api.sharedutils.ThreadScheduler

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
    val integrationType: IntegrationType,
    val threadScheduler: Scheduler = ThreadScheduler(SERIAL_DISPATCH_QUEUE_POOL_SIZE)
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
        // NOTE: a single thread pool makes the ThreadScheduler behave like a serial dispatch queue
        const val SERIAL_DISPATCH_QUEUE_POOL_SIZE = 1

        private fun createUniqueSessionId() = UUIDHelper().formattedUUID

        private fun getAppPackageNameWithoutUnderscores(context: Context) =
            context.applicationContext.packageName.replace("_", "")

        private fun createDefaultReturnUrlScheme(context: Context) =
            "${getAppPackageNameWithoutUnderscores(context)}.braintree"
    }
}
