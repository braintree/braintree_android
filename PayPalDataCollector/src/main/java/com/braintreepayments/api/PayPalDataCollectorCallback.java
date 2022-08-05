package com.braintreepayments.api;

import android.content.Context;

import androidx.annotation.Nullable;

/**
 * Callback for receiving result of
 * {@link PayPalDataCollector#collectDeviceData(Context, PayPalDataCollectorCallback)}
 */
public interface PayPalDataCollectorCallback {

    /**
     * @param deviceData the device information collected for fraud detection
     * @param error an exception that occurred while fetching device data
     */
    void onResult(@Nullable String deviceData, @Nullable Exception error);
}
