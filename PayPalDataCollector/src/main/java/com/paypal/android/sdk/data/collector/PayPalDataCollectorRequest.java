package com.paypal.android.sdk.data.collector;

import android.support.annotation.NonNull;

import java.util.HashMap;

/**
 * Used to configuration the PayPalDataCollector request
 */
public class PayPalDataCollectorRequest {

    private String mApplicationGuid;
    private String mClientMetadataId;
    private boolean mDisableBeacon;
    private HashMap<String,String> mAdditionalData;

    /**
     * @param additionalData Additional data that should be associated with the data collection.
     */
    public PayPalDataCollectorRequest setAdditionalData(HashMap<String, String> additionalData) {
        mAdditionalData = additionalData;

        return this;
    }

    /**
     * @param applicationGuid The application global unique identifier.
     *                        There is a 36 character length limit on this value.
     */
    public PayPalDataCollectorRequest setApplicationGuid(String applicationGuid) {
        this.mApplicationGuid = applicationGuid;

        return this;
    }

    /**
     * @param clientMetadataId The desired pairing ID, trimmed to 32 characters.
     */
    public PayPalDataCollectorRequest setClientMetadataId(@NonNull String clientMetadataId) {
        this.mClientMetadataId = clientMetadataId.substring(0, Math.min(clientMetadataId.length(), 32));;

        return this;
    }

    /**
     * @param disableBeacon Indicates if the beacon feature should be disabled.
     */
    public PayPalDataCollectorRequest setDisableBeacon(boolean disableBeacon) {
        mDisableBeacon = disableBeacon;

        return this;
    }

    public HashMap<String, String> getAdditionalData() {
        return mAdditionalData;
    }

    public String getApplicationGuid() {
        return mApplicationGuid;
    }

    public String getClientMetadataId() {
        return mClientMetadataId;
    }

    public boolean isDisableBeacon() {
        return mDisableBeacon;
    }
}
