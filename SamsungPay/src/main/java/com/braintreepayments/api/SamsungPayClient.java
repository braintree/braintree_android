package com.braintreepayments.api;

import android.content.Context;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.samsung.android.sdk.samsungpay.v2.SpaySdk;
import com.samsung.android.sdk.samsungpay.v2.payment.CustomSheetPaymentInfo;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.samsung.android.sdk.samsungpay.v2.SpaySdk.SPAY_NOT_READY;
import static com.samsung.android.sdk.samsungpay.v2.SpaySdk.SPAY_NOT_SUPPORTED;
import static com.samsung.android.sdk.samsungpay.v2.SpaySdk.SPAY_READY;

public class SamsungPayClient {

    private static final String BRAINTREE_TOKENIZATION_API_VERSION = "2018-10-01";

    private final ClassHelper classHelper;
    private final BraintreeClient braintreeClient;
    private SamsungPayInternalClient internalClient;

    @VisibleForTesting
    SamsungPayClient(BraintreeClient braintreeClient, ClassHelper classHelper) {
        this.braintreeClient = braintreeClient;
        this.classHelper = classHelper;
    }

    private void getInternalClient(final GetSamsungPayInternalClientCallback callback) {
        if (internalClient != null) {
            callback.onResult(internalClient, null);
        } else {
            braintreeClient.getConfiguration(new ConfigurationCallback() {
                @Override
                public void onResult(@Nullable Configuration configuration, @Nullable Exception error) {
                    if (configuration != null) {
                        Context context = braintreeClient.getApplicationContext();
                        String sessionId = braintreeClient.getSessionId();
                        String integrationType = braintreeClient.getIntegrationType();
                        try {
                            internalClient =
                                    new SamsungPayInternalClient(context, configuration, sessionId, integrationType);
                            callback.onResult(internalClient, null);
                        } catch (JSONException e) {
                            callback.onResult(null, e);
                        }
                    } else {
                        callback.onResult(null, error);
                    }
                }
            });
        }
    }

    public void goToUpdatePage(final Context context) {
        getInternalClient(new GetSamsungPayInternalClientCallback() {
            @Override
            public void onResult(@Nullable SamsungPayInternalClient internalClient, @Nullable Exception error) {
                if (internalClient != null) {
                    internalClient.goToSamsungPayUpdatePage();
                    braintreeClient.sendAnalyticsEvent("samsung-pay.goto-update-page");
                } else {
                    // TODO: determine if we should notify an error here
                }
            }
        });
    }

    public void activateSamsungPay(Context context) {
        getInternalClient(new GetSamsungPayInternalClientCallback() {
            @Override
            public void onResult(@Nullable SamsungPayInternalClient internalClient, @Nullable Exception error) {
                if (internalClient != null) {
                    internalClient.activateSamsungPay();
                    braintreeClient.sendAnalyticsEvent("samsung-pay.activate-samsung-pay");
                } else {
                    // TODO: determine if we should notify an error here
                }
            }
        });
    }

    private void getSamsungPayStatus(final GetSamsungPayStatusCallback callback) {
        getInternalClient(new GetSamsungPayInternalClientCallback() {
            @Override
            public void onResult(@Nullable SamsungPayInternalClient internalClient, @Nullable Exception error) {
                if (internalClient != null) {
                    internalClient.getSamsungPayStatus(callback);
                } else {
                    callback.onResult(null, error);
                }
            }
        });
    }

    private void getBraintreeSupportedSamsungPayCards(final GetBraintreeSupportedSamsungPayCards callback) {
        getInternalClient(new GetSamsungPayInternalClientCallback() {
            @Override
            public void onResult(@Nullable final SamsungPayInternalClient internalClient, @Nullable Exception error) {
                if (internalClient != null) {
                    braintreeClient.getConfiguration(new ConfigurationCallback() {
                        @Override
                        public void onResult(@Nullable final Configuration configuration, @Nullable Exception error) {
                            if (configuration != null) {
                                internalClient.getAcceptedCardBrands(new GetAcceptedCardBrandsCallback() {
                                    @Override
                                    public void onResult(@Nullable List<SpaySdk.Brand> spayAcceptedCardBrands, @Nullable Exception error) {
                                        if (spayAcceptedCardBrands != null) {
                                            Set<SpaySdk.Brand> braintreeAcceptedCardBrands =
                                                    filterAcceptedCardBrands(configuration.getSupportedCardTypes());
                                            Set<SpaySdk.Brand> intersection = new HashSet<>(spayAcceptedCardBrands);
                                            intersection.retainAll(braintreeAcceptedCardBrands);

                                            List<SpaySdk.Brand> result = new ArrayList<>(intersection);
                                            callback.onResult(result, null);

                                        } else {
                                            callback.onResult(null, error);
                                        }
                                    }
                                });
                            } else {
                                callback.onResult(null, error);
                            }
                        }
                    });
                } else {
                    callback.onResult(null, error);
                }
            }
        });
    }

    public void isReadyToPay(final Context context, final SamsungIsReadyToPayCallback callback) {
        if (isSamsungPayAvailable()) {
            getSamsungPayStatus(new GetSamsungPayStatusCallback() {
                @Override
                public void onResult(@Nullable Integer status, @Nullable Exception error) {
                    if (status != null) {
                        if (status == SPAY_READY) {
                            getBraintreeSupportedSamsungPayCards(new GetBraintreeSupportedSamsungPayCards() {
                                @Override
                                public void onResult(@Nullable List<SpaySdk.Brand> braintreeSupportedSamsungPayCards, @Nullable Exception error) {
                                    if (braintreeSupportedSamsungPayCards != null) {
                                        boolean isReadyToPay = !braintreeSupportedSamsungPayCards.isEmpty();

                                        if (isReadyToPay) {
                                            callback.onResult(true, null);
                                        } else {
                                            braintreeClient.sendAnalyticsEvent("samsung-pay.request-card-info.no-supported-cards-in-wallet");
                                            //listener.onResponse(SamsungPayAvailability(SPAY_NOT_READY, SPAY_NO_SUPPORTED_CARDS_IN_WALLET))
                                            callback.onResult(false, null);
                                        }
                                    } else {
                                        callback.onResult(false, error);
                                    }
                                }
                            });
                        } else {
                            switch (status) {
                                case SPAY_NOT_READY:
                                    braintreeClient.sendAnalyticsEvent("samsung-pay.is-ready-to-pay.not-ready");
                                    break;
                                case SPAY_NOT_SUPPORTED:
                                    braintreeClient.sendAnalyticsEvent("samsung-pay.is-ready-to-pay.device-not-supported");
                                    break;
                            }
                            callback.onResult(false, null);
                        }
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

    public void startSamsungPay(final CustomSheetPaymentInfo paymentInfo, final StartSamsungPayCallback callback) {
        getInternalClient(new GetSamsungPayInternalClientCallback() {
            @Override
            public void onResult(@Nullable SamsungPayInternalClient internalClient, @Nullable Exception error) {
                if (internalClient != null) {
                    internalClient.startSamsungPay(paymentInfo, callback);
                } else {
                    callback.onResult(null, error);
                }
            }
        });
    }

    public boolean isSamsungPayAvailable() {
        return classHelper.isClassAvailable("com.samsung.android.sdk.samsungpay.v2.SamsungPay");
    }

    private Set<SpaySdk.Brand> filterAcceptedCardBrands(List<String> braintreeAcceptedCardBrands) {
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
