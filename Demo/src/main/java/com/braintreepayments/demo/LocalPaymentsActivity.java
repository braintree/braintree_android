package com.braintreepayments.demo;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.annotation.Nullable;

import com.braintreepayments.api.BrowserSwitchException;
import com.braintreepayments.api.LocalPaymentStartCallback;
import com.braintreepayments.api.LocalPaymentTransaction;
import com.braintreepayments.api.interfaces.PaymentMethodNonceCreatedListener;
import com.braintreepayments.api.models.LocalPaymentRequest;
import com.braintreepayments.api.models.LocalPaymentResult;
import com.braintreepayments.api.models.PostalAddress;

import org.json.JSONException;

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
    protected void onBraintreeInitialized() {
        if (!Settings.SANDBOX_ENV_NAME.equals(Settings.getEnvironment(this))) {
            handleError(new Exception("To use this feature, enable the \"Sandbox\" environment."));
            return;
        }
        mIdealButton.setEnabled(true);
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

        startLocalPayment(request, new LocalPaymentStartCallback() {
            @Override
            public void onResult(@Nullable LocalPaymentTransaction transaction, @Nullable Exception error) {
                try {
                    approveLocalPayment(transaction);
                } catch (JSONException | BrowserSwitchException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    protected void onLocalPaymentResult(LocalPaymentResult localPaymentResult, Exception error) {
        super.onPaymentMethodNonceCreated(localPaymentResult);

        Intent intent = new Intent().putExtra(MainActivity.EXTRA_PAYMENT_RESULT, localPaymentResult);
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
