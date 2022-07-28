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

    private static final String DEVICE_SESSION_ID_KEY = "device_session_id";
    private static final String FRAUD_MERCHANT_ID_KEY = "fraud_merchant_id";
    private static final String CORRELATION_ID_KEY = "correlation_id";

    private final BraintreeClient braintreeClient;
    private final PayPalDataCollector payPalDataCollector;
    private final KountDataCollector kountDataCollector;
    private final UUIDHelper uuidHelper;

    public DataCollector(@NonNull BraintreeClient braintreeClient) {
        this(braintreeClient, new PayPalDataCollector(braintreeClient), new KountDataCollector(braintreeClient), new UUIDHelper());
    }

    @VisibleForTesting
    DataCollector(BraintreeClient braintreeClient, PayPalDataCollector payPalDataCollector, KountDataCollector kountDataCollector, UUIDHelper uuidHelper) {
        this.braintreeClient = braintreeClient;
        this.payPalDataCollector = payPalDataCollector;
        this.kountDataCollector = kountDataCollector;
        this.uuidHelper = uuidHelper;
    }

    /**
     * Collect device information for fraud identification purposes.
     *
     * @param context  Android Context
     * @param callback {@link DataCollectorCallback}
     */
    public void collectDeviceData(@NonNull Context context, @NonNull DataCollectorCallback callback) {
        collectDeviceData(context, null, callback);
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
     */
    public void collectDeviceData(@NonNull final Context context, @Nullable final String merchantId, @NonNull final DataCollectorCallback callback) {
        braintreeClient.getConfiguration(new ConfigurationCallback() {
            @Override
            public void onResult(@Nullable Configuration configuration, @Nullable Exception error) {
                if (configuration != null) {
                    final JSONObject deviceData = new JSONObject();
                    try {
                        String clientMetadataId = getPayPalClientMetadataId(context, configuration);
                        if (!TextUtils.isEmpty(clientMetadataId)) {
                            deviceData.put(CORRELATION_ID_KEY, clientMetadataId);
                        }
                    } catch (JSONException ignored) {
                    }

                    if (configuration.isKountEnabled()) {
                        final String id;
                        if (merchantId != null) {
                            id = merchantId;
                        } else {
                            id = configuration.getKountMerchantId();
                        }

                        final String deviceSessionId = uuidHelper.getFormattedUUID();
                        kountDataCollector.startDataCollection(context, id, deviceSessionId, new KountDataCollectorCallback() {
                            @Override
                            public void onResult(@Nullable String kountSessionId, @Nullable Exception error) {
                                try {
                                    deviceData.put(DEVICE_SESSION_ID_KEY, deviceSessionId);
                                    deviceData.put(FRAUD_MERCHANT_ID_KEY, id);
                                } catch (JSONException ignored) {
                                }

                                callback.onResult(deviceData.toString(), null);
                            }
                        });
                    } else {
                        callback.onResult(deviceData.toString(), null);
                    }
                } else {
                    callback.onResult(null, error);
                }
            }
        });
    }

    /**
     * Collect device information for fraud identification purposes from PayPal only.
     *
     * @param context       Android Context
     * @param configuration the merchant configuration
     * @return The client metadata id associated with the collected data.
     */
    private String getPayPalClientMetadataId(Context context, Configuration configuration) {
        try {
            return payPalDataCollector.getClientMetadataId(context, configuration);
        } catch (NoClassDefFoundError ignored) {
        }
        return "";
    }

    /**
     * Collects device information for fraud identification purposes from PayPal only.
     *
     * @param context           Android Context
     * @param riskCorrelationId Correlation id to associate with data collection
     * @param callback          {@link DataCollectorCallback}
     */
    public void collectPayPalDeviceData(@NonNull final Context context, @NonNull final String riskCorrelationId, @NonNull final CollectPayPalDeviceDataCallback callback) {
        braintreeClient.getConfiguration(new ConfigurationCallback() {
            @Override
            public void onResult(@Nullable Configuration configuration, @Nullable Exception configurationError) {
                if (configuration != null) {
                    String applicationGUID = payPalDataCollector.getPayPalInstallationGUID(context);
                    PayPalDataCollectorRequest request = new PayPalDataCollectorRequest()
                            .setApplicationGuid(applicationGUID)
                            .setClientMetadataId(riskCorrelationId);

                    String clientMetaDataId =
                        payPalDataCollector.getClientMetadataId(context, request, configuration);
                    callback.onResult(clientMetaDataId, null);

                } else {
                    callback.onResult(null, configurationError);
                }
            }
        });
    }
}
