package com.braintreepayments.demo;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;

import com.braintreepayments.api.BraintreeFragment;
import com.braintreepayments.api.GooglePayments;
import com.braintreepayments.api.exceptions.InvalidArgumentException;
import com.braintreepayments.api.interfaces.BraintreeResponseListener;
import com.braintreepayments.api.interfaces.ConfigurationListener;
import com.braintreepayments.api.models.Configuration;
import com.braintreepayments.api.models.GooglePaymentsCardNonce;
import com.braintreepayments.api.models.GooglePaymentsRequest;
import com.braintreepayments.api.models.PaymentMethodNonce;
import com.google.android.gms.identity.intents.model.UserAddress;
import com.google.android.gms.wallet.ShippingAddressRequirements;
import com.google.android.gms.wallet.TransactionInfo;
import com.google.android.gms.wallet.WalletConstants;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

public class GooglePaymentsActivity extends BaseActivity implements ConfigurationListener {

    private ImageButton mGooglePaymentsButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.google_payments_activity);
        setUpAsBack();

        mGooglePaymentsButton = findViewById(R.id.google_payments_button);
    }

    @Override
    protected void reset() {
        mGooglePaymentsButton.setVisibility(GONE);
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
        if (configuration.getAndroidPay().isEnabled(this)) {
            GooglePayments.isReadyToPay(mBraintreeFragment, new BraintreeResponseListener<Boolean>() {
                @Override
                public void onResponse(Boolean isReadyToPay) {
                    if (isReadyToPay) {
                        mGooglePaymentsButton.setVisibility(VISIBLE);
                    } else {
                        showDialog("There are no cards set up in Google Payments or the Android Pay app." +
                                "Please add a card and try again.");
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

        Intent intent = new Intent().putExtra(MainActivity.EXTRA_PAYMENT_METHOD_NONCE, paymentMethodNonce);
        setResult(RESULT_OK, intent);
        finish();
    }

    public void launchGooglePayments(View v) {
        setProgressBarIndeterminateVisibility(true);

        GooglePaymentsRequest googlePaymentsRequest = new GooglePaymentsRequest()
                .transactionInfo(TransactionInfo.newBuilder()
                        .setCurrencyCode(Settings.getGooglePaymentsCurrency(this))
                        .setTotalPrice("1.00")
                        .setTotalPriceStatus(WalletConstants.TOTAL_PRICE_STATUS_FINAL)
                        .build())
                .allowPrepaidCards(Settings.areGooglePaymentsPrepaidCardsAllowed(this))
                .billingAddressFormat(WalletConstants.BILLING_ADDRESS_FORMAT_FULL)
                .billingAddressRequired(Settings.isGooglePaymentsBillingAddressRequired(this))
                .emailRequired(Settings.isGooglePaymentsEmailRequired(this))
                .phoneNumberRequired(Settings.isGooglePaymentsPhoneNumberRequired(this))
                .shippingAddressRequired(Settings.isGooglePaymentsShippingAddressRequired(this))
                .shippingAddressRequirements(ShippingAddressRequirements.newBuilder()
                        .addAllowedCountryCodes(Settings.getGooglePaymentsAllowedCountriesForShipping(this))
                        .build())
                .uiRequired(true);

        GooglePayments.requestPayment(mBraintreeFragment, googlePaymentsRequest);
    }

    public static String getDisplayString(GooglePaymentsCardNonce nonce) {
        return "Underlying Card Last Two: " + nonce.getLastTwo() + "\n" +
                "Email: " + nonce.getEmail() + "\n" +
                "Billing address: " + formatAddress(nonce.getBillingAddress()) + "\n" +
                "Shipping address: " + formatAddress(nonce.getShippingAddress()) + "\n" +
                getDisplayString(nonce.getBinData());
    }

    private static String formatAddress(UserAddress address) {
        if (address == null) {
            return "null";
        }

        return address.getName() + " " +
                address.getAddress1() + " " +
                address.getAddress2() + " " +
                address.getAddress3() + " " +
                address.getAddress4() + " " +
                address.getAddress5() + " " +
                address.getLocality() + " " +
                address.getAdministrativeArea() + " " +
                address.getPostalCode() + " " +
                address.getSortingCode() + " " +
                address.getCountryCode() + " " +
                address.getPhoneNumber();
    }
}
