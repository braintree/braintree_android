package com.braintreepayments.api.datacollector;

import java.util.HashMap;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

/**
 * Used to configuration the PayPalDataCollector request
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class DataCollectorRequest {

    private String applicationGuid;
    private String clientMetadataId;
    private boolean disableBeacon;
    private HashMap<String,String> additionalData;

    /**
     * @param additionalData Additional data that should be associated with the data collection.
     */
    DataCollectorRequest setAdditionalData(HashMap<String, String> additionalData) {
        this.additionalData = additionalData;

        return this;
    }

    /**
     * @param applicationGuid The application global unique identifier.
     *                        There is a 36 character length limit on this value.
     */
    DataCollectorRequest setApplicationGuid(String applicationGuid) {
        this.applicationGuid = applicationGuid;

        return this;
    }

    /**
     * @param riskCorrelationId The desired pairing ID, trimmed to 32 characters.
     */
    DataCollectorRequest setRiskCorrelationId(@NonNull String riskCorrelationId) {
        this.clientMetadataId = riskCorrelationId.substring(0, Math.min(riskCorrelationId.length(), 32));

        return this;
    }

    /**
     * @param disableBeacon Indicates if the beacon feature should be disabled.
     */
    DataCollectorRequest setDisableBeacon(boolean disableBeacon) {
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

    boolean isDisableBeacon() {
        return disableBeacon;
    }
}
