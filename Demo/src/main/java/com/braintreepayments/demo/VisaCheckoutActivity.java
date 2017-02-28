package com.braintreepayments.demo;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;

import com.braintreepayments.api.BraintreeFragment;
import com.braintreepayments.api.VisaCheckout;
import com.braintreepayments.api.exceptions.InvalidArgumentException;
import com.braintreepayments.api.interfaces.BraintreeResponseListener;
import com.braintreepayments.api.models.PaymentMethodNonce;
import com.visa.checkout.Profile.ProfileBuilder;
import com.visa.checkout.PurchaseInfo;
import com.visa.checkout.PurchaseInfo.PurchaseInfoBuilder;
import com.visa.checkout.PurchaseInfo.UserReviewAction;
import com.visa.checkout.VisaCheckoutSdk;
import com.visa.checkout.VisaCheckoutSdkInitListener;

import java.math.BigDecimal;

public class VisaCheckoutActivity extends BaseActivity implements OnClickListener,
        BraintreeResponseListener<ProfileBuilder>, VisaCheckoutSdkInitListener {

    private View mVisaPaymentButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.visa_checkout_activity);
        mVisaPaymentButton = findViewById(R.id.visa_checkout_button);
        mVisaPaymentButton.setOnClickListener(this);
    }

    @Override
    protected void onAuthorizationFetched() {
        try {
            mBraintreeFragment = BraintreeFragment.newInstance(this, mAuthorization);
        } catch (InvalidArgumentException e) {
            e.printStackTrace();
        }

        VisaCheckout.createProfileBuilder(mBraintreeFragment, this);
    }

    @Override
    public void onClick(View view) {
        PurchaseInfoBuilder purchaseInfo= new PurchaseInfoBuilder(new BigDecimal("12.34"),
                PurchaseInfo.Currency.USD)
                .setUserReviewAction(UserReviewAction.PAY)
                .setDescription("Description")
                .setOrderId("order-id")
                .setMerchantRequestId("merchant-request-id");

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
    protected void reset() {}

    @Override
    public void onResponse(ProfileBuilder profileBuilder) {
        profileBuilder.setDisplayName("My app");

        VisaCheckoutSdk.init(VisaCheckoutActivity.this.getApplicationContext(), profileBuilder.build(), this);
    }

    @Override
    public void status(int code, String message) {
        Log.d("Visa Checkout", code + " " + message);
    }
}
