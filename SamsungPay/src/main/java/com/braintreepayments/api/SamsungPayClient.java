package com.braintreepayments.api;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.samsung.android.sdk.samsungpay.v2.PartnerInfo;
import com.samsung.android.sdk.samsungpay.v2.SamsungPay;
import com.samsung.android.sdk.samsungpay.v2.SpaySdk;
import com.samsung.android.sdk.samsungpay.v2.Status;
import com.samsung.android.sdk.samsungpay.v2.StatusListener;
import com.samsung.android.sdk.samsungpay.v2.payment.CardInfo;
import com.samsung.android.sdk.samsungpay.v2.payment.PaymentManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.samsung.android.sdk.samsungpay.v2.SpaySdk.PARTNER_SERVICE_TYPE;
import static com.samsung.android.sdk.samsungpay.v2.SpaySdk.SPAY_NOT_READY;
import static com.samsung.android.sdk.samsungpay.v2.SpaySdk.SPAY_NOT_SUPPORTED;
import static com.samsung.android.sdk.samsungpay.v2.SpaySdk.SPAY_READY;
import static com.samsung.android.sdk.samsungpay.v2.SpaySdk.ServiceType.INAPP_PAYMENT;
import static com.samsung.android.sdk.samsungpay.v2.payment.PaymentManager.EXTRA_KEY_TEST_MODE;

public class SamsungPayClient {

    private static final String BRAINTREE_TOKENIZATION_API_VERSION = "2018-10-01";

    private final ClassHelper classHelper;
    private final BraintreeClient braintreeClient;

    @VisibleForTesting
    SamsungPayClient(BraintreeClient braintreeClient, ClassHelper classHelper) {
        this.braintreeClient = braintreeClient;
        this.classHelper = classHelper;
    }

    public void goToUpdatePage(final Context context) {
        getSamsungPay(context, new GetSamsungPayCallback() {
            @Override
            public void onResult(@Nullable SamsungPay samsungPay, @Nullable Exception error) {
                if (samsungPay != null) {
                    samsungPay.goToUpdatePage();
                    braintreeClient.sendAnalyticsEvent("samsung-pay.goto-update-page");
                }
            }
        });
    }

    public void activateSamsungPay(Context context) {
        getSamsungPay(context, new GetSamsungPayCallback() {
            @Override
            public void onResult(@Nullable SamsungPay samsungPay, @Nullable Exception error) {
                if (samsungPay != null) {
                    samsungPay.activateSamsungPay();
                    braintreeClient.sendAnalyticsEvent("samsung-pay.activate-samsung-pay");
                }
            }
        });
    }

    public void isReadyToPay(final Context context, final SamsungIsReadyToPayCallback callback) {
        if (isSamsungPayAvailable()) {
            getSamsungPay(context, new GetSamsungPayCallback() {
                @Override
                public void onResult(@Nullable SamsungPay samsungPay, @Nullable Exception error) {
                    if (samsungPay != null) {
                        samsungPay.getSamsungPayStatus(new StatusListener() {
                            @Override
                            public void onSuccess(int status, Bundle bundle) {

                                if (status == SPAY_READY) {
                                    getPaymentManager(context, new GetPaymentManagerCallback() {
                                        @Override
                                        public void onResult(@Nullable PaymentManager paymentManager, @Nullable Exception error) {
                                            if (paymentManager != null) {

                                                paymentManager.requestCardInfo(new Bundle(), new PaymentManager.CardInfoListener() {
                                                    @Override
                                                    public void onResult(final List<CardInfo> cardInfos) {
                                                        if (cardInfos != null) {
                                                            braintreeClient.getConfiguration(new ConfigurationCallback() {
                                                                @Override
                                                                public void onResult(@Nullable Configuration configuration, @Nullable Exception error) {
                                                                    Set<SpaySdk.Brand> braintreeAcceptedCardBrands = filterAcceptedCardBrands(configuration.getSupportedCardTypes());
                                                                    Set<SpaySdk.Brand> spayAcceptedCardBrands = new HashSet<>();
                                                                    for (CardInfo cardInfo: cardInfos) {
                                                                        spayAcceptedCardBrands.add(cardInfo.getBrand());
                                                                    }
                                                                    spayAcceptedCardBrands.retainAll(braintreeAcceptedCardBrands);
                                                                    boolean isReadyToPay = !spayAcceptedCardBrands.isEmpty();

                                                                    if (isReadyToPay) {
                                                                        callback.onResult(true, null);
                                                                    } else {
                                                                        braintreeClient.sendAnalyticsEvent("samsung-pay.request-card-info.no-supported-cards-in-wallet")
//                                                                    listener.onResponse(SamsungPayAvailability(SPAY_NOT_READY, SPAY_NO_SUPPORTED_CARDS_IN_WALLET))
                                                                        callback.onResult(false, null);
                                                                    }
                                                                }
                                                            });

                                                        } else {
//                                                        callback.onResult(SamsungPayAvailability(SPAY_NOT_READY, SPAY_NO_SUPPORTED_CARDS_IN_WALLET));
                                                            callback.onResult(false, null);
                                                            braintreeClient.sendAnalyticsEvent("samsung-pay.request-card-info.no-supported-cards-in-wallet");
                                                        }
                                                    }

                                                    @Override
                                                    public void onFailure(int errorCode, Bundle bundle) {
                                                        Exception error = new SamsungPayException(errorCode);
                                                        callback.onResult(false, error);
                                                        braintreeClient.sendAnalyticsEvent("samsung-pay.request-card-info.failed");
                                                    }
                                                });
                                            } else {
                                                callback.onResult(false, error);
                                            }
                                        }
                                    });

                                } else {
                                    switch (status) {
                                        case SPAY_NOT_SUPPORTED:
                                            braintreeClient.sendAnalyticsEvent("samsung-pay.is-ready-to-pay.device-not-supported");
                                            break;
                                        case SPAY_NOT_READY:
                                            braintreeClient.sendAnalyticsEvent("samsung-pay.is-ready-to-pay.not-ready");
                                            break;
                                    }
                                    callback.onResult(false, null);
                                }
                            }

                            @Override
                            public void onFail(int errorCode, Bundle bundle) {
                                braintreeClient.sendAnalyticsEvent("samsung-pay.is-ready-to-pay.failed");
                                SamsungPayException error = new SamsungPayException(i);
                                callback.onResult(false, error);
                            }
                        });
                    } else {
                        callback.onResult(false, error);
                    }
                }
            });

        } else {
            callback.onResult(false, null);
            braintreeClient.sendAnalyticsEvent("samsung-pay.is-ready-to-pay.samsung-pay-class-unavailable");
        }
    }

