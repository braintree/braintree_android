package com.braintreepayments.api.data;

import android.app.Activity;

import com.devicecollector.DeviceCollector;

import java.util.UUID;

/**
 * BraintreeData is used to collect device information to aid fraud detection and prevention.
 */
public final class BraintreeData {

    private final String sessionId;
    private DeviceCollector deviceCollector;

    /**
     * Creates a new BraintreeData instance for fraud detection.
     *
     * @param activity The currently visible activity.
     * @param environment The Braintree environment to use.
     */
    public BraintreeData(Activity activity, BraintreeEnvironment environment) {
        this(activity, environment.getMerchantId(), environment.getCollectorUrl());
    }

    /**
     * Creates a new BraintreeData instance for fraud detection. This should be used in conjunction
     * with a non-aggregate fraud id.
     *
     * @param activity The currently visible activity.
     * @param merchantId The fraud merchant id from Braintree.
     * @param collectorUrl The fraud collector url from Braintree.
     */
    public BraintreeData(Activity activity, String merchantId, String collectorUrl) {
        sessionId = UUID.randomUUID().toString().replace("-", "");

        deviceCollector = new DeviceCollector(activity);
        deviceCollector.setMerchantId(merchantId);
        deviceCollector.setCollectorUrl(collectorUrl);
    }

    /**
     * Call to get a device_id to send to Braintree
     * @return device_id String
     */
    public String collectDeviceData() {
        deviceCollector.collect(sessionId);
        return sessionId;
    }

}