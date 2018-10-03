package com.paypal.android.sdk.data.collector;

import android.content.Context;
import android.support.annotation.MainThread;

import lib.android.paypal.com.magnessdk.MagnesResult;
import lib.android.paypal.com.magnessdk.MagnesSDK;
import lib.android.paypal.com.magnessdk.MagnesSettings;
import lib.android.paypal.com.magnessdk.MagnesSource;

public class PayPalDataCollector {
    /**
     * Gets a Client Metadata ID at the time of payment activity. Once a user initiates a PayPal payment
     * from their device, PayPal uses the Client Metadata ID to verify that the payment is
     * originating from a valid, user-consented device and application. This helps reduce fraud and
     * decrease declines. This method MUST be called prior to initiating a pre-consented payment (a
     * "future payment") from a mobile device. Pass the result to your server, to include in the
     * payment request sent to PayPal. Do not otherwise cache or store this value.
     *
     * @param context
     * @return clientMetadataId Your server will send this to PayPal
     */
    @MainThread
    public static String getClientMetadataId(Context context) {
        PayPalDataCollectorRequest request = new PayPalDataCollectorRequest()
                .setApplicationGuid(InstallationIdentifier.getInstallationGUID(context));

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
     * @param context
     * @param clientMetadataId The desired client metadata ID
     * @return clientMetadataId Your server will send this to PayPal
     */
    @MainThread
    public static String getClientMetadataId(Context context, String clientMetadataId) {
        PayPalDataCollectorRequest request = new PayPalDataCollectorRequest()
                .setApplicationGuid(InstallationIdentifier.getInstallationGUID(context))
                .setClientMetadataId(clientMetadataId);

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
     * @param context An Android context.
     * @param request configures what data to collect.
     * @return
     */
    @MainThread
    public static String getClientMetadataId(Context context, PayPalDataCollectorRequest request) {
        if (context == null) {
            return "";
        }

        MagnesSDK magnesInstance = MagnesSDK.getInstance();
        MagnesSettings.Builder magnesSettingsBuilder = new MagnesSettings.Builder(context)
                .setMagnesSource(MagnesSource.BRAINTREE)
                .setAppGuid(request.getApplicationGuid());

        magnesInstance.setUp(magnesSettingsBuilder.build());

        MagnesResult result = magnesInstance.collectAndSubmit(context, request.getClientMetadataId(), null);

        return result.getPaypalClientMetaDataId();
    }
}
