package com.braintreepayments.api.models;

public class Metadata {

    private String integration;
    private String source;

    public Metadata(String integration, String source) {
        this.integration = integration;
        this.source = source;
    }

}
