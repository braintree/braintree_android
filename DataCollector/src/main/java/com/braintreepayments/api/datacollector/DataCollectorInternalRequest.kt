package com.braintreepayments.api.datacollector

import androidx.annotation.RestrictTo

/**
 * Used to configuration the PayPalDataCollector request
 *
 * @property hasUserLocationConsent Whether the request has user location consent
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class DataCollectorInternalRequest(val hasUserLocationConsent: Boolean) {

    /**
     * @Property additionalData Additional data that should be associated with the data collection.
     */
    var additionalData: HashMap<String, String>? = null

    /**
     * @Property applicationGuid The application global unique identifier.
     * There is a 36 character length limit on this value.
     */
    var applicationGuid: String? = null

    /**
     * @Property riskCorrelationId The desired pairing ID, trimmed to 32 characters.
     */
    var clientMetadataId: String? = null
        set(value) {
            field = value?.let {
                if (it.length > RISK_CORRELATION_ID_MAX_LENGTH) {
                    it.substring(0, RISK_CORRELATION_ID_MAX_LENGTH)
                } else {
                    it.substring(0, it.length)
                }
            }
        }

    /**
     * @Property disableBeacon Indicates if the beacon feature should be disabled.
     */
    var isDisableBeacon: Boolean = false

    companion object {
        private const val RISK_CORRELATION_ID_MAX_LENGTH = 32
    }
}
