package com.braintreepayments.api.models;

import com.google.gson.annotations.SerializedName;

/**
 * Contains configuration for Braintree analytics calls
 */
public class AnalyticsConfiguration {

    @SerializedName("url") private String mUrl;

    /**
     * @return the {@link java.lang.String} url of the Braintree analytics service.
     */
    public String getUrl() {
        return mUrl;
    }
}
