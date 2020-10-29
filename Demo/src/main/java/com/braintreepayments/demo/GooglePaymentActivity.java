package com.braintreepayments.demo;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;

import com.braintreepayments.api.BraintreeFragment;
import com.braintreepayments.api.GooglePayCapabilities;
import com.braintreepayments.api.GooglePayment;
import com.braintreepayments.api.exceptions.InvalidArgumentException;
import com.braintreepayments.api.interfaces.BraintreeResponseListener;
import com.braintreepayments.api.interfaces.ConfigurationListener;
import com.braintreepayments.api.models.Configuration;
import com.braintreepayments.api.models.GooglePaymentCardNonce;
import com.braintreepayments.api.models.GooglePaymentConfiguration;
import com.braintreepayments.api.models.GooglePaymentRequest;
import com.braintreepayments.api.models.PaymentMethodNonce;
import com.braintreepayments.api.models.PostalAddress;
import com.google.android.gms.wallet.ShippingAddressRequirements;
import com.google.android.gms.wallet.TransactionInfo;
import com.google.android.gms.wallet.WalletConstants;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

public class GooglePaymentActivity extends BaseActivity implements ConfigurationListener {

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
    protected void onAuthorizationFetched() {
        try {
            mBraintreeFragment = BraintreeFragment.newInstance(this, mAuthorization);
        } catch (InvalidArgumentException e) {
            onError(e);
        }
    }

    @Override
    public void onConfigurationFetched(Configuration configuration) {
        if (GooglePayCapabilities.isGooglePayEnabled(this, configuration.getGooglePayment())) {
            GooglePayment.isReadyToPay(mBraintreeFragment, new BraintreeResponseListener<Boolean>() {
                @Override
                public void onResponse(Boolean isReadyToPay) {
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

        GooglePayment.requestPayment(mBraintreeFragment, googlePaymentRequest);
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
