package com.braintreepayments.api;

import android.content.Context;

import androidx.annotation.Nullable;

/**
 * Callback for receiving result of
 * {@link DataCollector#collectPayPalDeviceData(Context, String, CollectPayPalDeviceDataCallback)}
 */
public interface CollectPayPalDeviceDataCallback {

    /**
     * @param clientMetadataId PayPal Client Metadata ID used to help reduce fraud and decrease declines.
     * @param error an exception that occurred while fetching device data
     */
    void onResult(@Nullable String clientMetadataId, @Nullable Exception error);
}