    public void createPaymentInfo() {

    }

    public void createPaymentManager() {

    }

    public void requestPayment() {

    }

    public boolean isSamsungPayAvailable() {
        return classHelper.isClassAvailable("com.samsung.android.sdk.samsungpay.v2.SamsungPay");
    }

    private Set<SpaySdk.Brand> filterAcceptedCardBrands(List<String> braintreeAcceptedCardBrands) {
        List<SpaySdk.Brand> result = new ArrayList<>();

        for (String brand: braintreeAcceptedCardBrands) {
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

    private void getPaymentManager(final Context context, final GetPaymentManagerCallback callback) {
        braintreeClient.getConfiguration(new ConfigurationCallback() {
            @Override
            public void onResult(@Nullable Configuration configuration, @Nullable Exception error) {
                if (configuration != null) {
                    Bundle bundle = new Bundle();
                    bundle.putString(PARTNER_SERVICE_TYPE, INAPP_PAYMENT.toString());

                    boolean isTestEnvironment = false;
                    String samsungPayEnvironment = configuration.getSamsungPayEnvironment();
                    if (samsungPayEnvironment != null) {
                        isTestEnvironment = samsungPayEnvironment.equalsIgnoreCase("SANDBOX");
                    }
                    bundle.putBoolean(EXTRA_KEY_TEST_MODE, isTestEnvironment);

                    JSONObject clientSdkMetadata = new MetadataBuilder()
                            .integration(braintreeClient.getIntegrationType())
                            .sessionId(braintreeClient.getSessionId())
                            .version()
                            .build();

                    JSONObject additionalData = new JSONObject();
                    try {
                        additionalData.put("braintreeTokenizationApiVersion", BRAINTREE_TOKENIZATION_API_VERSION);
                        additionalData.put("clientSdkMetadata", clientSdkMetadata);
                    } catch (JSONException e) {
                        callback.onResult(null, e);
                    }
                    bundle.putString("additionalData", additionalData.toString());

                    String serviceId = configuration.getSamsungPayServiceId();
                    PartnerInfo partnerInfo = new PartnerInfo(serviceId, bundle);
                    PaymentManager paymentManager = new PaymentManager(context, partnerInfo);
                    callback.onResult(paymentManager, null);
                } else {
                    callback.onResult(null, error);
                }
            }
        });
    }

    private void getSamsungPay(final Context context, final GetSamsungPayCallback callback) {
        braintreeClient.getConfiguration(new ConfigurationCallback() {
            @Override
            public void onResult(@Nullable Configuration configuration, @Nullable Exception error) {
                if (configuration != null) {
                    Bundle bundle = new Bundle();
                    bundle.putString(PARTNER_SERVICE_TYPE, INAPP_PAYMENT.toString());

                    boolean isTestEnvironment = false;
                    String samsungPayEnvironment = configuration.getSamsungPayEnvironment();
                    if (samsungPayEnvironment != null) {
                        isTestEnvironment = samsungPayEnvironment.equalsIgnoreCase("SANDBOX");
                    }
                    bundle.putBoolean(EXTRA_KEY_TEST_MODE, isTestEnvironment);

                    JSONObject clientSdkMetadata = new MetadataBuilder()
                            .integration(braintreeClient.getIntegrationType())
                            .sessionId(braintreeClient.getSessionId())
                            .version()
                            .build();

                    JSONObject additionalData = new JSONObject();
                    try {
                        additionalData.put("braintreeTokenizationApiVersion", BRAINTREE_TOKENIZATION_API_VERSION);
                        additionalData.put("clientSdkMetadata", clientSdkMetadata);
                    } catch (JSONException e) {
                        callback.onResult(null, e);
                    }
                    bundle.putString("additionalData", additionalData.toString());

                    String serviceId = configuration.getSamsungPayServiceId();
                    PartnerInfo partnerInfo = new PartnerInfo(serviceId, bundle);
                    SamsungPay samsungPay = new SamsungPay(context, partnerInfo);
                    callback.onResult(samsungPay, null);
                } else {
                    callback.onResult(null, error);
                }
            }
        });
    }
}
