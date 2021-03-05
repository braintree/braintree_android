package com.braintreepayments.api;

import android.content.Context;

import androidx.annotation.Nullable;

/**
 * Callback for receiving result of
 * {@link DataCollector#collectDeviceData(Context, DataCollectorCallback)} and
 * {@link DataCollector#collectDeviceData(Context, String, DataCollectorCallback)} and
 * {@link DataCollector#collectPayPalDeviceData(Context, DataCollectorCallback)}.
 */
public interface DataCollectorCallback {

    /**
     * @param deviceData the device information collected for fraud detection
     * @param error an exception that occurred while fetching device data
     */
    void onResult(@Nullable String deviceData, @Nullable Exception error);
}
