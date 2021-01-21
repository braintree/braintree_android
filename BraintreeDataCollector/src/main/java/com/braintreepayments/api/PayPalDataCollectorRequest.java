package com.braintreepayments.api;

import java.util.HashMap;

import androidx.annotation.NonNull;

/**
 * Used to configuration the PayPalDataCollector request
 */
class PayPalDataCollectorRequest {

    private String mApplicationGuid;
    private String mClientMetadataId;
    private boolean mDisableBeacon;
    private HashMap<String,String> mAdditionalData;

    /**
     * @param additionalData Additional data that should be associated with the data collection.
     */
    PayPalDataCollectorRequest setAdditionalData(HashMap<String, String> additionalData) {
        mAdditionalData = additionalData;

        return this;
    }

    /**
     * @param applicationGuid The application global unique identifier.
     *                        There is a 36 character length limit on this value.
     */
    PayPalDataCollectorRequest setApplicationGuid(String applicationGuid) {
        this.mApplicationGuid = applicationGuid;

        return this;
    }

    /**
     * @param clientMetadataId The desired pairing ID, trimmed to 32 characters.
     */
    PayPalDataCollectorRequest setClientMetadataId(@NonNull String clientMetadataId) {
        this.mClientMetadataId = clientMetadataId.substring(0, Math.min(clientMetadataId.length(), 32));

        return this;
    }

    /**
     * @param disableBeacon Indicates if the beacon feature should be disabled.
     */
    PayPalDataCollectorRequest setDisableBeacon(boolean disableBeacon) {
        mDisableBeacon = disableBeacon;

        return this;
    }

    HashMap<String, String> getAdditionalData() {
        return mAdditionalData;
    }

    String getApplicationGuid() {
        return mApplicationGuid;
    }

    String getClientMetadataId() {
        return mClientMetadataId;
    }

    boolean isDisableBeacon() {
        return mDisableBeacon;
    }
}
