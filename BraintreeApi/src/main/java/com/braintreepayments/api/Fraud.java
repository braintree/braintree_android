package com.braintreepayments.api;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.VisibleForTesting;

import com.devicecollector.DeviceCollector;
import com.devicecollector.DeviceCollector.ErrorCode;
import com.devicecollector.DeviceCollector.StatusListener;
import com.paypal.android.sdk.onetouch.core.PayPalOneTouchCore;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.UUID;

/**
 * BraintreeData is used to collect device information to aid fraud detection and prevention.
 */
public class Fraud {

    private static final String DEVICE_SESSION_ID_KEY = "device_session_id";
    private static final String FRAUD_MERCHANT_ID_KEY = "fraud_merchant_id";
    private static final String CORRELATION_ID_KEY = "correlation_id";

    @VisibleForTesting
    protected enum BraintreeEnvironment {
        SANDBOX("https://assets.braintreegateway.com/sandbox/data/logo.htm"),
        PRODUCTION("https://assets.braintreegateway.com/data/logo.htm");

        private static final String BRAINTREE_MERCHANT_ID = "600000";

        private final String mUrl;

        BraintreeEnvironment(String url) {
            mUrl = url;
        }

        protected String getMerchantId() {
            return BRAINTREE_MERCHANT_ID;
        }

        protected String getCollectorUrl() {
            return mUrl;
        }
    }

    private static Object sDeviceCollector;

    /**
     * Collect device information for fraud identification purposes.
     *
     * @param fragment {@link BraintreeFragment}
     * @return Device data String to send to Braintree.
     */
    public static String collectDeviceData(BraintreeFragment fragment) {
        return collectDeviceData(fragment, BraintreeEnvironment.SANDBOX.getMerchantId(),
                BraintreeEnvironment.SANDBOX.getCollectorUrl());
    }

    /**
     * Collect device information for fraud identification purposes. This should be used in conjunction
     * with a non-aggregate fraud id.
     *
     * @param fragment {@link BraintreeFragment}
     * @param merchantId The fraud merchant id from Braintree.
     * @param collectorUrl The fraud collector url from Braintree.
     * @return Device data String to send to Braintree.
     */
    public static String collectDeviceData(BraintreeFragment fragment, String merchantId,
            String collectorUrl) {
        JSONObject deviceData = new JSONObject();

        try {
            DeviceCollector deviceCollector = new DeviceCollector(fragment.getActivity());
            sDeviceCollector = deviceCollector;
            deviceCollector.setMerchantId(merchantId);
            deviceCollector.setCollectorUrl(collectorUrl);
            deviceCollector.setStatusListener(new StatusListener() {
                @Override
                public void onCollectorStart() {}

                @Override
                public void onCollectorSuccess() {
                    sDeviceCollector = null;
                }

                @Override
                public void onCollectorError(ErrorCode errorCode, Exception e) {
                    sDeviceCollector = null;
                }
            });

            String deviceSessionId = UUID.randomUUID().toString().replace("-", "");
            deviceData.put(DEVICE_SESSION_ID_KEY, deviceSessionId);
            deviceData.put(FRAUD_MERCHANT_ID_KEY, merchantId);
            deviceCollector.collect(deviceSessionId);
        } catch (NoClassDefFoundError | JSONException ignored) {}

        try {
            deviceData.put(CORRELATION_ID_KEY, getPayPalClientMetadataId(fragment.getApplicationContext()));
        } catch (JSONException ignored) {}

        return deviceData.toString();
    }

    /**
     * Collect device information for fraud identification purposes from PayPal only.
     *
     * @param context A valid {@link Context}
     * @return The client metadata id associated with the collected data.
     */
    public static String getPayPalClientMetadataId(Context context) {
        return  PayPalOneTouchCore.getClientMetadataId(context);
    }
}
