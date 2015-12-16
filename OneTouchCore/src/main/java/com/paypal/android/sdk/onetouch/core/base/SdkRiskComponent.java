package com.paypal.android.sdk.onetouch.core.base;

import android.content.Context;
import android.support.annotation.MainThread;
import android.util.Log;

import com.paypal.android.sdk.onetouch.core.metadata.MetadataIdProvider;
import com.paypal.android.sdk.onetouch.core.metadata.MetadataIdProviderImpl;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

public final class SdkRiskComponent {

    private static MetadataIdProvider sMetadataIdProvider;

    /**
     * Starts the risk component if it hasn't been initialized yet.  Otherwise, just generate a
     * clientMetadataId.
     *
     * @param context
     * @return the clientMetadataId
     */
    @MainThread
    public static String getClientMetadataId(Context context, String applicationGuid,
            String productVersion, String pairingId) {
        if (sMetadataIdProvider == null) {
            try {
                sMetadataIdProvider = new MetadataIdProviderImpl();

                Map<String, Object> params;
                if (pairingId != null) {
                    params = new HashMap<>();
                    params.put(MetadataIdProvider.PAIRING_ID, pairingId);
                } else {
                    params = Collections.emptyMap();
                }

                String clientMetadataId = sMetadataIdProvider.init(context.getApplicationContext(),
                        applicationGuid, productVersion, params);

                Executors.newSingleThreadExecutor().submit(new Runnable() {
                    @Override
                    public void run() {
                        sMetadataIdProvider.flush();
                    }
                });

                return clientMetadataId;
            } catch (Throwable t) {
                Log.e(Constants.PUBLIC_TAG, "An internal component failed to initialize: " +
                        t.getMessage());
                return "";
            }
        } else {
            String clientMetadataId;
            if (null != pairingId) {
                clientMetadataId = sMetadataIdProvider.generatePairingId(pairingId);
            } else {
                clientMetadataId = sMetadataIdProvider.generatePairingId();
            }

            return clientMetadataId;
        }
    }
}
