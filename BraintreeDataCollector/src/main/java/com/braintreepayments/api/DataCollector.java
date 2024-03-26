package com.braintreepayments.api;

import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * DataCollector is used to collect device information to aid in fraud detection and prevention.
 */
public class DataCollector {

    private static final String CORRELATION_ID_KEY = "correlation_id";

    private final BraintreeClient braintreeClient;
    private final PayPalDataCollector payPalDataCollector;

    public DataCollector(@NonNull BraintreeClient braintreeClient) {
        this(braintreeClient, new PayPalDataCollector(braintreeClient));
    }

    @VisibleForTesting
    DataCollector(BraintreeClient braintreeClient, PayPalDataCollector payPalDataCollector) {
        this.braintreeClient = braintreeClient;
        this.payPalDataCollector = payPalDataCollector;
    }

    /**
     * Collect device information for fraud identification purposes.
     *
     * @param context  Android Context
     * @param callback {@link DataCollectorCallback
     * @deprecated Passing in {@link DataCollectorRequest} is required. Use
     * {@link PayPalDataCollector#collectDeviceData(Context, DataCollectorRequest, PayPalDataCollectorCallback)} instead.
     */
    @Deprecated()
    public void collectDeviceData(
        @NonNull Context context,
        @NonNull DataCollectorCallback callback
    ) {
        DataCollectorRequest request = new DataCollectorRequest(false);
        collectDeviceData(context, request, callback);
    }

    /**
     * Collect device information for fraud identification purposes.
     *
     * @param context  Android Context
     * @param dataCollectorRequest The {@link DataCollectorRequest} containing the configuration for
     *                             the data collection request
     * @param callback {@link DataCollectorCallback}
     */
    public void collectDeviceData(
        @NonNull Context context,
        @NonNull DataCollectorRequest dataCollectorRequest,
        @NonNull DataCollectorCallback callback
    ) {
        final Context appContext = context.getApplicationContext();
        braintreeClient.getConfiguration(new ConfigurationCallback() {
            @Override
            public void onResult(@Nullable Configuration configuration, @Nullable Exception error) {
                if (configuration != null) {
                    final JSONObject deviceData = new JSONObject();
                    try {
                        String clientMetadataId = getPayPalClientMetadataId(appContext, configuration, dataCollectorRequest);
                        if (!TextUtils.isEmpty(clientMetadataId)) {
                            deviceData.put(CORRELATION_ID_KEY, clientMetadataId);
                        }
                    } catch (JSONException ignored) {
                    }
                    callback.onResult(deviceData.toString(), null);
                } else {
                    callback.onResult(null, error);
                }
            }
        });
    }

    /**
     * Collects device data based on your merchant configuration.
     * <p>
     * We recommend that you call this method as early as possible, e.g. at app launch. If that's too early,
     * call it at the beginning of customer checkout.
     * <p>
     * Use the return value on your server, e.g. with `Transaction.sale`.
     *
     * @param context    Android Context
     * @param merchantId Optional - Custom Kount merchant id. Leave blank to use the default.
     * @param callback   {@link DataCollectorCallback}
     * @deprecated Kount is officially deprecated, use {@link PayPalDataCollector#collectDeviceData(Context, String, PayPalDataCollectorCallback)} instead.
     */
    @Deprecated
    public void collectDeviceData(
        @NonNull final Context context,
        @Nullable final String merchantId,
        @NonNull final DataCollectorCallback callback
    ) {
        collectDeviceData(context, callback);
    }

    /**
     * Collect device information for fraud identification purposes from PayPal only.
     *
     * @param context Android Context
     * @param configuration the merchant configuration
     * @return The client metadata id associated with the collected data.
     */
    private String getPayPalClientMetadataId(
        Context context,
        Configuration configuration,
        DataCollectorRequest dataCollectorRequest
    ) {
        try {
            return payPalDataCollector.getClientMetadataId(context, configuration, dataCollectorRequest.getHasUserLocationConsent());
        } catch (NoClassDefFoundError ignored) {
        }
        return "";
    }
}
