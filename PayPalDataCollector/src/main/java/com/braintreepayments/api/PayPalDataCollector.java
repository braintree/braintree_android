package com.braintreepayments.api;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import org.json.JSONException;
import org.json.JSONObject;

import lib.android.paypal.com.magnessdk.Environment;
import lib.android.paypal.com.magnessdk.InvalidInputException;
import lib.android.paypal.com.magnessdk.MagnesResult;
import lib.android.paypal.com.magnessdk.MagnesSDK;
import lib.android.paypal.com.magnessdk.MagnesSettings;
import lib.android.paypal.com.magnessdk.MagnesSource;

public class PayPalDataCollector {

    private static final String CORRELATION_ID_KEY = "correlation_id";

    private final MagnesSDK magnesSDK;
    private final UUIDHelper uuidHelper;
    private final BraintreeClient braintreeClient;

    PayPalDataCollector(@NonNull BraintreeClient braintreeClient) {
        this(braintreeClient, MagnesSDK.getInstance(), new UUIDHelper());
    }

    @VisibleForTesting
    PayPalDataCollector(BraintreeClient braintreeClient, MagnesSDK magnesSDK, UUIDHelper uuidHelper) {
        this.braintreeClient = braintreeClient;
        this.magnesSDK = magnesSDK;
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
     * @param context Android Context
     * @param configuration the merchant configurationn
     * @return clientMetadataId Your server will send this to PayPal
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
     * @param context Android Context.
     * @param request configures what data to collect.
     * @param configuration the merchant configuration
     * @return clientMetadataId Your server will send this to PayPal
     */
    @MainThread
    String getClientMetadataId(Context context, PayPalDataCollectorRequest request, Configuration configuration) {
        if (context == null || context.getApplicationContext() == null) {
            return "";
        }

        try {
            MagnesSettings.Builder magnesSettingsBuilder = new MagnesSettings.Builder(context.getApplicationContext())
                    .setMagnesSource(MagnesSource.BRAINTREE)
                    .disableBeacon(request.isDisableBeacon())
                    .setMagnesEnvironment(getMagnesEnvironment(configuration.getEnvironment()))
                    .setAppGuid(request.getApplicationGuid());

            magnesSDK.setUp(magnesSettingsBuilder.build());

            MagnesResult result = magnesSDK.collectAndSubmit(context.getApplicationContext(), request.getClientMetadataId(), request.getAdditionalData());

            return result.getPaypalClientMetaDataId();
        } catch (InvalidInputException e) {
            // Either clientMetadataId or appGuid exceeds their character limit
            Log.e("Exception", "Error fetching client metadata ID. Contact Braintree Support for assistance.", e);
            return "";
        }
    }

    private Environment getMagnesEnvironment(String environment) {
        if ("sandbox".equals(environment)) {
            return Environment.SANDBOX;
        }
        return Environment.LIVE;
    }

    /**
     * Collects device data based on your merchant configuration.
     * <p>
     * We recommend that you call this method as early as possible, e.g. at app launch. If that's too early,
     * call it at the beginning of customer checkout.
     * <p>
     * Use the return value on your server, e.g. with `Transaction.sale`.
     *  @param context    Android Context
     * @param callback   {@link PayPalDataCollectorCallback}
     */
    public void collectDeviceData(@NonNull final Context context, @NonNull final PayPalDataCollectorCallback callback) {
        braintreeClient.getConfiguration(new ConfigurationCallback() {
            @Override
            public void onResult(@Nullable Configuration configuration, @Nullable Exception error) {
                if (configuration != null) {
                    final JSONObject deviceData = new JSONObject();
                    try {
                        String clientMetadataId = getClientMetadataId(context, configuration);
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
}
