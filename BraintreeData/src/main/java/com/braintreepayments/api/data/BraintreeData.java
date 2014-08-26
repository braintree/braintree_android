package com.braintreepayments.api.data;

import android.app.Activity;

import com.devicecollector.DeviceCollector;

import java.util.UUID;

/**
 * BraintreeData is used to collect device information to aid fraud detection and prevention.
 */
public final class BraintreeData {

    private String mFraudMerchantId;
    private String mDeviceSessionId;
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
     * @param fraudMerchantId The fraud merchant id from Braintree.
     * @param collectorUrl The fraud collector url from Braintree.
     */
    public BraintreeData(Activity activity, String fraudMerchantId, String collectorUrl) {
        mFraudMerchantId = fraudMerchantId;

        deviceCollector = new DeviceCollector(activity);
        deviceCollector.setMerchantId(mFraudMerchantId);
        deviceCollector.setCollectorUrl(collectorUrl);
    }

    /**
     * Call to get device_data to send to Braintree
     * @return String device_data JSON-encoded String of device data.
     */
    public String collectDeviceData() {
        if(mDeviceSessionId == null) {
            mDeviceSessionId = UUID.randomUUID().toString().replace("-", "");
            deviceCollector.collect(mDeviceSessionId);
        }

        return "{\"device_session_id\": \"" + mDeviceSessionId
                + "\", \"fraud_merchant_id\": \"" + mFraudMerchantId + "\"}";
    }

}