package com.braintreepayments.api;

import java.util.HashMap;

import androidx.annotation.NonNull;

/**
 * Used to configuration the PayPalDataCollector request
 */
class PayPalDataCollectorInternalRequest {

    private String applicationGuid;
    private String clientMetadataId;
    private boolean disableBeacon;
    final private boolean hasUserLocationConsent;
    private HashMap<String,String> additionalData;

    PayPalDataCollectorInternalRequest(boolean hasUserLocationConsent) {
        this.hasUserLocationConsent = hasUserLocationConsent;
    }

    /**
     * @param additionalData Additional data that should be associated with the data collection.
     */
    PayPalDataCollectorInternalRequest setAdditionalData(HashMap<String, String> additionalData) {
        this.additionalData = additionalData;

        return this;
    }

    /**
     * @param applicationGuid The application global unique identifier.
     *                        There is a 36 character length limit on this value.
     */
    PayPalDataCollectorInternalRequest setApplicationGuid(String applicationGuid) {
        this.applicationGuid = applicationGuid;

        return this;
    }

    /**
     * @param riskCorrelationId The desired pairing ID, trimmed to 32 characters.
     */
    PayPalDataCollectorInternalRequest setRiskCorrelationId(@NonNull String riskCorrelationId) {
        this.clientMetadataId = riskCorrelationId.substring(0, Math.min(riskCorrelationId.length(), 32));

        return this;
    }

    /**
     * @param disableBeacon Indicates if the beacon feature should be disabled.
     */
    PayPalDataCollectorInternalRequest setDisableBeacon(boolean disableBeacon) {
        this.disableBeacon = disableBeacon;

        return this;
    }

    HashMap<String, String> getAdditionalData() {
        return additionalData;
    }

    String getApplicationGuid() {
        return applicationGuid;
    }

    String getClientMetadataId() {
        return clientMetadataId;
    }

    public boolean getHasUserLocationConsent() {
        return hasUserLocationConsent;
    }

    boolean isDisableBeacon() {
        return disableBeacon;
    }
}
