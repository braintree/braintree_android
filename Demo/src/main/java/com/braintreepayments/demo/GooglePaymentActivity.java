package com.braintreepayments.demo;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import com.braintreepayments.api.Configuration;
import com.braintreepayments.api.ConfigurationCallback;
import com.braintreepayments.api.GooglePayCapabilities;
import com.braintreepayments.api.GooglePaymentCardNonce;
import com.braintreepayments.api.GooglePaymentIsReadyToPayCallback;
import com.braintreepayments.api.GooglePaymentRequest;
import com.braintreepayments.api.GooglePaymentRequestPaymentCallback;
import com.braintreepayments.api.PaymentMethodNonce;
import com.braintreepayments.api.PostalAddress;
import com.google.android.gms.wallet.ShippingAddressRequirements;
import com.google.android.gms.wallet.TransactionInfo;
import com.google.android.gms.wallet.WalletConstants;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

public class GooglePaymentActivity extends BaseActivity {

    private ImageButton mGooglePaymentButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.google_payment_activity);
        setUpAsBack();

        mGooglePaymentButton = findViewById(R.id.google_payment_button);
    }

    @Override
    protected void reset() {
        mGooglePaymentButton.setVisibility(GONE);
    }

    @Override
    protected void onBraintreeInitialized() {

        final FragmentActivity activity = this;
        getConfiguration(new ConfigurationCallback() {
            @Override
            public void onResult(@Nullable Configuration configuration, @Nullable Exception error) {

                if (GooglePayCapabilities.isGooglePayEnabled(activity, configuration.getGooglePayment())) {

                    googlePayIsReadyToPay(null, new GooglePaymentIsReadyToPayCallback() {
                        @Override
                        public void onResult(Boolean isReadyToPay, Exception e) {
                            if (isReadyToPay) {
                                mGooglePaymentButton.setVisibility(VISIBLE);
                            } else {
                                showDialog("Google Payments are not available. The following issues could be the cause:\n\n" +
                                        "No user is logged in to the device.\n\n" +
                                        "Google Play Services is missing or out of date.");
                            }
                        }
                    });
                } else {
                    showDialog("Google Payments are not available. The following issues could be the cause:\n\n" +
                            "Google Payments are not enabled for the current merchant.\n\n" +
                            "Google Play Services is missing or out of date.");
                }
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

    public void launchGooglePayment(View v) {
        setProgressBarIndeterminateVisibility(true);

        GooglePaymentRequest googlePaymentRequest = new GooglePaymentRequest()
                .transactionInfo(TransactionInfo.newBuilder()
                        .setCurrencyCode(Settings.getGooglePaymentCurrency(this))
                        .setTotalPrice("1.00")
                        .setTotalPriceStatus(WalletConstants.TOTAL_PRICE_STATUS_FINAL)
                        .build())
                .allowPrepaidCards(Settings.areGooglePaymentPrepaidCardsAllowed(this))
                .billingAddressFormat(WalletConstants.BILLING_ADDRESS_FORMAT_FULL)
                .billingAddressRequired(Settings.isGooglePaymentBillingAddressRequired(this))
                .emailRequired(Settings.isGooglePaymentEmailRequired(this))
                .phoneNumberRequired(Settings.isGooglePaymentPhoneNumberRequired(this))
                .shippingAddressRequired(Settings.isGooglePaymentShippingAddressRequired(this))
                .shippingAddressRequirements(ShippingAddressRequirements.newBuilder()
                        .addAllowedCountryCodes(Settings.getGooglePaymentAllowedCountriesForShipping(this))
                        .build())
                .googleMerchantId(Settings.getGooglePaymentMerchantId(this));

        googleRequestPayment(googlePaymentRequest, new GooglePaymentRequestPaymentCallback() {
            @Override
            public void onResult(boolean b, Exception e) {
                if (e != null) {
                    handleError(e);
                }
            }
        });
    }

    public static String getDisplayString(GooglePaymentCardNonce nonce) {
        return "Underlying Card Last Two: " + nonce.getLastTwo() + "\n" +
                "Card Description: " + nonce.getDescription() + "\n" +
                "Email: " + nonce.getEmail() + "\n" +
                "Billing address: " + formatAddress(nonce.getBillingAddress()) + "\n" +
                "Shipping address: " + formatAddress(nonce.getShippingAddress()) + "\n" +
                getDisplayString(nonce.getBinData());
    }

    private static String formatAddress(PostalAddress address) {
        if (address == null) {
            return "null";
        }

        return address.getRecipientName() + " " +
                address.getStreetAddress() + " " +
                address.getExtendedAddress() + " " +
                address.getLocality() + " " +
                address.getRegion() + " " +
                address.getPostalCode() + " " +
                address.getSortingCode() + " " +
                address.getCountryCodeAlpha2() + " " +
                address.getPhoneNumber();
    }
}
