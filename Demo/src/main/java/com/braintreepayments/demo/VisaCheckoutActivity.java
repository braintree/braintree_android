package com.braintreepayments.demo;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;

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

import java.math.BigDecimal;

public class VisaCheckoutActivity extends BaseActivity implements OnClickListener,
        BraintreeResponseListener<ProfileBuilder>, VisaCheckoutSdkInitListener {

    private View mVisaPaymentButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.visa_checkout_activity);
        setUpAsBack();

        mVisaPaymentButton = findViewById(R.id.visa_checkout_button);
        mVisaPaymentButton.setOnClickListener(this);
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
    public void onClick(View view) {
        setProgressBarIndeterminateVisibility(true);

        PurchaseInfoBuilder purchaseInfo = new PurchaseInfoBuilder(new BigDecimal("1.00"), PurchaseInfo.Currency.USD)
                .setDescription("Description");
        VisaCheckout.authorize(mBraintreeFragment, purchaseInfo);
    }

    @Override
    public void onPaymentMethodNonceCreated(PaymentMethodNonce paymentMethodNonce) {
        super.onPaymentMethodNonceCreated(paymentMethodNonce);

        Intent intent = new Intent()
                .putExtra(MainActivity.EXTRA_PAYMENT_METHOD_NONCE, paymentMethodNonce);
        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    protected void reset() {
        mVisaPaymentButton.setEnabled(false);
    }

    @Override
    public void onResponse(ProfileBuilder profileBuilder) {
        VisaCheckoutSdk.init(getApplicationContext(), profileBuilder.build(), this);
    }

    @Override
    public void status(int code, String message) {
        if (code != Status.SUCCESS) {
            onError(new Exception("Visa Checkout: " + code + " " + message));
            mVisaPaymentButton.setEnabled(false);
        } else {
            mVisaPaymentButton.setEnabled(true);
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
                "Bin Data: \n"  +
                "         - Prepaid: " + nonce.getBinData().getHealthcare() + "\n" +
                "         - Healthcare: " + nonce.getBinData().getHealthcare() + "\n" +
                "         - Debit: " + nonce.getBinData().getDebit() + "\n" +
                "         - Durbin Regulated: " + nonce.getBinData().getDurbinRegulated() + "\n" +
                "         - Commercial: " + nonce.getBinData().getCommercial() + "\n" +
                "         - Payroll: " + nonce.getBinData().getPayroll() + "\n" +
                "         - Issuing Bank: " + nonce.getBinData().getIssuingBank() + "\n" +
                "         - Country of Issuance: " + nonce.getBinData().getCountryOfIssuance() + "\n" +
                "         - Product Id: " + nonce.getBinData().getProductId();
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
