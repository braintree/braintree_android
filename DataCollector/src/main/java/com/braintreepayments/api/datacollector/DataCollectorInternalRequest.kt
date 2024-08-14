package com.braintreepayments.api.datacollector

import androidx.annotation.RestrictTo
import kotlin.math.min

/**
 * Used to configuration the PayPalDataCollector request
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class DataCollectorInternalRequest(val hasUserLocationConsent: Boolean) {
    var applicationGuid: String? = null
        private set
    var clientMetadataId: String? = null
        private set
    var isDisableBeacon: Boolean = false
        private set
    var additionalData: HashMap<String, String>? = null
        private set

    /**
     * @param additionalData Additional data that should be associated with the data collection.
     */
    fun setAdditionalData(additionalData: HashMap<String, String>?): DataCollectorInternalRequest {
        this.additionalData = additionalData

        return this
    }

    /**
     * @param applicationGuid The application global unique identifier.
     * There is a 36 character length limit on this value.
     */
    fun setApplicationGuid(applicationGuid: String?): DataCollectorInternalRequest {
        this.applicationGuid = applicationGuid

        return this
    }

    /**
     * @param riskCorrelationId The desired pairing ID, trimmed to 32 characters.
     */
    fun setRiskCorrelationId(riskCorrelationId: String): DataCollectorInternalRequest {
        this.clientMetadataId = riskCorrelationId.substring(
            0, min(riskCorrelationId.length, RISK_CORRELATION_ID_MAX_LENGTH)
        )

        return this
    }

    /**
     * @param disableBeacon Indicates if the beacon feature should be disabled.
     */
    fun setDisableBeacon(disableBeacon: Boolean): DataCollectorInternalRequest {
        this.isDisableBeacon = disableBeacon

        return this
    }

    companion object {
        private const val RISK_CORRELATION_ID_MAX_LENGTH = 32
    }
}
