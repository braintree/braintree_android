package com.paypal.android.sdk.data.collector;

import android.content.Context;
import android.support.annotation.MainThread;

import com.paypal.android.sdk.onetouch.core.metadata.MetadataIdProvider;
import com.paypal.android.sdk.onetouch.core.metadata.MetadataIdProviderImpl;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

public class PayPalDataCollector {

    private static MetadataIdProvider sMetadataIdProvider;

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
        return getClientMetadataId(context, null);
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
     * @param pairingId The desired pairing id
     * @return clientMetadataId Your server will send this to PayPal
     */
    @MainThread
    public static String getClientMetadataId(Context context, String pairingId) {
        return getClientMetadataId(context, InstallationIdentifier.getInstallationGUID(context), pairingId);
    }

    @MainThread
    static String getClientMetadataId(Context context, String applicationGuid, String pairingId) {
        if (sMetadataIdProvider == null) {
            if (context == null) {
                return "";
            }

            sMetadataIdProvider = new MetadataIdProviderImpl();

            Map<String, Object> params;
            if (pairingId != null) {
                params = new HashMap<>();
                params.put(MetadataIdProvider.PAIRING_ID, pairingId);
            } else {
                params = Collections.emptyMap();
            }

            String clientMetadataId = sMetadataIdProvider.init(context.getApplicationContext(), applicationGuid, params);

            Executors.newSingleThreadExecutor().submit(new Runnable() {
                @Override
                public void run() {
                    sMetadataIdProvider.flush();
                }
            });

            return clientMetadataId;
        } else {
            return sMetadataIdProvider.generatePairingId(pairingId);
        }
    }
}
