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

    private final String sessionId;
    private final String integrationType;
    private final Configuration configuration;
    private final SamsungPay samsungPay;
    private final PaymentManager paymentManager;

    SamsungPayInternalClient(Context context, Configuration configuration, String sessionId, String integrationType) throws JSONException {
        this.sessionId = sessionId;
        this.configuration = configuration;
        this.integrationType = integrationType;

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

    void getSamsungPayStatus(GetSamsungPayStatusCallback callback) {
        samsungPay.getSamsungPayStatus(new StatusListener() {
            @Override
            public void onSuccess(int i, Bundle bundle) {

            }

            @Override
            public void onFail(int i, Bundle bundle) {

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

    public void startSamsungPay(CustomSheetPaymentInfo customSheetPaymentInfo) {
        paymentManager.startInAppPayWithCustomSheet(customSheetPaymentInfo, new PaymentManager.CustomSheetTransactionInfoListener() {
            @Override
            public void onCardInfoUpdated(CardInfo cardInfo, CustomSheet customSheet) {
                // TODO: notify merchant card info updated
            }

            @Override
            public void onSuccess(CustomSheetPaymentInfo customSheetPaymentInfo, String s, Bundle bundle) {
                // TODO: parse nonce
            }

            @Override
            public void onFailure(int i, Bundle bundle) {
                // TODO: notify merchant failure
            }
        });
    }
}
