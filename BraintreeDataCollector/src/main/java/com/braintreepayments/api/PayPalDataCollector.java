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
    private final PayPalInstallationIdentifier payPalInstallationIdentifier;

    PayPalDataCollector() {
        this(MagnesSDK.getInstance(), new PayPalInstallationIdentifier());
    }

    @VisibleForTesting
    PayPalDataCollector(MagnesSDK magnesSDK, PayPalInstallationIdentifier payPalInstallationIdentifier) {
        this.magnesSDK = magnesSDK;
        this.payPalInstallationIdentifier = payPalInstallationIdentifier;
    }

    String getPayPalInstallationGUID(Context context) {
        return payPalInstallationIdentifier.getInstallationGUID(context);
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
     * @return clientMetadataId Your server will send this to PayPal
     */
    @MainThread
    String getClientMetadataId(Context context) {
        PayPalDataCollectorRequest request = new PayPalDataCollectorRequest()
                .setApplicationGuid(getPayPalInstallationGUID(context));

        return getClientMetadataId(context, request);
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
     * @return clientMetadataId Your server will send this to PayPal
     */
    @MainThread
    String getClientMetadataId(Context context, PayPalDataCollectorRequest request) {
        // NEXT_MAJOR_VERSION In error scenarios, this function return null instead of "".
        if (context == null) {
            return "";
        }

        try {
            MagnesSettings.Builder magnesSettingsBuilder = new MagnesSettings.Builder(context)
                    .setMagnesSource(MagnesSource.BRAINTREE)
                    .disableBeacon(request.isDisableBeacon())
                    .setMagnesEnvironment(Environment.LIVE)
                    .setAppGuid(request.getApplicationGuid());

            magnesSDK.setUp(magnesSettingsBuilder.build());

            MagnesResult result = magnesSDK.collectAndSubmit(context, request.getClientMetadataId(), request.getAdditionalData());

            return result.getPaypalClientMetaDataId();
        } catch (InvalidInputException e) {
            // Either clientMetadataId or appGuid exceeds their character limit
            Log.e("Exception", "Error fetching client metadata ID. Contact Braintree Support for assistance.", e);
            return "";
        }
    }
}
