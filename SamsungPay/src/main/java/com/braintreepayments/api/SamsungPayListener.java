package com.braintreepayments.api;

import androidx.annotation.NonNull;

import com.samsung.android.sdk.samsungpay.v2.payment.CardInfo;
import com.samsung.android.sdk.samsungpay.v2.payment.CustomSheetPaymentInfo;
import com.samsung.android.sdk.samsungpay.v2.payment.sheet.CustomSheet;

/**
 * Callback for receiving results of
 * {@link SamsungPayClient#startSamsungPay(CustomSheetPaymentInfo, SamsungPayListener)}.
 */
public interface SamsungPayListener {

    /**
     * Called when Samsung Pay card info has been updated. {@link SamsungPayClient#updateCustomSheet(CustomSheet)}
     * @param cardInfo SamsungPay [CardInfo]
     * @param customSheet SamsungPay [CustomSheet]
     */
    void onSamsungPayCardInfoUpdated(CardInfo cardInfo, CustomSheet customSheet);

    /**
     * Called on Samsung Pay start success.
     * @param samsungPayNonce {@link PaymentMethodNonce} representing a Samsung Pay Card
     * @param paymentInfo Samsung Pay [CustomSheetPaymentInfo] containing additional information
     */
    void onSamsungPayStartSuccess(@NonNull SamsungPayNonce samsungPayNonce, CustomSheetPaymentInfo paymentInfo);

    /**
     * Called on Samsung Pay start error.
     * @param error {@link SamsungPayError}
     */
    void onSamsungPayStartError(@NonNull Exception error);
}
