package com.braintreepayments.demo;

import android.content.Intent;
import android.os.Bundle;

import com.braintreepayments.api.VisaCheckoutButton;
import com.braintreepayments.api.VisaCheckoutCreateProfileBuilderCallback;
import com.braintreepayments.api.VisaCheckoutTokenizeCallback;
import com.braintreepayments.api.PaymentMethodNonce;
import com.braintreepayments.api.VisaCheckoutAddress;
import com.braintreepayments.api.VisaCheckoutNonce;
import com.visa.checkout.Profile.ProfileBuilder;
import com.visa.checkout.PurchaseInfo;
import com.visa.checkout.PurchaseInfo.PurchaseInfoBuilder;
import com.visa.checkout.VisaCheckoutSdk;
import com.visa.checkout.VisaPaymentSummary;

import java.math.BigDecimal;

public class VisaCheckoutActivity extends BaseActivity {

    private VisaCheckoutButton mVisaPaymentButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.visa_checkout_activity);
        setUpAsBack();

        mVisaPaymentButton = findViewById(R.id.visa_checkout_button);
    }

    @Override
    protected void onBraintreeInitialized() {

        createVisaCheckoutProfile(new VisaCheckoutCreateProfileBuilderCallback() {
            @Override
            public void onResult(ProfileBuilder profileBuilder, Exception e) {
                PurchaseInfoBuilder purchaseInfo = new PurchaseInfoBuilder(new BigDecimal("1.00"), PurchaseInfo.Currency.USD)
                        .setDescription("Description");

                mVisaPaymentButton.init(VisaCheckoutActivity.this, profileBuilder,
                        purchaseInfo, new VisaCheckoutSdk.VisaCheckoutResultListener() {
                            @Override
                            public void onButtonClick(LaunchReadyHandler launchReadyHandler) {
                                launchReadyHandler.launch();
                            }

                            @Override
                            public void onResult(VisaPaymentSummary visaPaymentSummary) {
                                tokenizeVisaCheckout(visaPaymentSummary, new VisaCheckoutTokenizeCallback() {
                                    @Override
                                    public void onResult(PaymentMethodNonce paymentMethodNonce, Exception e) {
                                        handlePaymentMethodNonceCreated(paymentMethodNonce);
                                    }
                                });
                            }
                        });
            }
        });
    }

    private void handlePaymentMethodNonceCreated(PaymentMethodNonce paymentMethodNonce) {
        super.onPaymentMethodNonceCreated(paymentMethodNonce);
        Intent intent = new Intent()
                .putExtra(MainActivity.EXTRA_PAYMENT_RESULT, paymentMethodNonce);
        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    protected void reset() {
    }

    public static String getDisplayString(VisaCheckoutNonce nonce) {
        return "User data\n" +
                "First name: " + nonce.getUserData().getUserFirstName() + "\n" +
                "Last name: " + nonce.getUserData().getUserLastName() + "\n" +
                "Full name: " + nonce.getUserData().getUserFullName() + "\n" +
                "User name: " + nonce.getUserData().getUsername() + "\n" +
                "Email: " + nonce.getUserData().getUserEmail() + "\n" +
                "Billing Address: " + formatAddress(nonce.getBillingAddress()) + "\n" +
                "Shipping Address: " + formatAddress(nonce.getShippingAddress()) + "\n" +
                getDisplayString(nonce.getBinData());
    }

    private static String formatAddress(VisaCheckoutAddress address) {
        return address.getFirstName() + " " +
                address.getLastName() + " " +
                address.getStreetAddress() + " " +
                address.getExtendedAddress() + " " +
                address.getLocality() + " " +
                address.getPostalCode() + " " +
                address.getRegion() + " " +
                address.getCountryCode() + " " +
                address.getPhoneNumber();
    }
}
