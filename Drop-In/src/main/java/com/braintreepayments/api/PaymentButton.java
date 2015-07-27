package com.braintreepayments.api;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.VisibleForTesting;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ViewSwitcher;

import com.braintreepayments.api.dropin.R;
import com.braintreepayments.api.interfaces.ConfigurationErrorListener;
import com.braintreepayments.api.interfaces.ConfigurationListener;
import com.google.android.gms.wallet.Cart;

import java.util.List;

/**
 * An intelligent button for handling non-card payment methods. This button will display payment
 * methods depending on their availability.
 *
 * Created {@link com.braintreepayments.api.models.PaymentMethod}s will be posted to
 * {@link com.braintreepayments.api.interfaces.PaymentMethodCreatedListener}.
 */
public class PaymentButton extends RelativeLayout implements ConfigurationErrorListener,
        OnClickListener {

    private BraintreeFragment mBraintreeFragment;
    private ViewSwitcher mProgressViewSwitcher;

    private Cart mCart;
    private boolean mIsBillingAgreement;
    private boolean mShippingAddressRequired;
    private boolean mPhoneNumberRequired;
    private int mRequestCode;

    private List<String> mAdditionalScopes;

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
     * Options to be used with Android Pay. Must be called *before*
     * {@link PaymentButton#initialize(BraintreeFragment)} if you would like to use Android Pay.
     * Failure to do so will result in Android Pay not present in the {@link PaymentButton}.
     *
     * @param cart The {@link Cart} to use with Android Pay.
     * @param requestCode The requestCode to use with {@link android.app.Activity#startActivityForResult(Intent, int)}.
     */
    public void setAndroidPayOptions(Cart cart, int requestCode) {
        setAndroidPayOptions(cart, false, false, false, requestCode);
    }

    /**
     * Options to be used with Android Pay. Must be called *before*
     * {@link PaymentButton#initialize(BraintreeFragment)} if you would like to use Android Pay.
     * Failure to do so will result in Android Pay not present in the {@link PaymentButton}.
     *
     * @param cart The {@link Cart} to use with Android Pay
     * @param isBillingAgreement Should a multiple use card be requested.
     * @param shippingAddressRequired Should the user be prompted for a shipping address.
     * @param phoneNumberRequired Should the user be prompted for a phone number.
     * @param requestCode The requestCode to use with {@link android.app.Activity#startActivityForResult(Intent, int)}}.
     */
    public void setAndroidPayOptions(Cart cart, boolean isBillingAgreement,
            boolean shippingAddressRequired, boolean phoneNumberRequired, int requestCode) {
        mCart = cart;
        mIsBillingAgreement = isBillingAgreement;
        mShippingAddressRequired = shippingAddressRequired;
        mPhoneNumberRequired = phoneNumberRequired;
        mRequestCode = requestCode;
    }

    /**
     * Set additional scopes to request when a user is authorizing PayPal.
     *
     * @param additionalScopes A {@link java.util.List} of additional scopes.
     *                         Ex: PayPalOAuthScopes.PAYPAL_SCOPE_ADDRESS.
     *                         Acceptable scopes are defined in {@link com.paypal.android.sdk.payments.PayPalOAuthScopes}.
     */
    public void setAdditionalPayPalScopes(List<String> additionalScopes) {
        mAdditionalScopes = additionalScopes;
    }

    /**
     * Initialize the button. This method *MUST* be called or {@link PaymentButton} will not be
     * displayed.
     *
     * @param fragment {@link BraintreeFragment}
     */
    public void initialize(BraintreeFragment fragment) {
        inflate(getContext(), R.layout.bt_payment_button, this);
        mProgressViewSwitcher = (ViewSwitcher) findViewById(R.id.bt_payment_method_view_switcher);
        showProgress(true);

        mBraintreeFragment = fragment;
        mBraintreeFragment.addListener(this);
        mBraintreeFragment.waitForConfiguration(new ConfigurationListener() {
            @Override
            public void onConfigurationFetched() {
                mBraintreeFragment.removeListener(this);
                setupButton();
                showProgress(false);
            }
        });
    }

    @Override
    public void onConfigurationError(Throwable throwable) {
        setVisibility(GONE);
        mBraintreeFragment.removeListener(this);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.bt_paypal_button) {
            PayPal.authorizeAccount(mBraintreeFragment, mAdditionalScopes);
        } else if (v.getId() == R.id.bt_venmo_button) {
            Venmo.authorize(mBraintreeFragment);
        } else if (v.getId() == R.id.bt_android_pay_button) {
            AndroidPay.performMaskedWalletRequest(mBraintreeFragment, mCart, mIsBillingAgreement,
                    mShippingAddressRequired, mPhoneNumberRequired, mRequestCode);
        }

        performClick();
    }

    private void setupButton() {
        boolean isPayPalEnabled = isPayPalEnabled();
        boolean isVenmoEnabled = isVenmoEnabled();
        boolean isAndroidPayEnabled = isAndroidPayEnabled();
        int buttonCount = 0;
        if (!isPayPalEnabled && !isVenmoEnabled && !isAndroidPayEnabled) {
            setVisibility(GONE);
        } else {
            if (isPayPalEnabled) {
                buttonCount++;
            }
            if (isVenmoEnabled) {
                buttonCount++;
            }
            if (isAndroidPayEnabled) {
                buttonCount++;
            }

            if (isPayPalEnabled) {
                enableButton(findViewById(R.id.bt_paypal_button), buttonCount);
            }
            if (isVenmoEnabled) {
                enableButton(findViewById(R.id.bt_venmo_button), buttonCount);
            }
            if (isAndroidPayEnabled) {
                enableButton(findViewById(R.id.bt_android_pay_button), buttonCount);
            }

            if (isPayPalEnabled && buttonCount > 1) {
                findViewById(R.id.bt_payment_button_divider).setVisibility(VISIBLE);
            } else if (isVenmoEnabled && buttonCount > 1) {
                findViewById(R.id.bt_payment_button_divider_2).setVisibility(VISIBLE);
            }
            if (buttonCount > 2) {
                findViewById(R.id.bt_payment_button_divider_2).setVisibility(VISIBLE);
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
    protected boolean isVenmoEnabled() {
        return Venmo.isAvailable(getContext(), mBraintreeFragment.getConfiguration());
    }

    @VisibleForTesting
    protected boolean isAndroidPayEnabled() {
        return (mBraintreeFragment.getConfiguration().getAndroidPay().isEnabled(getContext())
                && (mCart != null || mIsBillingAgreement));
    }
}
