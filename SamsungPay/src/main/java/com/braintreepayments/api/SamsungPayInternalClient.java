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
import java.util.List;

public class SamsungPayInternalClient {

    private final SamsungPay samsungPay;
    private final PaymentManager paymentManager;

    SamsungPayInternalClient(Context context, Configuration configuration, String sessionId, String integrationType) throws JSONException {
        this(context, new SamsungPayPartnerInfoBuilder()
                .setConfiguration(configuration)
                .setSessionId(sessionId)
                .setIntegrationType(integrationType)
                .build());
    }

    private SamsungPayInternalClient(Context context, PartnerInfo partnerInfo) {
        this(new SamsungPay(context, partnerInfo), new PaymentManager(context, partnerInfo));
    }

    @VisibleForTesting
    SamsungPayInternalClient(SamsungPay samsungPay, PaymentManager paymentManager) {
        this.samsungPay = samsungPay;
        this.paymentManager = paymentManager;
    }

    public void goToSamsungPayUpdatePage() {
        samsungPay.goToUpdatePage();
    }

    public void activateSamsungPay() {
        samsungPay.activateSamsungPay();
    }

    void getSamsungPayStatus(final GetSamsungPayStatusCallback callback) {
        samsungPay.getSamsungPayStatus(new StatusListener() {
            @Override
            public void onSuccess(int statusCode, Bundle bundle) {
                callback.onResult(statusCode, null);
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
                List<SpaySdk.Brand> result = new ArrayList<>();
                if (cardInfos != null) {
                    for (CardInfo cardInfo : cardInfos) {
                        result.add(cardInfo.getBrand());
                    }
                }
                callback.onResult(result, null);
            }

            @Override
            public void onFailure(int errorCode, Bundle bundle) {
                Exception error = new SamsungPayException(errorCode);
                callback.onResult(null, error);
            }
        });
    }

    public void startSamsungPay(CustomSheetPaymentInfo customSheetPaymentInfo, final StartSamsungPayCallback callback) {
        paymentManager.startInAppPayWithCustomSheet(customSheetPaymentInfo, new PaymentManager.CustomSheetTransactionInfoListener() {
            @Override
            public void onCardInfoUpdated(CardInfo cardInfo, CustomSheet customSheet) {
                // TODO: notify merchant card info updated
            }

            @Override
            public void onSuccess(CustomSheetPaymentInfo customSheetPaymentInfo, String s, Bundle bundle) {
                try {
                    JSONObject json = new JSONObject(s);
                    SamsungPayNonce samsungPayNonce = SamsungPayNonce.fromJSON(json);
                    callback.onResult(samsungPayNonce, null);
                } catch (JSONException e) {
                    callback.onResult(null, e);
                }
            }

            @Override
            public void onFailure(int errorCode, Bundle bundle) {
                if (errorCode == SpaySdk.ERROR_USER_CANCELED) {
                    UserCanceledException userCanceledError = new UserCanceledException("User Canceled");
                    callback.onResult(null, userCanceledError);
                } else {
                    SamsungPayException samsungPayError = new SamsungPayException(errorCode);
                    callback.onResult(null, samsungPayError);
                }
            }
        });
    }
}
