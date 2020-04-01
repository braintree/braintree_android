package com.braintreepayments.demo;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.braintreepayments.api.BraintreeFragment;
import com.braintreepayments.api.LocalPayment;
import com.braintreepayments.api.exceptions.InvalidArgumentException;
import com.braintreepayments.api.interfaces.BraintreeResponseListener;
import com.braintreepayments.api.interfaces.PaymentMethodNonceCreatedListener;
import com.braintreepayments.api.models.LocalPaymentRequest;
import com.braintreepayments.api.models.LocalPaymentResult;
import com.braintreepayments.api.models.PaymentMethodNonce;
import com.braintreepayments.api.models.PostalAddress;

public class LocalPaymentsActivity extends BaseActivity implements PaymentMethodNonceCreatedListener {

    private Button mIdealButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ideal_activity);

        mIdealButton = findViewById(R.id.ideal_button);
    }

    @Override
    protected void reset() {
        mIdealButton.setEnabled(false);
    }

    @Override
    protected void onAuthorizationFetched() {
        if (!Settings.SANDBOX_ENV_NAME.equals(Settings.getEnvironment(this))) {
            onError(new Exception("To use this feature, enable the \"Sandbox\" environment."));
            return;
        }

        try {
            mBraintreeFragment = BraintreeFragment.newInstance(this, Settings.getLocalPaymentsTokenizationKey(this));
            mIdealButton.setEnabled(true);
        } catch (InvalidArgumentException e) {
            onError(e);
        }
    }

    public void launchIdeal(View v) {
        PostalAddress address = new PostalAddress()
                .streetAddress("Stadhouderskade 78")
                .countryCodeAlpha2("NL")
                .locality("Amsterdam")
                .postalCode("1072 AE");
        LocalPaymentRequest request = new LocalPaymentRequest()
                .paymentType("ideal")
                .amount("1.10")
                .address(address)
                .phone("207215300")
                .email("android-test-buyer@paypal.com")
                .givenName("Test")
                .surname("Buyer")
                .shippingAddressRequired(true)
                .merchantAccountId("altpay_eur")
                .currencyCode("EUR");
        LocalPayment.startPayment(mBraintreeFragment, request, new BraintreeResponseListener<LocalPaymentRequest>() {
            @Override
            public void onResponse(LocalPaymentRequest localPaymentRequest) {
                LocalPayment.approvePayment(mBraintreeFragment, localPaymentRequest);
            }
        });
    }

    @Override
    public void onPaymentMethodNonceCreated(PaymentMethodNonce paymentMethodNonce) {
        super.onPaymentMethodNonceCreated(paymentMethodNonce);

        Intent intent = new Intent().putExtra(MainActivity.EXTRA_PAYMENT_RESULT, paymentMethodNonce);
        setResult(RESULT_OK, intent);
        finish();
    }

    public static String getDisplayString(LocalPaymentResult nonce) {
        return "First name: " + nonce.getGivenName() + "\n" +
                "Last name: " + nonce.getSurname() + "\n" +
                "Email: " + nonce.getEmail() + "\n" +
                "Phone: " + nonce.getPhone() + "\n" +
                "Payer id: " + nonce.getPayerId() + "\n" +
                "Client metadata id: " + nonce.getClientMetadataId() + "\n" +
                "Billing address: " + formatAddress(nonce.getBillingAddress()) + "\n" +
                "Shipping address: " + formatAddress(nonce.getShippingAddress());
    }

    private static String formatAddress(PostalAddress address) {
        return address.getRecipientName() + " " +
                address.getStreetAddress() + " " +
                address.getExtendedAddress() + " " +
                address.getLocality() + " " +
                address.getRegion() + " " +
                address.getPostalCode() + " " +
                address.getCountryCodeAlpha2();
    }
}
