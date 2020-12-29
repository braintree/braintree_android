package com.braintreepayments.demo;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.annotation.Nullable;

import com.braintreepayments.api.BraintreeDataCollectorCallback;
import com.braintreepayments.api.ConfigurationCallback;
import com.braintreepayments.api.PayPalRequestCallback;
import com.braintreepayments.api.interfaces.PaymentMethodNonceCreatedListener;
import com.braintreepayments.api.models.Configuration;
import com.braintreepayments.api.models.PayPalAccountNonce;
import com.braintreepayments.api.models.PayPalRequest;
import com.braintreepayments.api.models.PaymentMethodNonce;
import com.braintreepayments.api.models.PostalAddress;

import java.util.Arrays;
import java.util.List;

public class PayPalActivity extends BaseActivity implements PaymentMethodNonceCreatedListener {

    private String mDeviceData;

    private Button mBillingAgreementButton;
    private Button mSinglePaymentButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.paypal_activity);
        setUpAsBack();

        mBillingAgreementButton = findViewById(R.id.paypal_billing_agreement_button);
        mSinglePaymentButton = findViewById(R.id.paypal_single_payment_button);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void reset() {
        enableButtons(false);
    }

    @Override
    protected void onBraintreeInitialized() {
        getConfiguration(new ConfigurationCallback() {
            @Override
            public void onResult(@Nullable Configuration configuration, @Nullable Exception error) {
                if (getIntent().getBooleanExtra(MainActivity.EXTRA_COLLECT_DEVICE_DATA, false)) {
                    collectDeviceData(new BraintreeDataCollectorCallback() {
                        @Override
                        public void onResult(@Nullable String deviceData, @Nullable Exception error) {
                            mDeviceData = deviceData;
                        }
                    });
                }
            }
        });
        enableButtons(true);
    }

    private void enableButtons(boolean enabled) {
        mBillingAgreementButton.setEnabled(enabled);
        mSinglePaymentButton.setEnabled(enabled);
    }

    public void launchSinglePayment(View v) {
        setProgressBarIndeterminateVisibility(true);
        requestPayPalOneTimePayment(getPayPalRequest("1.00"), new PayPalRequestCallback() {
            @Override
            public void onResult(boolean requestInitiated, Exception error) {
                if (error != null) {
                    onBraintreeError(error);
                }
            }
        });
    }

    public void launchBillingAgreement(View v) {
        setProgressBarIndeterminateVisibility(true);
        requestPayPalBillingAgreement(getPayPalRequest(null), new PayPalRequestCallback() {
            @Override
            public void onResult(boolean requestInitiated, Exception error) {
                if (error != null) {
                    onBraintreeError(error);
                }
            }
        });
    }

    @Override
    protected void onPayPalResult(PaymentMethodNonce paymentMethodNonce, Exception error) {
        if (paymentMethodNonce != null) {
            super.onPaymentMethodNonceCreated(paymentMethodNonce);

            Intent intent = new Intent()
                    .putExtra(MainActivity.EXTRA_PAYMENT_RESULT, paymentMethodNonce)
                    .putExtra(MainActivity.EXTRA_DEVICE_DATA, mDeviceData);
            setResult(RESULT_OK, intent);
        }
        finish();
    }

    private PayPalRequest getPayPalRequest(@Nullable String amount) {
        PayPalRequest request = new PayPalRequest(amount);

        request.displayName(Settings.getPayPalDisplayName(this));

        String landingPageType = Settings.getPayPalLandingPageType(this);
        if (getString(R.string.paypal_landing_page_type_billing).equals(landingPageType)) {
            request.landingPageType(PayPalRequest.LANDING_PAGE_TYPE_BILLING);
        } else if (getString(R.string.paypal_landing_page_type_login).equals(landingPageType)) {
            request.landingPageType(PayPalRequest.LANDING_PAGE_TYPE_LOGIN);
        }

        String intentType = Settings.getPayPalIntentType(this);
        if (intentType.equals(getString(R.string.paypal_intent_authorize))) {
            request.intent(PayPalRequest.INTENT_AUTHORIZE);
        } else if (intentType.equals(getString(R.string.paypal_intent_order))) {
            request.intent(PayPalRequest.INTENT_ORDER);
        } else if (intentType.equals(getString(R.string.paypal_intent_sale))) {
            request.intent(PayPalRequest.INTENT_SALE);
        }

        if (Settings.isPayPalUseractionCommitEnabled(this)) {
            request.userAction(PayPalRequest.USER_ACTION_COMMIT);
        }

        if (Settings.isPayPalCreditOffered(this)) {
            request.offerCredit(true);
        }

        if (Settings.usePayPalAddressOverride(this)) {
            request.shippingAddressOverride(new PostalAddress()
                    .recipientName("Brian Tree")
                    .streetAddress("123 Fake Street")
                    .extendedAddress("Floor A")
                    .locality("San Francisco")
                    .region("CA")
                    .countryCodeAlpha2("US")
            );
        }

        return request;
    }

    public static String getDisplayString(PayPalAccountNonce nonce) {
        return "First name: " + nonce.getFirstName() + "\n" +
                "Last name: " + nonce.getLastName() + "\n" +
                "Email: " + nonce.getEmail() + "\n" +
                "Phone: " + nonce.getPhone() + "\n" +
                "Payer id: " + nonce.getPayerId() + "\n" +
                "Client metadata id: " + nonce.getClientMetadataId() + "\n" +
                "Billing address: " + formatAddress(nonce.getBillingAddress()) + "\n" +
                "Shipping address: " + formatAddress(nonce.getShippingAddress());
    }

    private static String formatAddress(PostalAddress address) {
        String addressString = "";
        List<String> addresses = Arrays.asList(
                address.getRecipientName(),
                address.getStreetAddress(),
                address.getExtendedAddress(),
                address.getLocality(),
                address.getRegion(),
                address.getPostalCode(),
                address.getCountryCodeAlpha2()
        );

        for (String line : addresses) {
            if (line == null) {
                addressString += "null";
            } else {
                addressString += line;
            }
            addressString += " ";
        }

        return addressString;
    }
}
