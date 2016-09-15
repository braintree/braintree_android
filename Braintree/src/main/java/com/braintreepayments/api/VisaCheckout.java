package com.braintreepayments.api;

import android.content.Intent;
import android.os.Parcel;
import android.os.Parcelable;

import com.braintreepayments.api.interfaces.ConfigurationListener;
import com.braintreepayments.api.interfaces.PaymentMethodNonceCallback;
import com.braintreepayments.api.models.Configuration;
import com.braintreepayments.api.models.PaymentMethodBuilder;
import com.braintreepayments.api.models.PaymentMethodNonce;
import com.visa.checkout.VisaLibrary;
import com.visa.checkout.VisaMcomLibrary;
import com.visa.checkout.VisaMerchantInfo;
import com.visa.checkout.VisaPaymentInfo;
import com.visa.checkout.VisaPaymentSummary;
import com.visa.checkout.utils.VisaEnvironmentConfig;

import org.json.JSONException;
import org.json.JSONObject;

public class VisaCheckout {
    public static final int VISA_CHECKOUT_REQUEST_CODE = 12345; // TODO better code
    public static boolean isEnabled() {
        try {
            Class.forName("com.visa.checkout.VisaPaymentInfo");
            VisaPaymentInfo visaPaymentInfo;
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public static void authorize(final BraintreeFragment braintreeFragment, final VisaPaymentInfo visaPaymentInfo) {
        braintreeFragment.waitForConfiguration(new ConfigurationListener() {
            @Override
            public void onConfigurationFetched(Configuration configuration) {
                // Modify with Braintree configuration
                VisaMerchantInfo visaMerchantInfo = visaPaymentInfo.getVisaMerchantInfo();
                if (visaMerchantInfo == null) {
                    visaMerchantInfo = new VisaMerchantInfo();
                }

                visaMerchantInfo.setMerchantApiKey("SNNBESUFWC5EEDCQYAE513SbyYkeVDWiEGSWOnOugTCi96fxY");
                visaMerchantInfo.setDisplayName("Braintree");
                visaPaymentInfo.setVisaMerchantInfo(visaMerchantInfo);

                // Start Visa Payment Activity
                // Wait for braintreeFragment.onActivityResult

                VisaEnvironmentConfig visaEnvironmentConfig = VisaEnvironmentConfig.SANDBOX;
                visaEnvironmentConfig.setMerchantApiKey("SNNBESUFWC5EEDCQYAE513SbyYkeVDWiEGSWOnOugTCi96fxY");
                visaEnvironmentConfig.setVisaCheckoutRequestCode(VISA_CHECKOUT_REQUEST_CODE);

                VisaMcomLibrary visaMcomLibrary = VisaMcomLibrary.getLibrary(braintreeFragment.getActivity(),
                        visaEnvironmentConfig);

                // Problem! They start their own activity
                visaMcomLibrary.checkoutWithPayment(visaPaymentInfo, VISA_CHECKOUT_REQUEST_CODE);
            }
        });

    }

    protected static void onActivityResult(BraintreeFragment braintreeFragment, int resultCode, Intent data) {
        // Process data
        VisaPaymentSummary visaPaymentSummary = data.getParcelableExtra(VisaLibrary.PAYMENT_SUMMARY);
        tokenize(braintreeFragment, visaPaymentSummary);
    }

    private static void tokenize(final BraintreeFragment braintreeFragment, final VisaPaymentSummary visaPaymentSummary) {
        braintreeFragment.waitForConfiguration(new ConfigurationListener() {
            @Override
            public void onConfigurationFetched(Configuration configuration) {
                TokenizationClient.tokenize(braintreeFragment, new VisaCheckoutPaymentBuilder(visaPaymentSummary),
                        new PaymentMethodNonceCallback() {
                            @Override
                            public void success(PaymentMethodNonce paymentMethodNonce) {
                                // TODO analytics
                                braintreeFragment.postCallback(paymentMethodNonce);
                            }

                            @Override
                            public void failure(Exception exception) {
                                // TODO analytics
                                braintreeFragment.postCallback(exception);
                            }
                        });
            }
        });
    }

    private static class VisaCheckoutPaymentMethodNonce extends PaymentMethodNonce implements Parcelable {

        protected VisaCheckoutPaymentMethodNonce(Parcel in) {
            super(in);
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
        }

        @Override
        public int describeContents() {
            return 0;
        }

        public static final Creator<VisaCheckoutPaymentMethodNonce> CREATOR =
                new Creator<VisaCheckoutPaymentMethodNonce>() {
                    @Override
                    public VisaCheckoutPaymentMethodNonce createFromParcel(Parcel in) {
                        return new VisaCheckoutPaymentMethodNonce(in);
                    }

                    @Override
                    public VisaCheckoutPaymentMethodNonce[] newArray(int size) {
                        return new VisaCheckoutPaymentMethodNonce[size];
                    }
                };

        @Override
        public String getTypeLabel() {
            return "Visa Checkout";
        }
    }

    private static class VisaCheckoutPaymentBuilder extends PaymentMethodBuilder {

        public VisaCheckoutPaymentBuilder(VisaPaymentSummary visaPaymentSummary) {

        }

        @Override
        protected void build(JSONObject base, JSONObject paymentMethodNonceJson) throws JSONException {

        }

        @Override
        public String getApiPath() {
            return null;
        }

        @Override
        public String getResponsePaymentMethodType() {
            return null;
        }
    }
}
