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

    private final BraintreeClient braintreeClient;

    @VisibleForTesting
    SamsungPayInternalClient internalClient;

    @VisibleForTesting
    SamsungPayClient(BraintreeClient braintreeClient) {
        this.braintreeClient = braintreeClient;
    }

    public void goToUpdatePage() {
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

    public void activateSamsungPay() {
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

    public void isReadyToPay(final SamsungIsReadyToPayCallback callback) {
        getSamsungPayStatus(new GetSamsungPayStatusCallback() {
            @Override
            public void onResult(@Nullable Integer status, @Nullable Exception error) {
                if (status != null) {
                    if (status == SPAY_READY) {
                        getBraintreeSupportedSamsungPayCards(new GetAcceptedCardBrandsCallback() {
                            @Override
                            public void onResult(@Nullable List<SpaySdk.Brand> acceptedCardBrands, @Nullable Exception error) {
                                if (acceptedCardBrands != null) {
                                    boolean isReadyToPay = !acceptedCardBrands.isEmpty();

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

    private void getBraintreeSupportedSamsungPayCards(final GetAcceptedCardBrandsCallback callback) {
        getInternalClient(new GetSamsungPayInternalClientCallback() {
            @Override
            public void onResult(@Nullable final SamsungPayInternalClient internalClient, @Nullable Exception error) {
                if (internalClient != null) {
                    internalClient.getAcceptedCardBrands(callback);
                } else {
                    callback.onResult(null, error);
                }
            }
        });
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

    private void getInternalClient(final GetSamsungPayInternalClientCallback callback) {
        if (internalClient != null) {
            callback.onResult(internalClient, null);
        } else {
            braintreeClient.getConfiguration(new ConfigurationCallback() {
                @Override
                public void onResult(@Nullable Configuration configuration, @Nullable Exception error) {
                    if (configuration != null) {
                        try {
                            internalClient =
                                new SamsungPayInternalClient(braintreeClient, configuration);
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
}
