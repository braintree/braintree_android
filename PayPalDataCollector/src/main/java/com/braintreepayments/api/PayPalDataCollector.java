package com.braintreepayments.api;

import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * PayPalDataCollector is used to collect PayPal specific device information to aid in fraud detection and prevention.
 */
public class PayPalDataCollector {

    private static final String CORRELATION_ID_KEY = "correlation_id";

    private final MagnesInternalClient magnesInternalClient;
    private final UUIDHelper uuidHelper;
    private final BraintreeClient braintreeClient;

    public PayPalDataCollector(@NonNull BraintreeClient braintreeClient) {
        this(braintreeClient, new MagnesInternalClient(), new UUIDHelper());
    }

    @VisibleForTesting
    PayPalDataCollector(BraintreeClient braintreeClient, MagnesInternalClient magnesInternalClient, UUIDHelper uuidHelper) {
        this.braintreeClient = braintreeClient;
        this.magnesInternalClient = magnesInternalClient;
        this.uuidHelper = uuidHelper;
    }

    String getPayPalInstallationGUID(Context context) {
        return uuidHelper.getInstallationGUID(context);
    }

    /**
     * Gets a Client Metadata ID at the time of payment activity. Once a user initiates a PayPal payment
     * from their device, PayPal uses the Client Metadata ID to verify that the payment is
     * originating from a valid, user-consented device and application. This helps reduce fraud and
     * decrease declines. This method MUST be called prior to initiating a pre-consented payment (a
     * "future payment") from a mobile device. Pass the result to your server, to include in the
     * payment request sent to PayPal. Do not otherwise cache or store this value.
     *
     * @param context       Android Context
     * @param configuration The merchant configuration
     */
    @MainThread
    String getClientMetadataId(Context context, Configuration configuration) {
        PayPalDataCollectorRequest request = new PayPalDataCollectorRequest()
                .setApplicationGuid(getPayPalInstallationGUID(context));

        return getClientMetadataId(context, request, configuration);
    }

    /**
     * Gets a Client Metadata ID at the time of payment activity. Once a user initiates a PayPal payment
     * from their device, PayPal uses the Client Metadata ID to verify that the payment is
     * originating from a valid, user-consented device and application. This helps reduce fraud and
     * decrease declines. This method MUST be called prior to initiating a pre-consented payment (a
     * "future payment") from a mobile device. Pass the result to your server, to include in the
     * payment request sent to PayPal. Do not otherwise cache or store this value.
     *
     * @param context       Android Context.
     * @param request       configures what data to collect.
     * @param configuration the merchant configuration
     */
    @MainThread
    String getClientMetadataId(Context context, PayPalDataCollectorRequest request, Configuration configuration) {
        return magnesInternalClient.getClientMetadataId(context, configuration, request);
    }

    /**
     * Collects device data based on your merchant configuration.
     * <p>
     * We recommend that you call this method as early as possible, e.g. at app launch. If that's too early,
     * call it at the beginning of customer checkout.
     * <p>
     * Use the return value on your server, e.g. with `Transaction.sale`.
     *
     * @param context  Android Context
     * @param callback {@link PayPalDataCollectorCallback}
     */
    public void collectDeviceData(@NonNull final Context context, @NonNull final PayPalDataCollectorCallback callback) {
        collectDeviceData(context, null, callback);
    }

    /**
     * Collects device data for PayPal APIs.
     * <p>
     * We recommend that you call this method as early as possible, e.g. at app launch. If that's too early,
     * call it at the beginning of customer checkout.
     * <p>
     * Use the return value on your server, e.g. with `Transaction.sale`.
     *
     * @param context           Android Context
     * @param riskCorrelationId Optional client metadata id
     * @param callback          {@link PayPalDataCollectorCallback}
     */
    public void collectDeviceData(@NonNull final Context context, @Nullable final String riskCorrelationId, @NonNull final PayPalDataCollectorCallback callback) {
        braintreeClient.getConfiguration(new ConfigurationCallback() {
            @Override
            public void onResult(@Nullable Configuration configuration, @Nullable Exception error) {
                if (configuration != null) {
                    final JSONObject deviceData = new JSONObject();
                    try {
                        PayPalDataCollectorRequest request = new PayPalDataCollectorRequest()
                                .setApplicationGuid(getPayPalInstallationGUID(context));
                        if (riskCorrelationId != null) {
                            request.setRiskCorrelationId(riskCorrelationId);
                        }

                        String correlationId =
                                magnesInternalClient.getClientMetadataId(context, configuration, request);
                        if (!TextUtils.isEmpty(correlationId)) {
                            deviceData.put(CORRELATION_ID_KEY, correlationId);
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
}
