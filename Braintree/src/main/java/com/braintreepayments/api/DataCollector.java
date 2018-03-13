package com.braintreepayments.api;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.text.TextUtils;

import com.braintreepayments.api.interfaces.BraintreeResponseListener;
import com.braintreepayments.api.interfaces.ConfigurationListener;
import com.braintreepayments.api.internal.UUIDHelper;
import com.braintreepayments.api.models.Configuration;
import com.paypal.android.sdk.data.collector.PayPalDataCollector;
import com.paypal.android.sdk.onetouch.core.PayPalOneTouchCore;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * DataCollector is used to collect device information to aid in fraud detection and prevention.
 */
public class DataCollector {

    private static final String DEVICE_SESSION_ID_KEY = "device_session_id";
    private static final String FRAUD_MERCHANT_ID_KEY = "fraud_merchant_id";
    private static final String CORRELATION_ID_KEY = "correlation_id";

    private static final String BRAINTREE_MERCHANT_ID = "600000";


    /**
     * Collect device information for fraud identification purposes.
     *
     * @param fragment {@link BraintreeFragment}
     * @param listener to be called with the device data String to send to Braintree.
     */
    public static void collectDeviceData(BraintreeFragment fragment, BraintreeResponseListener<String> listener) {
        collectDeviceData(fragment, null, listener);
    }

    /**
     * Collect device information for fraud identification purposes. This should be used in conjunction
     * with a non-aggregate fraud id.
     *
     * @param fragment {@link BraintreeFragment}
     * @param merchantId The fraud merchant id from Braintree.
     * @param listener listener to be called with the device data String to send to Braintree.
     */
    public static void collectDeviceData(final BraintreeFragment fragment, final String merchantId,
            final BraintreeResponseListener<String> listener) {
        fragment.waitForConfiguration(new ConfigurationListener() {
            @Override
            public void onConfigurationFetched(Configuration configuration) {
                final JSONObject deviceData = new JSONObject();

                try {
                    String clientMetadataId = getPayPalClientMetadataId(fragment.getApplicationContext());
                    if (!TextUtils.isEmpty(clientMetadataId)) {
                        deviceData.put(CORRELATION_ID_KEY, clientMetadataId);
                    }
                } catch (JSONException ignored) {}

                if (configuration.getKount().isEnabled()) {
                    final String id;
                    if (merchantId != null) {
                        id = merchantId;
                    } else {
                        id = configuration.getKount().getKountMerchantId();
                    }

                    try {
                        final String deviceSessionId = UUIDHelper.getFormattedUUID();
                        startDeviceCollector(fragment, id, deviceSessionId, new BraintreeResponseListener<String>() {
                            @Override
                            public void onResponse(String sessionId) {
                                try {
                                    deviceData.put(DEVICE_SESSION_ID_KEY, deviceSessionId);
                                    deviceData.put(FRAUD_MERCHANT_ID_KEY, id);
                                } catch (JSONException ignored) {}

                                listener.onResponse(deviceData.toString());
                            }
                        });
                    } catch (ClassNotFoundException | NoClassDefFoundError | NumberFormatException ignored) {
                        listener.onResponse(deviceData.toString());
                    }
                } else {
                    listener.onResponse(deviceData.toString());
                }
            }
        });
    }

    /**
     * Collect PayPal device information for fraud identification purposes.
     *
     * @param fragment {@link BraintreeFragment}
     * @param listener listener to be called with the device data String to send to Braintree.
     */
    public static void collectPayPalDeviceData(final BraintreeFragment fragment, final BraintreeResponseListener<String> listener) {
        final JSONObject deviceData = new JSONObject();

        try {
            String clientMetadataId = getPayPalClientMetadataId(fragment.getApplicationContext());
            if (!TextUtils.isEmpty(clientMetadataId)) {
                deviceData.put(CORRELATION_ID_KEY, clientMetadataId);
            }
        } catch (JSONException ignored) { }

        listener.onResponse(deviceData.toString());
    }

    /**
     * @deprecated Use {@link #collectDeviceData(BraintreeFragment, BraintreeResponseListener)} instead.
     */
    @Deprecated
    public static String collectDeviceData(BraintreeFragment fragment) {
        return collectDeviceData(fragment, BRAINTREE_MERCHANT_ID);
    }

