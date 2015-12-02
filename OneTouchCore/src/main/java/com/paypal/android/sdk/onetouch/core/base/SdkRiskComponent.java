package com.paypal.android.sdk.onetouch.core.base;

import android.content.Context;
import android.util.Log;

import com.paypal.android.sdk.onetouch.core.metadata.MetadataIdProvider;
import com.paypal.android.sdk.onetouch.core.metadata.MetadataIdProviderImpl;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;

public final class SdkRiskComponent {
    private static final String TAG = SdkRiskComponent.class.getSimpleName();

    private static MetadataIdProvider sMetadataIdProvider;

    /**
     * Starts the risk component if it hasn't been initialized yet.  Otherwise, just generate a
     * clientMetadataId.
     * <p>
     * Warning!  The init() method MUST be run on the main thread!
     *
     * @param context
     * @return the clientMetadataId
     */
    public static synchronized final String getClientMetadataId(ExecutorService executorService,
            Context context,
            String applicationGuid,
            String productVersion, String pairingId) {
        if (null == sMetadataIdProvider) {
            try {
                sMetadataIdProvider = new MetadataIdProviderImpl();

                Map<String, Object> params;
                if (null != pairingId) {
                    params = new HashMap<>();
                    params.put(MetadataIdProvider.PAIRING_ID, pairingId);
                } else {
                    params = Collections.emptyMap();
                }

                String clientMetadataId =
                        sMetadataIdProvider.init(
                                context,
                                applicationGuid,
                                productVersion,
                                params);

                executorService.submit(new Runnable() {
                    // don't run this on main UI thread.
                    @Override
                    public void run() {
                        sMetadataIdProvider.flush();
                    }
                });

                return clientMetadataId;

            } catch (Throwable t) {
                Log.e(Constants.PUBLIC_TAG,
                        "An internal component failed to initialize: " + t.getMessage());
                return null;
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
