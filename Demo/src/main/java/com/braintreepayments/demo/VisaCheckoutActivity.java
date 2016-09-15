package com.braintreepayments.demo;

import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;

import com.braintreepayments.api.BraintreeFragment;
import com.braintreepayments.api.VisaCheckout;
import com.braintreepayments.api.exceptions.InvalidArgumentException;
import com.braintreepayments.api.models.PaymentMethodNonce;
import com.visa.checkout.VisaPaymentInfo;

public class VisaCheckoutActivity extends BaseActivity implements OnClickListener {

    private View mVisaCheckoutButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.visa_checkout_activity);
        mVisaCheckoutButton = findViewById(R.id.visa_checkout_button);
        mVisaCheckoutButton.setOnClickListener(this);
    }

    @Override
    protected void reset() {

    }

    @Override
    protected void onAuthorizationFetched() {
        try {
            mBraintreeFragment = BraintreeFragment.newInstance(this, mAuthorization);
        } catch (InvalidArgumentException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onClick(View view) {
        VisaPaymentInfo visaPaymentInfo = new VisaPaymentInfo();
        VisaCheckout.authorize(mBraintreeFragment, visaPaymentInfo);
    }

    @Override
    public void onPaymentMethodNonceCreated(PaymentMethodNonce paymentMethodNonce) {
        super.onPaymentMethodNonceCreated(paymentMethodNonce);
    }
}
