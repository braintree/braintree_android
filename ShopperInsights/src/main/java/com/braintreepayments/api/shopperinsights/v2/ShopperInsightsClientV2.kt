package com.braintreepayments.api.shopperinsights.v2

import android.content.Context
import com.braintreepayments.api.core.AnalyticsClient
import com.braintreepayments.api.core.BraintreeClient
import com.braintreepayments.api.core.ExperimentalBetaApi

/**
 * Use [ShopperInsightsClientV2] to optimize your checkout experience by prioritizing the customer’s preferred payment
 * methods in your UI.
 *
 * By customizing each customer’s checkout experience, you can improve conversion, increase sales/repeat buys and boost
 * user retention/loyalty.
 *
 * The use of this client is a completely separate integration path from the deprecated
 * [com.braintreepayments.api.shopperinsights.ShopperInsightsClient].
 *
 * Note: **This feature is in beta. It's public API may change in future releases.**
 */
@ExperimentalBetaApi
class ShopperInsightsClientV2 internal constructor(
    private val braintreeClient: BraintreeClient,
    lazyAnalyticsClient: Lazy<AnalyticsClient>
) {

    /**
     * @param context: an Android context
     * @param authorization: a Tokenization Key or Client Token used to authenticate
     */
    constructor(
        context: Context,
        authorization: String
    ) : this(
        BraintreeClient(context, authorization),
        lazyAnalyticsClient = AnalyticsClient.lazyInstance
    )

    private val analyticsClient: AnalyticsClient by lazyAnalyticsClient
}
