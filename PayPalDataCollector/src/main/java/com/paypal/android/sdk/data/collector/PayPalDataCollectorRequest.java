package com.paypal.android.sdk.data.collector;

/**
 * Used to configuration the PayPalDataCollector request
 */
public class PayPalDataCollectorRequest {

    private String mApplicationGuid;
    private String mClientMetadataId;

    /**
     * @param applicationGuid The application global unique identifier.
     *                        There is a 36 character length limit on this value.
     */
    public PayPalDataCollectorRequest setApplicationGuid(String applicationGuid) {
        this.mApplicationGuid = applicationGuid;

        return this;
    }

    /**
     * @param clientMetadataId The desired pairing ID.
     */
    public PayPalDataCollectorRequest setClientMetadataId(String clientMetadataId) {
        this.mClientMetadataId = clientMetadataId;

        return this;
    }

    public String getApplicationGuid() {
        return mApplicationGuid;
    }

    public String getClientMetadataId() {
        return mClientMetadataId;
    }
}
