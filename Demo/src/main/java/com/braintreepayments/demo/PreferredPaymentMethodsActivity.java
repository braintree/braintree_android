package com.braintreepayments.demo;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.braintreepayments.api.BraintreeFragment;
import com.braintreepayments.api.PayPal;
import com.braintreepayments.api.PreferredPaymentMethods;
import com.braintreepayments.api.Venmo;
import com.braintreepayments.api.exceptions.InvalidArgumentException;
import com.braintreepayments.api.interfaces.PreferredPaymentMethodsListener;
import com.braintreepayments.api.models.PayPalRequest;
import com.braintreepayments.api.models.PostalAddress;
import com.braintreepayments.api.models.PreferredPaymentMethodsResult;

public class PreferredPaymentMethodsActivity extends BaseActivity {

    private Button mPreferredPaymentMethodsButton;
    private TextView mPreferredPaymentMethodsTextView;
    private Button mBillingAgreementButton;
    private Button mSinglePaymentButton;
    private Button mVenmoButton;

    private BraintreeFragment mBraintreeFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.preferred_payment_methods_activity);

        mPreferredPaymentMethodsTextView = findViewById(R.id.preferred_payment_methods_text_view);
        mPreferredPaymentMethodsButton = findViewById(R.id.preferred_payment_methods_button);
        mBillingAgreementButton = findViewById(R.id.paypal_billing_agreement_button);
        mSinglePaymentButton = findViewById(R.id.paypal_single_payment_button);
        mVenmoButton = findViewById(R.id.venmo_button);
    }

    @Override
    protected void reset() {
        mPreferredPaymentMethodsTextView.setText("");
        mPreferredPaymentMethodsButton.setEnabled(false);
        mBillingAgreementButton.setEnabled(false);
        mSinglePaymentButton.setEnabled(false);
        mVenmoButton.setEnabled(false);
    }

    @Override
    protected void onAuthorizationFetched() {
        try {
            mBraintreeFragment = BraintreeFragment.newInstance(this, mAuthorization);
        } catch (InvalidArgumentException e) {
            onError(e);
        }

        mPreferredPaymentMethodsButton.setEnabled(true);
    }

    public void launchPreferredPaymentMethods(View v) {
        mPreferredPaymentMethodsTextView.setText(getString(R.string.preferred_payment_methods_progress));

        PreferredPaymentMethods.fetchPreferredPaymentMethods(mBraintreeFragment, new PreferredPaymentMethodsListener() {
            @Override
            public void onPreferredPaymentMethodsFetched(PreferredPaymentMethodsResult result) {
                mPreferredPaymentMethodsTextView.setText(String.format("PayPal Preferred: %b\nVenmo Preferred: %b",
                        result.isPayPalPreferred(),
                        result.isVenmoPreferred()));

                mBillingAgreementButton.setEnabled(result.isPayPalPreferred());
                mSinglePaymentButton.setEnabled(result.isPayPalPreferred());
                mVenmoButton.setEnabled(result.isVenmoPreferred());
            }
        });
    }

    public void launchSinglePayment(View v) {
        setProgressBarIndeterminateVisibility(true);

        PayPal.requestOneTimePayment(mBraintreeFragment, getPayPalRequest("1.00"));
    }

    public void launchBillingAgreement(View v) {
        setProgressBarIndeterminateVisibility(true);

        PayPal.requestBillingAgreement(mBraintreeFragment, getPayPalRequest(null));
    }

    public void launchVenmo(View v) {
        setProgressBarIndeterminateVisibility(true);

        Venmo.authorizeAccount(mBraintreeFragment);
    }

    // Launching a payment method from the home screen creates a new BraintreeFragment. To maintain sessionID within a
    // fragment, we opted to add PayPal within the PreferredPaymentMethodsActivity.
    private PayPalRequest getPayPalRequest(@Nullable String amount) {
        PayPalRequest request = new PayPalRequest(amount);

        request.displayName(Settings.getPayPalDisplayName(this));

        String landingPageType = Settings.getPayPalLandingPageType(this);
        if (getString(R.string.paypal_landing_page_type_billing).equals(landingPageType)) {
            request.landingPageType(PayPalRequest.LANDING_PAGE_TYPE_BILLING);
        } else if (getString(R.string.paypal_landing_page_type_login).equals(landingPageType)) {
            request.landingPageType(PayPalRequest.LANDING_PAGE_TYPE_LOGIN);
        }

        String intentType = Settings.getPayPalIntentType(this);
        if (intentType.equals(getString(R.string.paypal_intent_authorize))) {
            request.intent(PayPalRequest.INTENT_AUTHORIZE);
        } else if (intentType.equals(getString(R.string.paypal_intent_order))) {
            request.intent(PayPalRequest.INTENT_ORDER);
        } else if (intentType.equals(getString(R.string.paypal_intent_sale))) {
            request.intent(PayPalRequest.INTENT_SALE);
        }

        if (Settings.isPayPalUseractionCommitEnabled(this)) {
            request.userAction(PayPalRequest.USER_ACTION_COMMIT);
        }

        if (Settings.isPayPalCreditOffered(this)) {
            request.offerCredit(true);
        }

        if (Settings.usePayPalAddressOverride(this)) {
            request.shippingAddressOverride(new PostalAddress()
                    .recipientName("Brian Tree")
                    .streetAddress("123 Fake Street")
                    .extendedAddress("Floor A")
                    .locality("San Francisco")
                    .region("CA")
                    .countryCodeAlpha2("US")
            );
        }

        return request;
    }
}
