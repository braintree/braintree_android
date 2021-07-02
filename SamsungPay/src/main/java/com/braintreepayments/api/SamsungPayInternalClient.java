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
        this.braintreeAcceptedCardBrands = filterAcceptedCardBrands(configuration.getSupportedCardTypes());
        this.samsungPay = samsungPay;
        this.paymentManager = paymentManager;
    }

    void goToSamsungPayUpdatePage() {
        samsungPay.goToUpdatePage();
    }

    void activateSamsungPay() {
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

    void startSamsungPay(CustomSheetPaymentInfo customSheetPaymentInfo, final SamsungPayStartListener listener) {
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
                    listener.onSamsungPayStartSuccess(samsungPayNonce);
                } catch (JSONException e) {
                    listener.onSamsungPayStartError(e);
                }
            }

            @Override
            public void onFailure(int errorCode, Bundle bundle) {
                if (errorCode == SpaySdk.ERROR_USER_CANCELED) {
                    UserCanceledException userCanceledError = new UserCanceledException("User Canceled");
                    listener.onSamsungPayStartError(userCanceledError);
                } else {
                    SamsungPayException samsungPayError = new SamsungPayException(errorCode);
                    listener.onSamsungPayStartError(samsungPayError);
                }
            }
        });
    }

    private static Set<SpaySdk.Brand> filterAcceptedCardBrands(List<String> braintreeAcceptedCardBrands) {
        List<SpaySdk.Brand> result = new ArrayList<>();

        for (String brand : braintreeAcceptedCardBrands) {
            switch (brand.toLowerCase()) {
                case "visa":
                    result.add(SpaySdk.Brand.VISA);
                    break;
                case "mastercard":
                    result.add(SpaySdk.Brand.MASTERCARD);
                    break;
                case "discover":
                    result.add(SpaySdk.Brand.DISCOVER);
                    break;
                case "american_express":
                    result.add(SpaySdk.Brand.AMERICANEXPRESS);
                    break;
            }
        }
        return new HashSet<>(result);
    }
}
