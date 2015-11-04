package com.braintreepayments.api;

import android.content.Context;
import android.support.annotation.VisibleForTesting;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ViewSwitcher;

import com.braintreepayments.api.dropin.R;
import com.braintreepayments.api.interfaces.BraintreeResponseListener;
import com.braintreepayments.api.interfaces.ConfigurationListener;
import com.braintreepayments.api.interfaces.PaymentMethodNonceCreatedListener;
import com.braintreepayments.api.models.Configuration;
import com.braintreepayments.api.models.PaymentMethodNonce;

/**
 * An intelligent button for handling non-card payment methods. This button will display payment
 * methods depending on their availability.
 *
 * Created {@link PaymentMethodNonce}s will be posted to
 * {@link PaymentMethodNonceCreatedListener}.
 */
public class PaymentButton extends RelativeLayout implements OnClickListener {

    private BraintreeFragment mBraintreeFragment;
    private PaymentRequest mPaymentRequest;
    private ViewSwitcher mProgressViewSwitcher;

    public PaymentButton(Context context) {
        super(context);
    }

    public PaymentButton(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public PaymentButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    /**
     * Initialize the button. This method *MUST* be called or {@link PaymentButton} will not be
     * displayed.
     *
     * @param fragment {@link BraintreeFragment}
     * @param paymentRequest {@link PaymentRequest} containing payment method options.
     */
    public void initialize(BraintreeFragment fragment, PaymentRequest paymentRequest) {
        inflate(getContext(), R.layout.bt_payment_button, this);
        mProgressViewSwitcher = (ViewSwitcher) findViewById(R.id.bt_payment_method_view_switcher);
        showProgress(true);

        mPaymentRequest = paymentRequest;

        mBraintreeFragment = fragment;
        mBraintreeFragment.waitForConfiguration(new ConfigurationListener() {
            @Override
            public void onConfigurationFetched(Configuration configuration) {
                setupButton();
                showProgress(false);
            }
        });
        mBraintreeFragment.setConfigurationErrorListener(new BraintreeResponseListener<Exception>() {
            @Override
            public void onResponse(Exception e) {
                mBraintreeFragment.setConfigurationErrorListener(null);
                setVisibility(GONE);
            }
        });
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.bt_paypal_button) {
            PayPal.authorizeAccount(mBraintreeFragment, mPaymentRequest.getPayPalAdditionalScopes());
        } else if (v.getId() == R.id.bt_android_pay_button) {
            AndroidPay.performMaskedWalletRequest(mBraintreeFragment,
                    mPaymentRequest.getAndroidPayCart(),
                    mPaymentRequest.isAndroidPayShippingAddressRequired(),
                    mPaymentRequest.isAndroidPayPhoneNumberRequired(),
                    mPaymentRequest.getAndroidPayRequestCode());
        }

        performClick();
    }

    private void setupButton() {
        boolean isPayPalEnabled = isPayPalEnabled();
        boolean isAndroidPayEnabled = isAndroidPayEnabled();
        int buttonCount = 0;
        if (!isPayPalEnabled && !isAndroidPayEnabled) {
            setVisibility(GONE);
        } else {
            if (isPayPalEnabled) {
                buttonCount++;
            }
            if (isAndroidPayEnabled) {
                buttonCount++;
            }

            if (isPayPalEnabled) {
                enableButton(findViewById(R.id.bt_paypal_button), buttonCount);
            }
            if (isAndroidPayEnabled) {
                enableButton(findViewById(R.id.bt_android_pay_button), buttonCount);
            }

            if (isPayPalEnabled && buttonCount > 1) {
                findViewById(R.id.bt_payment_button_divider).setVisibility(VISIBLE);
            }

            setVisibility(VISIBLE);
        }
    }

    private void enableButton(View view, int buttonCount) {
        view.setVisibility(VISIBLE);
        view.setOnClickListener(this);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.MATCH_PARENT, 3f / buttonCount);
        view.setLayoutParams(params);
    }

    private void showProgress(boolean showing) {
        mProgressViewSwitcher.setDisplayedChild(showing ? 1 : 0);
    }

    @VisibleForTesting
    protected boolean isPayPalEnabled() {
        return mBraintreeFragment.getConfiguration().isPayPalEnabled();
    }

    @VisibleForTesting
    protected boolean isAndroidPayEnabled() {
        try {
            return (mBraintreeFragment.getConfiguration().getAndroidPay().isEnabled(getContext())
                    && mPaymentRequest.getAndroidPayCart() != null);
        } catch (NoClassDefFoundError e) {
            return false;
        }
    }
}
