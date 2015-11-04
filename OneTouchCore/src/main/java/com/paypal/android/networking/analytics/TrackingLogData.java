package com.paypal.android.networking.analytics;

import java.util.Map;

public class TrackingLogData {

    public final String mTrackingToken;
    public final Map<String, String> mParams;

    public TrackingLogData(String trackingToken, Map<String, String> params) {
        this.mParams = params;
        this.mTrackingToken = trackingToken;
    }
}
