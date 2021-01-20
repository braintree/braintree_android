package com.braintreepayments.demo;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.annotation.Nullable;

import com.braintreepayments.api.BraintreeClient;
import com.braintreepayments.api.BrowserSwitchCallback;
import com.braintreepayments.api.BrowserSwitchException;
import com.braintreepayments.api.BrowserSwitchResult;
import com.braintreepayments.api.LocalPaymentBrowserSwitchResultCallback;
import com.braintreepayments.api.LocalPaymentClient;
import com.braintreepayments.api.LocalPaymentNonce;
import com.braintreepayments.api.LocalPaymentRequest;
import com.braintreepayments.api.LocalPaymentStartCallback;
import com.braintreepayments.api.LocalPaymentTransaction;
import com.braintreepayments.api.exceptions.InvalidArgumentException;
import com.braintreepayments.api.interfaces.PaymentMethodNonceCreatedListener;
import com.braintreepayments.api.models.Authorization;
import com.braintreepayments.api.models.BraintreeRequestCodes;
import com.braintreepayments.api.models.PostalAddress;

import org.json.JSONException;

public class LocalPaymentActivity extends BaseActivity implements
    BrowserSwitchCallback,
    PaymentMethodNonceCreatedListener {

    private Button mIdealButton;
    private LocalPaymentClient localPaymentClient;

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
            handleError(new Exception("To use this feature, enable the \"Sandbox\" environment."));
            return;
        }

        try {
            Authorization authorization = Authorization.fromString(mAuthorization);
            BraintreeClient braintreeClient = new BraintreeClient(authorization, this, RETURN_URL_SCHEME);
            localPaymentClient = new LocalPaymentClient(braintreeClient);

            mIdealButton.setEnabled(true);
        } catch (InvalidArgumentException e) {
            e.printStackTrace();
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

        localPaymentClient.startPayment(request, new LocalPaymentStartCallback() {
            @Override
            public void onResult(@Nullable LocalPaymentTransaction transaction, @Nullable Exception error) {
                if (transaction != null) {
                    try {
                        localPaymentClient.approveTransaction(LocalPaymentActivity.this, transaction);
                    } catch (JSONException | BrowserSwitchException e) {
                        e.printStackTrace();
                    }
                }

                if (error != null) {
                    onBraintreeError(error);
                }
            }
        });
    }

    protected void handleLocalPaymentResult(LocalPaymentNonce localPaymentNonce, Exception error) {
        super.onPaymentMethodNonceCreated(localPaymentNonce);

        Intent intent = new Intent().putExtra(MainActivity.EXTRA_PAYMENT_RESULT, localPaymentNonce);
        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    public void onResult(int requestCode, BrowserSwitchResult browserSwitchResult, @Nullable Uri uri) {
        if (requestCode != BraintreeRequestCodes.LOCAL_PAYMENT) {
            return;
        }
        localPaymentClient.onBrowserSwitchResult(this, browserSwitchResult, uri, new LocalPaymentBrowserSwitchResultCallback() {
            @Override
            public void onResult(@Nullable LocalPaymentNonce localPaymentNonce, @Nullable Exception error) {
                handleLocalPaymentResult(localPaymentNonce, error);
            }
        });
    }

    public static String getDisplayString(LocalPaymentNonce nonce) {
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
