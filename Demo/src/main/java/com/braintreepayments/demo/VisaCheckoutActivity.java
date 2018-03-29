package com.braintreepayments.demo;

import android.content.Intent;
import android.os.Bundle;

import com.braintreepayments.api.BraintreeFragment;
import com.braintreepayments.api.VisaCheckout;
import com.braintreepayments.api.exceptions.InvalidArgumentException;
import com.braintreepayments.api.interfaces.BraintreeResponseListener;
import com.braintreepayments.api.models.PaymentMethodNonce;
import com.braintreepayments.api.models.VisaCheckoutAddress;
import com.braintreepayments.api.models.VisaCheckoutNonce;
import com.visa.checkout.Profile.ProfileBuilder;
import com.visa.checkout.PurchaseInfo;
import com.visa.checkout.PurchaseInfo.PurchaseInfoBuilder;
import com.visa.checkout.VisaCheckoutSdk;
import com.visa.checkout.VisaCheckoutSdk.Status;
import com.visa.checkout.VisaCheckoutSdkInitListener;
import com.visa.checkout.widget.VisaCheckoutButton;
import com.visa.checkout.widget.VisaCheckoutButton.CheckoutWithVisaListener;

import java.math.BigDecimal;

public class VisaCheckoutActivity extends BaseActivity implements BraintreeResponseListener<ProfileBuilder>,
        VisaCheckoutSdkInitListener, CheckoutWithVisaListener {

    private VisaCheckoutButton mVisaPaymentButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.visa_checkout_activity);
        setUpAsBack();

        mVisaPaymentButton = (VisaCheckoutButton) findViewById(R.id.visa_checkout_button);
        mVisaPaymentButton.setCheckoutListener(this);
    }

    @Override
    protected void onAuthorizationFetched() {
        try {
            mBraintreeFragment = BraintreeFragment.newInstance(this, mAuthorization);
        } catch (InvalidArgumentException e) {
            onError(e);
        }

        VisaCheckout.createProfileBuilder(mBraintreeFragment, this);
    }

    @Override
    public void onPaymentMethodNonceCreated(PaymentMethodNonce paymentMethodNonce) {
        super.onPaymentMethodNonceCreated(paymentMethodNonce);

        Intent intent = new Intent()
                .putExtra(MainActivity.EXTRA_PAYMENT_RESULT, paymentMethodNonce);
        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    protected void reset() { }

    @Override
    public void onResponse(ProfileBuilder profileBuilder) {
        VisaCheckoutSdk.init(getApplicationContext(), profileBuilder.build(), this);
    }

    @Override
    public void status(int code, String message) {
        switch (code) {
            case Status.INVALID_API_KEY:
            case Status.UNSUPPORTED_SDK_VERSION:
            case Status.OS_VERSION_NOT_SUPPORTED:
            case Status.FAIL_TO_INITIALIZE:
            case Status.MISSING_PARAMETER:
            case Status.INTERNAL_ERROR:
                onError(new Exception("Visa Checkout: " + code + " " + message));
                mVisaPaymentButton.setEnabled(false);
        }
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

    @Override
    public void onClick() {
        setProgressBarIndeterminateVisibility(true);

        PurchaseInfoBuilder purchaseInfo = new PurchaseInfoBuilder(new BigDecimal("1.00"), PurchaseInfo.Currency.USD)
                .setDescription("Description");
        VisaCheckout.authorize(mBraintreeFragment, purchaseInfo);
    }
}
