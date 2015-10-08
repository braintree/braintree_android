package com.braintreepayments.api;

import android.content.Context;

import com.braintreepayments.api.interfaces.ConfigurationListener;
import com.braintreepayments.api.models.Configuration;
import com.devicecollector.DeviceCollector;
import com.devicecollector.DeviceCollector.ErrorCode;
import com.devicecollector.DeviceCollector.StatusListener;
import com.paypal.android.sdk.onetouch.core.PayPalOneTouchCore;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.UUID;

/**
 * DataCollector is used to collect device information to aid in fraud detection and prevention.
 */
public class DataCollector {

    private static final String DEVICE_SESSION_ID_KEY = "device_session_id";
    private static final String FRAUD_MERCHANT_ID_KEY = "fraud_merchant_id";
    private static final String CORRELATION_ID_KEY = "correlation_id";

    private static final String BRAINTREE_MERCHANT_ID = "600000";
    private static final String SANDBOX_DEVICE_COLLECTOR_URL = "https://assets.braintreegateway.com/sandbox/data/logo.htm";
    private static final String PRODUCTION_DEVICE_COLLECTOR_URL = "https://assets.braintreegateway.com/data/logo.htm";

    private static Object sDeviceCollector;

    /**
     * Collect device information for fraud identification purposes.
     *
     * @param fragment {@link BraintreeFragment}
     * @return Device data String to send to Braintree.
     */
    public static String collectDeviceData(BraintreeFragment fragment) {
        return collectDeviceData(fragment, BRAINTREE_MERCHANT_ID);
    }

    /**
     * Collect device information for fraud identification purposes. This should be used in conjunction
     * with a non-aggregate fraud id.
     *
     * @param fragment {@link BraintreeFragment}
     * @param merchantId The fraud merchant id from Braintree.
     * @return Device data String to send to Braintree.
     */
    public static String collectDeviceData(BraintreeFragment fragment, String merchantId) {
        JSONObject deviceData = new JSONObject();

        try {
            String deviceSessionId = UUID.randomUUID().toString().replace("-", "");
            deviceData.put(DEVICE_SESSION_ID_KEY, deviceSessionId);
            deviceData.put(FRAUD_MERCHANT_ID_KEY, merchantId);
            startDeviceCollector(fragment, merchantId, deviceSessionId);
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

    private static void startDeviceCollector(final BraintreeFragment fragment,
            final String merchantId, final String deviceSessionId) {
        fragment.waitForConfiguration(new ConfigurationListener() {
            @Override
            public void onConfigurationFetched(Configuration configuration) {
                DeviceCollector deviceCollector = new DeviceCollector(fragment.getActivity());
                sDeviceCollector = deviceCollector;
                deviceCollector.setMerchantId(merchantId);
                deviceCollector.setCollectorUrl(getDeviceCollectorUrl(configuration.getEnvironment()));
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

                deviceCollector.collect(deviceSessionId);
            }
        });
    }

    static String getDeviceCollectorUrl(String environment) {
        if ("production".equalsIgnoreCase(environment)) {
            return PRODUCTION_DEVICE_COLLECTOR_URL;
        }
        return SANDBOX_DEVICE_COLLECTOR_URL;
    }
}
