package com.braintreepayments.api.core

import androidx.annotation.RestrictTo

/**
 * This class is responsible for holding parameters that are sent with analytic events.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class AnalyticsParamRepository(
    private val uuidHelper: UUIDHelper = UUIDHelper()
) {

    /**
     * Link type used for navigating back to the merchant app. See [LinkType].
     */
    var linkType: LinkType? = null

    /**
     * App switch enabled by the merchant request
     */
    var didEnablePayPalAppSwitch: Boolean? = null

    /**
     * Whether or not app-switch URL was received from the PayPal server response
     */
    var didPayPalServerAttemptAppSwitch: Boolean? = null

    /**
     * Whether or not the SDK attempted to perform an app switch based on whether the PayPal app
     * resolved the app-switch URI.
     */
    var didSdkAttemptAppSwitch: Boolean? = null

    /**
     * Value to determine whether PayPal, PayLater or PayPal Credit was clicked.
     */
    var fundingSource: String? = null

    /**
     * Whether a purchase is part of the payment flow.
     */
    var isPurchaseFlow: Boolean? = null

    /**
     * Whether or not billing agreement will be created - customer opted to save
     * PayPal for future purchases and a vaulted billing agreement was created with the charge.
     */
    var shouldRequestBillingAgreement: Boolean? = null

    /**
     * Recurring billing plan type, or charge pattern.
     */
    var recurringBillingPlanType: String? = null

    /**
     * Session ID to tie analytics events together which is used for reporting conversion funnels.
     */
    val sessionId: String = uuidHelper.formattedUUID

    /**
     * Clears all repository values.
     *
     * Note that this function is called in different spots of the SDK lifecycle for different payment modules. Some
     * modules call reset during launch of the SDK. The PayPal module calls reset at the end of the payment flow to
     * persist the [sessionId] value set from the Shopper Insights module.
     */
    fun reset() {
        linkType = null
        didEnablePayPalAppSwitch = null
        didPayPalServerAttemptAppSwitch = null
        didSdkAttemptAppSwitch = null
        fundingSource = null
        isPurchaseFlow = null
        shouldRequestBillingAgreement = null
        recurringBillingPlanType = null
    }

    companion object {

        /**
         * Singleton instance of the AnalyticsParamRepository.
         */
        val instance: AnalyticsParamRepository by lazy { AnalyticsParamRepository() }
    }
}
