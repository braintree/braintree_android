package com.braintreepayments.api;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.VisibleForTesting;

import com.samsung.android.sdk.samsungpay.v2.PartnerInfo;
import com.samsung.android.sdk.samsungpay.v2.SamsungPay;
import com.samsung.android.sdk.samsungpay.v2.SpaySdk;
import com.samsung.android.sdk.samsungpay.v2.StatusListener;
import com.samsung.android.sdk.samsungpay.v2.payment.CardInfo;
import com.samsung.android.sdk.samsungpay.v2.payment.CustomSheetPaymentInfo;
import com.samsung.android.sdk.samsungpay.v2.payment.PaymentManager;
import com.samsung.android.sdk.samsungpay.v2.payment.sheet.CustomSheet;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.braintreepayments.api.SamsungPayMapAcceptedCardBrands.mapToSamsungPayCardBrands;
import static com.samsung.android.sdk.samsungpay.v2.SpaySdk.ERROR_SPAY_APP_NEED_TO_UPDATE;
import static com.samsung.android.sdk.samsungpay.v2.SpaySdk.ERROR_SPAY_SETUP_NOT_COMPLETED;
import static com.samsung.android.sdk.samsungpay.v2.SpaySdk.SPAY_NOT_READY;
import static com.samsung.android.sdk.samsungpay.v2.SpaySdk.SPAY_NOT_SUPPORTED;
import static com.samsung.android.sdk.samsungpay.v2.SpaySdk.SPAY_READY;

class SamsungPayInternalClient {

    private final SamsungPay samsungPay;
    private final PaymentManager paymentManager;

    private final Set<SpaySdk.Brand> braintreeAcceptedCardBrands;

    SamsungPayInternalClient(BraintreeClient braintreeClient, Configuration configuration) {
        this(braintreeClient.getApplicationContext(), configuration, new SamsungPayPartnerInfoBuilder()
                .setConfiguration(configuration)
                .setSessionId(braintreeClient.getSessionId())
                .setIntegrationType(braintreeClient.getIntegrationType())
                .build());
    }

    private SamsungPayInternalClient(Context context, Configuration configuration, PartnerInfo partnerInfo) {
        this(configuration, new SamsungPay(context, partnerInfo), new PaymentManager(context, partnerInfo));
    }

    @VisibleForTesting
    SamsungPayInternalClient(Configuration configuration, SamsungPay samsungPay, PaymentManager paymentManager) {
        this.braintreeAcceptedCardBrands = mapToSamsungPayCardBrands(configuration.getSupportedCardTypes());
        this.samsungPay = samsungPay;
        this.paymentManager = paymentManager;
    }

    void goToSamsungPayUpdatePage() {
        samsungPay.goToUpdatePage();
    }

    void activateSamsungPay() {
        samsungPay.activateSamsungPay();
    }

    void updateCustomSheet(CustomSheet customSheet) {
        paymentManager.updateSheet(customSheet);
    }

    void getSamsungPayStatus(final GetSamsungPayStatusCallback callback) {
        samsungPay.getSamsungPayStatus(new StatusListener() {
            @Override
            public void onSuccess(int statusCode, Bundle bundle) {
                Exception samsungPayError = null;

                @SamsungPayError int error = SamsungPayError.SAMSUNG_PAY_ERROR_UNKNOWN;
                if (statusCode != SPAY_READY) {
                    if (bundle != null && bundle.containsKey(SamsungPay.EXTRA_ERROR_REASON)) {
                        int reason = bundle.getInt(SamsungPay.EXTRA_ERROR_REASON);
                        switch (reason) {
                            case ERROR_SPAY_APP_NEED_TO_UPDATE:
                                error = SamsungPayError.SAMSUNG_PAY_APP_NEEDS_UPDATE;
                                break;
                            case ERROR_SPAY_SETUP_NOT_COMPLETED:
                                error = SamsungPayError.SAMSUNG_PAY_SETUP_NOT_COMPLETED;
                                break;
                        }
                    } else {
                        if (statusCode == SPAY_NOT_READY) {
                            error = SamsungPayError.SAMSUNG_PAY_NOT_READY;
                        } else if (statusCode == SPAY_NOT_SUPPORTED) {
                            error = SamsungPayError.SAMSUNG_PAY_NOT_SUPPORTED;
                        }
                    }
                    samsungPayError = new SamsungPayException(error);
                }

                callback.onResult(statusCode, samsungPayError);
            }

            @Override
            public void onFail(int errorCode, Bundle bundle) {
                SamsungPayException exception = new SamsungPayException(errorCode);
                callback.onResult(null, exception);
            }
        });
    }

    void getAcceptedCardBrands(final GetAcceptedCardBrandsCallback callback) {
        paymentManager.requestCardInfo(new Bundle(), new PaymentManager.CardInfoListener() {
            @Override
            public void onResult(final List<CardInfo> cardInfos) {
                Set<SpaySdk.Brand> spayAcceptedCardBrands = new HashSet<>();
                if (cardInfos != null) {
                    for (CardInfo cardInfo : cardInfos) {
                        spayAcceptedCardBrands.add(cardInfo.getBrand());
                    }
                }
                // equivalent to getting the intersection of both sets
                spayAcceptedCardBrands.retainAll(braintreeAcceptedCardBrands);
                callback.onResult(new ArrayList<>(spayAcceptedCardBrands), null);
            }

            @Override
            public void onFailure(int errorCode, Bundle bundle) {
                Exception error = new SamsungPayException(errorCode);
                callback.onResult(null, error);
            }
        });
    }

    void startSamsungPay(CustomSheetPaymentInfo customSheetPaymentInfo, final SamsungPayListener listener) {
        paymentManager.startInAppPayWithCustomSheet(customSheetPaymentInfo, new PaymentManager.CustomSheetTransactionInfoListener() {
            @Override
            public void onCardInfoUpdated(CardInfo cardInfo, CustomSheet customSheet) {
                paymentManager.updateSheet(customSheet);
                listener.onSamsungPayCardInfoUpdated(cardInfo, customSheet);
            }

            @Override
            public void onSuccess(CustomSheetPaymentInfo customSheetPaymentInfo, String s, Bundle bundle) {
                try {
                    JSONObject json = new JSONObject(s);
                    SamsungPayNonce samsungPayNonce = SamsungPayNonce.fromJSON(json);
                    listener.onSamsungPayStartSuccess(samsungPayNonce, customSheetPaymentInfo);
                } catch (JSONException e) {
                    listener.onSamsungPayStartError(e);
                }
            }

            @Override
            public void onFailure(int errorCode, Bundle bundle) {
                if (errorCode == SpaySdk.ERROR_USER_CANCELED) {
                    UserCanceledException userCanceledError = new UserCanceledException("User canceled Samsung Pay.");
                    listener.onSamsungPayStartError(userCanceledError);
                } else {
                    SamsungPayException samsungPayError = new SamsungPayException(errorCode);
                    listener.onSamsungPayStartError(samsungPayError);
                }
            }
        });
    }
}
