package com.braintreepayments.api;

import android.content.Context;
import android.util.Log;

import androidx.annotation.MainThread;
import androidx.annotation.VisibleForTesting;

import lib.android.paypal.com.magnessdk.Environment;
import lib.android.paypal.com.magnessdk.InvalidInputException;
import lib.android.paypal.com.magnessdk.MagnesResult;
import lib.android.paypal.com.magnessdk.MagnesSDK;
import lib.android.paypal.com.magnessdk.MagnesSettings;
import lib.android.paypal.com.magnessdk.MagnesSource;

class PayPalDataCollector {

    private final MagnesSDK magnesSDK;
    private final UUIDHelper uuidHelper;

    PayPalDataCollector() {
        this(MagnesSDK.getInstance(), new UUIDHelper());
    }

    @VisibleForTesting
    PayPalDataCollector(MagnesSDK magnesSDK, UUIDHelper uuidHelper) {
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
}
