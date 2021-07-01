package com.braintreepayments.api;

import android.content.Context;
import android.os.Bundle;

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

import static com.samsung.android.sdk.samsungpay.v2.SpaySdk.PARTNER_SERVICE_TYPE;
import static com.samsung.android.sdk.samsungpay.v2.SpaySdk.ServiceType.INAPP_PAYMENT;
import static com.samsung.android.sdk.samsungpay.v2.payment.PaymentManager.EXTRA_KEY_TEST_MODE;

public class SamsungPayInternalClient {

    private static final String BRAINTREE_TOKENIZATION_API_VERSION = "2018-10-01";

    private final SamsungPay samsungPay;
    private final PaymentManager paymentManager;

    SamsungPayInternalClient(Context context, Configuration configuration, String sessionId, String integrationType) throws JSONException {
//        PartnerInfo partnerInfo = new SamsungPartnerInfoBuilder()
//                .setConfiguration(configuration)
//                .setSessionId(sessionId)
//                .setIntegrationType(integrationType)
//                .build();


        Bundle bundle = new Bundle();
        bundle.putString(PARTNER_SERVICE_TYPE, INAPP_PAYMENT.toString());

        boolean isTestEnvironment = false;
        String samsungPayEnvironment = configuration.getSamsungPayEnvironment();
        if (samsungPayEnvironment != null) {
            isTestEnvironment = samsungPayEnvironment.equalsIgnoreCase("SANDBOX");
        }
        bundle.putBoolean(EXTRA_KEY_TEST_MODE, isTestEnvironment);

        JSONObject clientSdkMetadata = new MetadataBuilder()
                .integration(integrationType)
                .sessionId(sessionId)
                .version()
                .build();

        JSONObject additionalData = new JSONObject();
        additionalData.put("braintreeTokenizationApiVersion", BRAINTREE_TOKENIZATION_API_VERSION);
        additionalData.put("clientSdkMetadata", clientSdkMetadata);
        bundle.putString("additionalData", additionalData.toString());

        String serviceId = configuration.getSamsungPayServiceId();
        PartnerInfo partnerInfo = new PartnerInfo(serviceId, bundle);

        this.samsungPay = new SamsungPay(context, partnerInfo);
        this.paymentManager = new PaymentManager(context, partnerInfo);
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

    public void goToSamsungPayUpdatePage() {
        samsungPay.goToUpdatePage();
    }

    public void activateSamsungPay() {
        samsungPay.activateSamsungPay();
    }
}
