package com.braintreepayments.api.internal;

public class AnalyticsManagerTestUtils {

    public static void setHttpClient(BraintreeHttpClient httpClient) {
        AnalyticsManager.sHttpClient = httpClient;
    }
}
