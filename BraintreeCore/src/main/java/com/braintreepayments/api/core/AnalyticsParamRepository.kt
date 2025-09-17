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
     * Whether or not the SDK attempted to perform an app switch.
     */

    var didSdkAttemptAppSwitch: Boolean? = null

    private lateinit var _sessionId: String

    /**
     * Session ID to tie analytics events together which is used for reporting conversion funnels.
     */
    val sessionId: String
        get() {
            if (!this::_sessionId.isInitialized) {
                _sessionId = uuidHelper.formattedUUID
            }
            return _sessionId
        }

    /**
     * Resets the [sessionId] and clears all other repository values.
     *
     * Note that this function is called in different spots of the SDK lifecycle for different payment modules. Some
     * modules call reset during launch of the SDK. The PayPal module calls reset at the end of the payment flow to
     * persist the [sessionId] value set from the Shopper Insights module.
     */
    fun reset() {
        _sessionId = uuidHelper.formattedUUID
        linkType = null
        didEnablePayPalAppSwitch = null
        didPayPalServerAttemptAppSwitch = null
        didSdkAttemptAppSwitch = null
    }

    companion object {

        /**
         * Singleton instance of the AnalyticsParamRepository.
         */
        val instance: AnalyticsParamRepository by lazy { AnalyticsParamRepository() }
    }
}
