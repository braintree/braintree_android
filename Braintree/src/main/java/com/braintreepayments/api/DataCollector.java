package com.braintreepayments.api;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import android.text.TextUtils;

import com.braintreepayments.api.interfaces.BraintreeResponseListener;
import com.braintreepayments.api.interfaces.ConfigurationListener;
import com.braintreepayments.api.internal.UUIDHelper;
import com.braintreepayments.api.models.ClientToken;
import com.braintreepayments.api.models.Configuration;
import com.braintreepayments.api.models.PaymentMethodNonce;
import com.paypal.android.sdk.data.collector.InstallationIdentifier;
import com.paypal.android.sdk.data.collector.PayPalDataCollector;
import com.paypal.android.sdk.data.collector.PayPalDataCollectorRequest;
import com.paypal.android.sdk.onetouch.core.PayPalOneTouchCore;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

/**
 * DataCollector is used to collect device information to aid in fraud detection and prevention.
 */
public class DataCollector {

    private static final String DEVICE_SESSION_ID_KEY = "device_session_id";
    private static final String FRAUD_MERCHANT_ID_KEY = "fraud_merchant_id";
    private static final String CORRELATION_ID_KEY = "correlation_id";

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
     * Collects device data based on your merchant configuration.
     *
     * We recommend that you call this method as early as possible, e.g. at app launch. If that's too early,
     * call it at the beginning of customer checkout.
     *
     * Use the return value on your server, e.g. with `Transaction.sale`.
     *
     * @param fragment {@link BraintreeFragment}
     * @param merchantId Optional - Custom Kount merchant id. Leave blank to use the default.
     * @param listener listener called with the deviceData string that should be passed into server-side calls, such as `Transaction.sale`.
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
        } catch (JSONException ignored) {}

        listener.onResponse(deviceData.toString());
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

    static void collectRiskData(final BraintreeFragment fragment,
                                @NonNull final PaymentMethodNonce paymentMethodNonce) {
        fragment.waitForConfiguration(new ConfigurationListener() {
            @Override
            public void onConfigurationFetched(Configuration configuration) {
                if (configuration.getCardConfiguration().isFraudDataCollectionEnabled()) {
                    HashMap<String,String> additionalProperties = new HashMap<>();
                    additionalProperties.put("rda_tenant", "bt_card");
                    additionalProperties.put("mid", configuration.getMerchantId());

                    if (fragment.getAuthorization() instanceof ClientToken) {
                        String customerId = ((ClientToken)fragment.getAuthorization()).getCustomerId();
                        if (customerId != null) {
                            additionalProperties.put("cid", customerId);
                        }
                    }

                    PayPalDataCollectorRequest request = new PayPalDataCollectorRequest()
                            .setApplicationGuid(InstallationIdentifier.getInstallationGUID(fragment.getApplicationContext()))
                            .setClientMetadataId(paymentMethodNonce.getNonce())
                            .setDisableBeacon(true)
                            .setAdditionalData(additionalProperties);

                    PayPalDataCollector.getClientMetadataId(fragment.getApplicationContext(), request);
                }
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
