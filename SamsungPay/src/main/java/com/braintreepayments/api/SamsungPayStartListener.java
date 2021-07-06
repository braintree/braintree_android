package com.braintreepayments.api;

import androidx.annotation.NonNull;

import com.samsung.android.sdk.samsungpay.v2.payment.CardInfo;
import com.samsung.android.sdk.samsungpay.v2.payment.CustomSheetPaymentInfo;
import com.samsung.android.sdk.samsungpay.v2.payment.sheet.CustomSheet;

public interface SamsungPayStartListener {
    void onSamsungPayStartError(@NonNull Exception error);
    void onSamsungPayStartSuccess(@NonNull SamsungPayNonce samsungPayNonce, CustomSheetPaymentInfo paymentInfo);
    void onSamsungPayCardInfoUpdated(CardInfo cardInfo, CustomSheet customSheet);
}
