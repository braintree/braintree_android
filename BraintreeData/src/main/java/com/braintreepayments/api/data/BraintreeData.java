package com.braintreepayments.api.data;

import android.app.Activity;
import android.content.Context;

import com.devicecollector.DeviceCollector;

import java.lang.reflect.Method;
import java.util.UUID;

/**
 * BraintreeData is used to collect device information to aid fraud detection and prevention.
 */
public final class BraintreeData {

    private String mFraudMerchantId;
    private String mDeviceSessionId;
    private String mCorrelationId;
    private DeviceCollector mDeviceCollector;

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
        mCorrelationId = getCorrelationId(activity);
        mDeviceCollector = new DeviceCollector(activity);
        mDeviceCollector.setMerchantId(mFraudMerchantId);
        mDeviceCollector.setCollectorUrl(collectorUrl);
    }

    /**
     * Call to get device_data to send to Braintree
     * @return String device_data JSON-encoded String of device data.
     */
    public String collectDeviceData() {
        if(mDeviceSessionId == null) {
            mDeviceSessionId = UUID.randomUUID().toString().replace("-", "");
            mDeviceCollector.collect(mDeviceSessionId);
        }

        String data = "{\"device_session_id\":\"" + mDeviceSessionId + "\"," +
                "\"fraud_merchant_id\":\"" + mFraudMerchantId + "\"";
        if (mCorrelationId != null) {
            data += ",\"correlation_id\":\"" + mCorrelationId + "\"}";
        } else {
            data += "}";
        }
        return data;
    }

    private String getCorrelationId(Activity activity) {
        try {
            Method method = getClass().getClassLoader()
                    .loadClass("com.paypal.android.sdk.onetouch.core.PayPalOneTouchCore")
                    .getMethod("getClientMetadataId", Context.class);
            return (String) method.invoke(null, activity);
        } catch (Exception ignored) {
            return null;
        }
    }

}