package com.braintreepayments.api.shopperinsights.v2

import android.content.Context
import com.braintreepayments.api.core.AnalyticsClient
import com.braintreepayments.api.core.AnalyticsEventParams
import com.braintreepayments.api.core.BraintreeClient
import com.braintreepayments.api.core.DeviceInspector
import com.braintreepayments.api.core.ExperimentalBetaApi
import com.braintreepayments.api.shopperinsights.ButtonType
import com.braintreepayments.api.shopperinsights.PresentmentDetails
import com.braintreepayments.api.shopperinsights.ShopperInsightsAnalytics.BUTTON_PRESENTED
import com.braintreepayments.api.shopperinsights.ShopperInsightsAnalytics.BUTTON_SELECTED
import com.braintreepayments.api.shopperinsights.v2.internal.CreateCustomerSessionApi
import com.braintreepayments.api.shopperinsights.v2.internal.UpdateCustomerSessionApi

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
    private val createCustomerSessionApi: CreateCustomerSessionApi = CreateCustomerSessionApi(braintreeClient),
    private val updateCustomerSessionApi: UpdateCustomerSessionApi = UpdateCustomerSessionApi(braintreeClient),
    private val deviceInspector: DeviceInspector = DeviceInspector(),
    lazyAnalyticsClient: Lazy<AnalyticsClient> = AnalyticsClient.lazyInstance
) {

    /**
     * @param context: an Android context
     * @param authorization: a Tokenization Key or Client Token used to authenticate
     */
    constructor(
        context: Context,
        authorization: String
    ) : this(
        BraintreeClient(context, authorization)
    )

    private val analyticsClient: AnalyticsClient by lazyAnalyticsClient

    /**
     * Creates a new customer session.
     *
     * @param customerSessionRequest: a [CustomerSessionRequest] object containing the request parameters
     * @param customerSessionCallback: a callback that returns the result of the customer session creation
     *
     * Note: **This feature is in beta. It's public API may change in future releases.**
     */
    fun createCustomerSession(
        customerSessionRequest: CustomerSessionRequest,
        customerSessionCallback: (customerSessionResult: CustomerSessionResult) -> Unit
    ) {
        createCustomerSessionApi.execute(customerSessionRequest) { createCustomerSessionResult ->
            when (createCustomerSessionResult) {
                is CreateCustomerSessionApi.CreateCustomerSessionResult.Success -> {
                    customerSessionCallback(CustomerSessionResult.Success(createCustomerSessionResult.sessionId))
                }

                is CreateCustomerSessionApi.CreateCustomerSessionResult.Error -> {
                    customerSessionCallback(CustomerSessionResult.Failure(createCustomerSessionResult.error))
                }
            }
        }
    }

    /**
     * Updates an existing customer session.
     *
     * @param customerSessionRequest: a [CustomerSessionRequest] object containing the request parameters
     * @param sessionId: the ID of the session to update
     * @param customerSessionCallback: a callback that returns the result of the customer session update
     *
     * Note: **This feature is in beta. Its public API may change in future releases.**
     */
    fun updateCustomerSession(
        customerSessionRequest: CustomerSessionRequest,
        sessionId: String,
        customerSessionCallback: (customerSessionResult: CustomerSessionResult) -> Unit
    ) {
        updateCustomerSessionApi.execute(customerSessionRequest, sessionId) { result ->
            when (result) {
                is UpdateCustomerSessionApi.UpdateCustomerSessionResult.Success ->
                    customerSessionCallback(CustomerSessionResult.Success(result.sessionId))
                is UpdateCustomerSessionApi.UpdateCustomerSessionResult.Error ->
                    customerSessionCallback(CustomerSessionResult.Failure(result.error))
            }
        }
    }

    /**
     * Call this method when the PayPal, Venmo or Other button has been successfully displayed to the buyer.
     * This method sends analytics to help improve the Shopper Insights feature experience.
     * @param buttonType Type of button presented - PayPal, Venmo, or Other.
     * @param presentmentDetails Detailed information, including button order, experiment type, and page type about the
     * payment button that is sent to analytics to help improve the Shopper Insights feature experience.
     * @param sessionId The shopper session ID
     */
    fun sendPresentedEvent(
        buttonType: ButtonType,
        presentmentDetails: PresentmentDetails,
        sessionId: String,
    ) {
        val params = AnalyticsEventParams(
            experiment = presentmentDetails.type.formattedExperiment(),
            shopperSessionId = sessionId,
            buttonType = buttonType.stringValue,
            buttonOrder = presentmentDetails.buttonOrder.stringValue,
            pageType = presentmentDetails.pageType.stringValue
        )
        analyticsClient.sendEvent(BUTTON_PRESENTED, params)
    }

    /**
     * Call this method when the PayPal, Venmo or Other button has been selected/tapped by the buyer.
     * This method sends analytics to help improve the Shopper Insights feature experience.
     * @param buttonType Type of button presented - PayPal, Venmo, or Other.
     * @param sessionId The shopper session ID
     */
    fun sendSelectedEvent(
        buttonType: ButtonType,
        sessionId: String,
    ) {
        val params = AnalyticsEventParams(
            shopperSessionId = sessionId,
            buttonType = buttonType.stringValue,
        )
        analyticsClient.sendEvent(BUTTON_SELECTED, params)
    }

    /**
     * Indicates whether the PayPal App is installed.
     */
    fun isPayPalAppInstalled(context: Context): Boolean {
        return deviceInspector.isPayPalInstalled(context)
    }

    /**
     * Indicates whether the Venmo App is installed.
     */
    fun isVenmoAppInstalled(context: Context): Boolean {
        return deviceInspector.isVenmoInstalled(context)
    }
}
