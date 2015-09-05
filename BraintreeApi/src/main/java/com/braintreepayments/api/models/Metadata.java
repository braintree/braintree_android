package com.braintreepayments.api.models;

import com.google.gson.annotations.SerializedName;

public class Metadata {

    @SerializedName("integration") private String mIntegration;
    @SerializedName("source") private String mSource;

    public Metadata(String integration, String source) {
        mIntegration = integration;
        mSource = source;
    }

}
