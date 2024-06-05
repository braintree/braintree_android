package com.braintreepayments.api;

public class BTHttpResponse {
    long startTime;
    long endTime;
    String body;

    public BTHttpResponse(long startTime, long endTime, String body) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.body = body;
    }
}