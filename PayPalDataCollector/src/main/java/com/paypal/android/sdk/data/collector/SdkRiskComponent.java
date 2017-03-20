package com.paypal.android.sdk.data.collector;

import android.content.Context;
import android.support.annotation.MainThread;

/**
 * @deprecated Use {@link PayPalDataCollector} instead.
 */
@Deprecated
public final class SdkRiskComponent {

    /**
     * @deprecated Use {@link}
     */
    @Deprecated
    @MainThread
    public static String getClientMetadataId(Context context, String applicationGuid, String pairingId) {
        return PayPalDataCollector.getClientMetadataId(context, applicationGuid, pairingId);
    }
}
