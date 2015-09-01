package com.braintreepayments.demo;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Html;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.braintreepayments.api.BraintreeFragment;
import com.braintreepayments.api.BraintreePaymentActivity;
import com.braintreepayments.api.PayPal;
import com.braintreepayments.api.exceptions.InvalidArgumentException;
import com.braintreepayments.api.interfaces.PaymentMethodCreatedListener;
import com.braintreepayments.api.models.PayPalAccount;
import com.braintreepayments.api.models.PayPalCheckout;
import com.braintreepayments.api.models.PaymentMethod;
import com.braintreepayments.api.models.PostalAddress;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class PayPalActivity extends Activity implements PaymentMethodCreatedListener {

    private static final long WAIT_TIME = TimeUnit.SECONDS.toMillis(15);
    private static final long ONE_SECOND = 1000;

    private TextView mLog;
    private Button mBillingAgreementButton;
    private Button mFuturePaymentAddressScopeButton;
    private Button mFuturePaymentButton;
    private Button mSinglePaymentButton;
    private Button mCancelButton;

    private BraintreeFragment mBraintreeFragment;

    private CountDownTimer mCountDownTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.paypal);

        Bundle extras = getIntent().getExtras();
        String extraClientToken = extras.getString(BraintreePaymentActivity.EXTRA_CLIENT_TOKEN);
        // Connect Views
        mLog = (TextView) findViewById(R.id.log);
        mBillingAgreementButton = (Button) findViewById(R.id.paypal_billing_agreement_button);
        mFuturePaymentAddressScopeButton =
                (Button) findViewById(R.id.paypal_future_payment_address_scope_button);
        mFuturePaymentButton = (Button) findViewById(R.id.paypal_future_payment_button);
        mSinglePaymentButton = (Button) findViewById(R.id.paypal_single_payment_button);
        mCancelButton = (Button) findViewById(R.id.cancel_button);

        // Initialize Views
        enableButtons(true);

        try {
            mBraintreeFragment = BraintreeFragment.newInstance(this, extraClientToken);
        } catch (InvalidArgumentException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onPause() {
        if (mCountDownTimer != null) {
            mCountDownTimer.cancel();
            mCountDownTimer = null;
        }
        super.onPause();
    }

    /**
     * Sets all the button states.
     */
    private void enableButtons(boolean enabled) {
        mBillingAgreementButton.setEnabled(enabled);
        mFuturePaymentAddressScopeButton.setEnabled(enabled);
        mFuturePaymentButton.setEnabled(enabled);
        mSinglePaymentButton.setEnabled(enabled);
    }

    /**
     * Kicks off a PayPal future payment.
     */
    public void launchFuturePayment(View v) {
        PayPal.authorize(mBraintreeFragment);
    }

    /**
     * Kicks off a PayPal Future Payment (Address Scope).
     */
    public void launchFuturePaymentAddressScope(View v) {
        List<String> additionalScopes = Collections.singletonList(PayPal.SCOPE_ADDRESS);
        PayPal.authorize(mBraintreeFragment, additionalScopes);
    }

    /**
     * Kicks off a PayPal Single Payment.
     */
    public void launchSinglePayment(View v) {
        PayPalCheckout checkout = new PayPalCheckout(new BigDecimal(1.5));
        PayPal.checkout(mBraintreeFragment, checkout);
    }

    /**
     * Kicks off a PayPal Billing Agreement.
     */
    public void launchBillingAgreement(View v) {
        PayPalCheckout checkout = new PayPalCheckout();
        PayPal.billingAgreement(mBraintreeFragment, checkout);
    }

    @Override
    public void onPaymentMethodCreated(final PaymentMethod paymentMethod) {
        logPaymentMethod(paymentMethod);
        mCancelButton.setEnabled(true);
        mCountDownTimer = new CountDownTimer(WAIT_TIME, ONE_SECOND) {

            @Override
            public void onTick(long millisUntilFinished) {
                int secondsLeft = (int) (millisUntilFinished / ONE_SECOND);
                updateFinishButton(secondsLeft);
            }

            @Override
            public void onFinish() {
                finish();
            }
        };
        mCountDownTimer.start();
        returnPaymentMethod(paymentMethod);
    }

    private void logPaymentMethod(PaymentMethod paymentMethod) {
        String log = "";
        String logFormat = "<b>%s</b>: %s<br>";

        PayPalAccount paypalAccount = (PayPalAccount) paymentMethod;
        log += String.format(logFormat, "Nonce", paymentMethod.getNonce());

        PostalAddress billingAddress = paypalAccount.getBillingAddress();
        if (billingAddress != null) {
            log += String.format(logFormat, "Address", billingAddress.toString());
        }
        PostalAddress shippingAddress = paypalAccount.getShippingAddress();
        if (shippingAddress != null) {
            log += String.format(logFormat, "Shipping", shippingAddress.toString());
        }
        mLog.setText(Html.fromHtml(log));
    }

    private void updateFinishButton(int secondsLeft) {
        mCancelButton.setText(String.format("Cancel [Cont. in %d]", secondsLeft));
    }

    private void returnPaymentMethod(PaymentMethod paymentMethod) {
        Intent intent = new Intent().putExtra(BraintreePaymentActivity.EXTRA_PAYMENT_METHOD,
                paymentMethod);
        setResult(RESULT_OK, intent);
    }

    public void cancelFinish(View v) {
        mCountDownTimer.cancel();
        mCancelButton.setEnabled(true);
        mCancelButton.setText("Continue");
        mCancelButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    /**
     * @return An intent that will launch a PayPalActivity.
     */
    public static Intent createIntent(Context context) {
        return new Intent(context, PayPalActivity.class);
    }
}
