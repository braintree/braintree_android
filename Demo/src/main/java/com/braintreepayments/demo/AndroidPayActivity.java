package com.braintreepayments.demo;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;

import com.braintreepayments.api.AndroidPay;
import com.braintreepayments.api.BraintreeFragment;
import com.braintreepayments.api.exceptions.InvalidArgumentException;
import com.braintreepayments.api.interfaces.BraintreeResponseListener;
import com.braintreepayments.api.interfaces.ConfigurationListener;
import com.braintreepayments.api.models.AndroidPayCardNonce;
import com.braintreepayments.api.models.Configuration;
import com.braintreepayments.api.models.PaymentMethodNonce;
import com.google.android.gms.identity.intents.model.CountrySpecification;
import com.google.android.gms.identity.intents.model.UserAddress;
import com.google.android.gms.wallet.Cart;

import java.util.ArrayList;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

public class AndroidPayActivity extends BaseActivity implements ConfigurationListener {

    private Cart mCart;

    private ImageButton mAndroidPayButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.android_pay_activity);
        setUpAsBack();

        mCart = getIntent().getParcelableExtra(MainActivity.EXTRA_ANDROID_PAY_CART);
        mAndroidPayButton = (ImageButton) findViewById(R.id.android_pay_button);
    }

    @Override
    protected void reset() {
        mAndroidPayButton.setVisibility(GONE);
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
            AndroidPay.isReadyToPay(mBraintreeFragment, new BraintreeResponseListener<Boolean>() {
                @Override
                public void onResponse(Boolean isReadyToPay) {
                    if (isReadyToPay) {
                        mAndroidPayButton.setVisibility(VISIBLE);
                    } else {
                        showDialog("There are no cards set up in the Android Pay app. Please add a card to the " +
                                "Android Pay app and try again.");
                    }
                }
            });
        } else {
            showDialog("Android Pay is not available. The following issues could be the cause:\n\n" +
                    "Android Pay is not enabled for the current merchant.\n\n" +
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

    public void launchAndroidPay(View v) {
        setProgressBarIndeterminateVisibility(true);

        ArrayList<CountrySpecification> allowedCountries = new ArrayList<>();
        for (String country : Settings.getAndroidPayAllowedCountriesForShipping(this)) {
            allowedCountries.add(new CountrySpecification(country));
        }

        AndroidPay.requestAndroidPay(mBraintreeFragment, mCart, Settings.isAndroidPayShippingAddressRequired(this),
                Settings.isAndroidPayPhoneNumberRequired(this), allowedCountries);
    }

    public static String getDisplayString(AndroidPayCardNonce nonce) {
        return "Underlying Card Last Two: " + nonce.getLastTwo() + "\n" +
                "Email: " + nonce.getEmail() + "\n" +
                "Billing address: " + formatAddress(nonce.getBillingAddress()) + "\n" +
                "Shipping address: " + formatAddress(nonce.getShippingAddress());
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
                address.getCountryCode();
    }
}
