package com.braintreepayments.api;

public class BTHTTPResponse {
    long startTime;
    long endTime;
    String body;

    public BTHTTPResponse(long startTime, long endTime, String body) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.body = body;
    }
}