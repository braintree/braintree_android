package com.braintreepayments.api;

import androidx.annotation.Nullable;

import com.samsung.android.sdk.samsungpay.v2.payment.CustomSheetPaymentInfo;

/**
 * Callback for receiving result of
 * {@link SamsungPayClient#buildCustomSheetPaymentInfo(BuildCustomSheetPaymentInfoCallback)}.
 */
public interface BuildCustomSheetPaymentInfoCallback {

    /**
     * @param builder [CustomSheetPaymentInfo.Builder] on success; null otherwise
     * @param error an exception that occurred while building a custom sheet payment info
     */
    void onResult(@Nullable CustomSheetPaymentInfo.Builder builder, @Nullable Exception error);
}
