package com.braintreepayments.api;

import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.braintreepayments.api.interfaces.BraintreeResponseListener;
import com.braintreepayments.api.interfaces.ConfigurationListener;
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
     * <p>
     * We recommend that you call this method as early as possible, e.g. at app launch. If that's too early,
     * call it at the beginning of customer checkout.
     * <p>
     * Use the return value on your server, e.g. with `Transaction.sale`.
     *
     * @param fragment   {@link BraintreeFragment}
     * @param merchantId Optional - Custom Kount merchant id. Leave blank to use the default.
     * @param listener   listener called with the deviceData string that should be passed into server-side calls, such as `Transaction.sale`.
     * @deprecated
     */
    @Deprecated
    public static void collectDeviceData(final BraintreeFragment fragment, final String merchantId, final BraintreeResponseListener<String> listener) {
        fragment.waitForConfiguration(new ConfigurationListener() {
            @Override
            public void onConfigurationFetched(Configuration configuration) {
                final JSONObject deviceData = new JSONObject();

                try {
                    String clientMetadataId = getPayPalClientMetadataId(fragment.getApplicationContext());
                    if (!TextUtils.isEmpty(clientMetadataId)) {
                        deviceData.put(CORRELATION_ID_KEY, clientMetadataId);
                    }
                } catch (JSONException ignored) {
                }

                listener.onResponse(deviceData.toString());
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
        } catch (JSONException ignored) {
        }

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
        } catch (NoClassDefFoundError ignored) {
        }

        try {
            return PayPalDataCollector.getClientMetadataId(context);
        } catch (NoClassDefFoundError ignored) {
        }

        return "";
    }

    static void collectRiskData(final BraintreeFragment fragment,
                                @NonNull final PaymentMethodNonce paymentMethodNonce) {
        fragment.waitForConfiguration(new ConfigurationListener() {
            @Override
            public void onConfigurationFetched(Configuration configuration) {
                if (configuration.getCardConfiguration().isFraudDataCollectionEnabled()) {
                    HashMap<String, String> additionalProperties = new HashMap<>();
                    additionalProperties.put("rda_tenant", "bt_card");
                    additionalProperties.put("mid", configuration.getMerchantId());

                    if (fragment.getAuthorization() instanceof ClientToken) {
                        String customerId = ((ClientToken) fragment.getAuthorization()).getCustomerId();
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
}