    /**
     * @deprecated Use {@link #collectDeviceData(BraintreeFragment, String, BraintreeResponseListener)} instead.
     */
    @Deprecated
    public static String collectDeviceData(BraintreeFragment fragment, String merchantId) {
        JSONObject deviceData = new JSONObject();

        try {
            String deviceSessionId = UUIDHelper.getFormattedUUID();
            startDeviceCollector(fragment, merchantId, deviceSessionId, null);
            deviceData.put(DEVICE_SESSION_ID_KEY, deviceSessionId);
            deviceData.put(FRAUD_MERCHANT_ID_KEY, merchantId);
        } catch (ClassNotFoundException | NoClassDefFoundError | NumberFormatException | JSONException ignored) {}

        try {
            String clientMetadataId = getPayPalClientMetadataId(fragment.getApplicationContext());
            if (!TextUtils.isEmpty(clientMetadataId)) {
                deviceData.put(CORRELATION_ID_KEY, clientMetadataId);
            }
        } catch (JSONException ignored) {}

        return deviceData.toString();
    }

    /**
     * @deprecated Use {@link #collectDeviceData(BraintreeFragment)} instead.
     */
    @Deprecated
    public static String collectDeviceData(Context context, BraintreeFragment fragment) {
        return collectDeviceData(fragment);
    }

    /**
     * @deprecated Use {@link #collectDeviceData(BraintreeFragment, String)} instead.
     */
    @Deprecated
    public static String collectDeviceData(Context context, BraintreeFragment fragment, String merchantId) {
        return collectDeviceData(fragment, merchantId);
    }

    /**
     * Collect device information for fraud identification purposes from PayPal only.
     *
     * @param context A valid {@link Context}
     * @return The client metadata id associated with the collected data.
     */
    public static String getPayPalClientMetadataId(Context context) {
        try {
            return PayPalOneTouchCore.getClientMetadataId(context);
        } catch (NoClassDefFoundError ignored) {}

        try {
            return PayPalDataCollector.getClientMetadataId(context);
        } catch (NoClassDefFoundError ignored) {}

        return "";
    }

    private static void startDeviceCollector(final BraintreeFragment fragment, final String merchantId,
            final String deviceSessionId, @Nullable final BraintreeResponseListener<String> listener)
            throws ClassNotFoundException, NumberFormatException {
        fragment.sendAnalyticsEvent("data-collector.kount.started");

        Class.forName(com.kount.api.DataCollector.class.getName());

        fragment.waitForConfiguration(new ConfigurationListener() {
            @Override
            public void onConfigurationFetched(Configuration configuration) {
                final com.kount.api.DataCollector dataCollector = com.kount.api.DataCollector.getInstance();
                dataCollector.setContext(fragment.getApplicationContext());
                dataCollector.setMerchantID(Integer.parseInt(merchantId));
                dataCollector.setLocationCollectorConfig(com.kount.api.DataCollector.LocationConfig.COLLECT);
                dataCollector.setEnvironment(getDeviceCollectorEnvironment(configuration.getEnvironment()));

                dataCollector.collectForSession(deviceSessionId, new com.kount.api.DataCollector.CompletionHandler() {
                    @Override
                    public void completed(String sessionID) {
                        fragment.sendAnalyticsEvent("data-collector.kount.succeeded");

                        if (listener != null) {
                            listener.onResponse(sessionID);
                        }
                    }

                    @Override
                    public void failed(String sessionID, final com.kount.api.DataCollector.Error error) {
                        fragment.sendAnalyticsEvent("data-collector.kount.failed");

                        if (listener != null) {
                            listener.onResponse(sessionID);
                        }
                    }
                });
            }
        });
    }

    @VisibleForTesting
    static int getDeviceCollectorEnvironment(String environment) {
        if ("production".equalsIgnoreCase(environment)) {
            return com.kount.api.DataCollector.ENVIRONMENT_PRODUCTION;
        }
        return com.kount.api.DataCollector.ENVIRONMENT_TEST;
    }
}
