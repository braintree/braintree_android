package com.braintreepayments.api;

import android.app.Activity;
import android.content.Intent;
import android.text.TextUtils;

import com.braintreepayments.api.exceptions.BraintreeException;
import com.braintreepayments.api.exceptions.ConfigurationException;
import com.braintreepayments.api.interfaces.ConfigurationListener;
import com.braintreepayments.api.interfaces.PaymentMethodNonceCallback;
import com.braintreepayments.api.models.BraintreeRequestCodes;
import com.braintreepayments.api.models.Configuration;
import com.braintreepayments.api.models.PaymentMethodNonce;
import com.braintreepayments.api.models.VisaCheckoutConfiguration;
import com.braintreepayments.api.models.VisaCheckoutPaymentBuilder;
import com.visa.checkout.VisaLibrary;
import com.visa.checkout.VisaMcomLibrary;
import com.visa.checkout.VisaMerchantInfo;
import com.visa.checkout.VisaMerchantInfo.MerchantDataLevel;
import com.visa.checkout.VisaPaymentInfo;
import com.visa.checkout.VisaPaymentSummary;
import com.visa.checkout.utils.VisaEnvironmentConfig;

public class VisaCheckout {
    public static void createVisaCheckoutLibrary(final BraintreeFragment braintreeFragment) {
        if (!VisaCheckoutConfiguration.isVisaPackageAvailable()) {
            braintreeFragment.postCallback(new ConfigurationException("Visa Checkout SDK is not available"));
            return;
        }
        braintreeFragment.waitForConfiguration(new ConfigurationListener() {
            @Override
            public void onConfigurationFetched(Configuration configuration) {
                VisaCheckoutConfiguration visaCheckoutConfiguration = configuration.getVisaCheckout();

                if (!visaCheckoutConfiguration.isEnabled()) {
                    braintreeFragment.postCallback(new ConfigurationException("Visa Checkout is not enabled."));
                    return;
                }

                VisaEnvironmentConfig visaEnvironmentConfig = VisaEnvironmentConfig.SANDBOX;

                if ("production".equals(configuration.getEnvironment())) {
                    visaEnvironmentConfig = VisaEnvironmentConfig.PRODUCTION;
                }

                visaEnvironmentConfig.setMerchantApiKey(configuration.getVisaCheckout().getApiKey());
                visaEnvironmentConfig.setVisaCheckoutRequestCode(BraintreeRequestCodes.VISA_CHECKOUT);

                VisaMcomLibrary visaMcomLibrary = VisaMcomLibrary.getLibrary(braintreeFragment.getActivity(),
                        visaEnvironmentConfig);
                BraintreeVisaCheckoutResultActivity.sVisaEnvironmentConfig = visaEnvironmentConfig;
                braintreeFragment.postVisaCheckoutLibraryCallback(visaMcomLibrary);
            }
        });

    }

    public static void authorize(final BraintreeFragment braintreeFragment, final VisaMcomLibrary visaMcomLibrary, final VisaPaymentInfo visaPaymentInfo) {
        braintreeFragment.waitForConfiguration(new ConfigurationListener() {

            @Override
            public void onConfigurationFetched(Configuration configuration) {
                VisaMerchantInfo visaMerchantInfo = visaPaymentInfo.getVisaMerchantInfo();
                if (visaMerchantInfo == null) {
                    visaMerchantInfo = new VisaMerchantInfo();
                }

                if (TextUtils.isEmpty(visaMerchantInfo.getMerchantApiKey())) {
                    visaMerchantInfo.setMerchantApiKey(configuration.getVisaCheckout().getApiKey());
                }

                if (TextUtils.isEmpty(visaPaymentInfo.getExternalClientId())) {
                    visaPaymentInfo.setExternalClientId(configuration.getVisaCheckout().getExternalClientId());
                }

                visaMerchantInfo.setDataLevel(MerchantDataLevel.FULL);

                visaPaymentInfo.setVisaMerchantInfo(visaMerchantInfo);
                BraintreeVisaCheckoutResultActivity.sVisaPaymentInfo = visaPaymentInfo;

                Intent visaCheckoutResultActivity = new Intent(braintreeFragment.getActivity(),
                        BraintreeVisaCheckoutResultActivity.class);
                braintreeFragment.startActivityForResult(visaCheckoutResultActivity,
                        BraintreeRequestCodes.VISA_CHECKOUT);
            }
        });
    }

    static void onActivityResult(BraintreeFragment braintreeFragment, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_CANCELED) {
            braintreeFragment.postCancelCallback(BraintreeRequestCodes.VISA_CHECKOUT);
            braintreeFragment.sendAnalyticsEvent("visacheckout.activityresult.canceled");
        } else if (resultCode == Activity.RESULT_OK && data != null) {
            VisaPaymentSummary visaPaymentSummary = data.getParcelableExtra(VisaLibrary.PAYMENT_SUMMARY);
            tokenize(braintreeFragment, visaPaymentSummary);
            braintreeFragment.sendAnalyticsEvent("visacheckout.activityresult.ok");
        } else {
            braintreeFragment.postCallback(new BraintreeException("Visa Checkout responded with resultCode=" + resultCode));
            braintreeFragment.sendAnalyticsEvent("visacheckout.activityresult.failed");
        }
    }

    static void tokenize(final BraintreeFragment braintreeFragment, final VisaPaymentSummary visaPaymentSummary) {
        braintreeFragment.waitForConfiguration(new ConfigurationListener() {
            @Override
            public void onConfigurationFetched(Configuration configuration) {
                TokenizationClient.tokenize(braintreeFragment, new VisaCheckoutPaymentBuilder(visaPaymentSummary),
                        new PaymentMethodNonceCallback() {
                            @Override
                            public void success(PaymentMethodNonce paymentMethodNonce) {
                                braintreeFragment.postCallback(paymentMethodNonce);
                                braintreeFragment.sendAnalyticsEvent("visacheckout.tokenize.succeeded");
                            }

                            @Override
                            public void failure(Exception exception) {
                                braintreeFragment.postCallback(exception);
                                braintreeFragment.sendAnalyticsEvent("visacheckout.tokenize.failed");
                            }
                        });
            }
        });
    }
}
