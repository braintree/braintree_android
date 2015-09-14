package com.braintreepayments.api.dropin.view;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.braintreepayments.api.Braintree;
import com.braintreepayments.api.Coinbase;
import com.braintreepayments.api.dropin.R;
import com.google.android.gms.wallet.Cart;

import java.util.List;

/**
 * Skinned button for launching flows other than basic credit card forms (Pay With PayPal, Pay With Venmo, etc.).
 * The button will intelligently display payment methods depending on their availability.
 *
 * Using a {@link com.braintreepayments.api.dropin.view.PaymentButton} requires some setup in the {@link android.app.Activity}
 * that will host the button:
 * <ul>
 *     <li>Initialize a {@link com.braintreepayments.api.Braintree} object with at least one
 *     {@link com.braintreepayments.api.Braintree.PaymentMethodCreatedListener} or {@link com.braintreepayments.api.Braintree.PaymentMethodNonceListener}.</li>
 *     <li>Call {@link #initialize(android.app.Activity, com.braintreepayments.api.Braintree)}</li>
 *     <li>Any time you receive {@link android.app.Activity#onActivityResult(int, int, android.content.Intent)},
 *     call {@link #onActivityResult(int, int, android.content.Intent)}. The button will only perform actions on the appropriate request code.
 *     </li>
 * </ul>
 *
 * {@link com.braintreepayments.api.models.PaymentMethod}s and nonces will be posted to the appropriate
 * listeners set in the {@link com.braintreepayments.api.Braintree} instance used to initialize the button.
 *
 * If you need to override the default {@link com.braintreepayments.api.dropin.view.PaymentButton#REQUEST_CODE},
 * use {@link com.braintreepayments.api.dropin.view.PaymentButton#initialize(android.app.Activity, com.braintreepayments.api.Braintree, int)} and provide
 * the request code you would like to use.
 */
public class PaymentButton extends RelativeLayout implements OnClickListener {

    private Activity mActivity;
    private int mRequestCode;
    private Braintree mBraintree;

    private Cart mCart;
    private boolean mIsBillingAgreement;
    private boolean mShippingAddressRequired;
    private boolean mPhoneNumberRequired;

    private List<String> mAdditionalScopes;

    /**
     * Default request code to use when launching Pay With... flows.
     * Can be overridden by using {@link com.braintreepayments.api.dropin.view.PaymentButton#initialize(android.app.Activity, com.braintreepayments.api.Braintree, int)}
     * instead of {@link com.braintreepayments.api.dropin.view.PaymentButton#initialize(android.app.Activity, com.braintreepayments.api.Braintree)}.
     */
    public static final int REQUEST_CODE = 11876;

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
     * Initialize the button for display. This method MUST be called to use the PaymentButton.
     * @param activity {@link android.app.Activity} hosting the button
     * @param braintree {@link com.braintreepayments.api.Braintree} instance being used to communicate with the Braintree gateway.
     */
    public void initialize(Activity activity, Braintree braintree) {
        initialize(activity, braintree, REQUEST_CODE);
    }

    /**
     * Use this if you need to manually specify a request code.
     * @param activity {@link Activity} hosting the button
     * @param braintree {@link Braintree} instance being used to communicate with the Braintree gateway.
     * @param requestCode Unique identifier for launching activities via {@link Activity#startActivityForResult(Intent, int)}.
     *                      Use this in your {@link Activity} when overriding {@link Activity#onActivityResult(int, int, Intent)}
     *                      to call {@link PaymentButton#onActivityResult(int, int, Intent)}.
     */
    public void initialize(Activity activity, Braintree braintree, int requestCode) {
        mActivity = activity;
        mBraintree = braintree;
        mRequestCode = requestCode;

        inflate(getContext(), R.layout.bt_payment_button, this);

        boolean isPayPalEnabled = mBraintree.isPayPalEnabled();
        boolean isVenmoEnabled = mBraintree.isVenmoEnabled();
        boolean isCoinbaseEnabled = mBraintree.isCoinbaseEnabled();
        boolean isAndroidPayEnabled = (mBraintree.isAndroidPayEnabled() && (mCart != null || mIsBillingAgreement));
        int buttonCount = 0;
        if (!isPayPalEnabled && !isVenmoEnabled && !isCoinbaseEnabled && !isAndroidPayEnabled) {
            setVisibility(GONE);
        } else {
            if (isPayPalEnabled) {
                buttonCount++;
            }
            if (isVenmoEnabled) {
                buttonCount++;
            }
            if (isCoinbaseEnabled) {
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
            if (isCoinbaseEnabled) {
                enableButton(findViewById(R.id.bt_coinbase_button), buttonCount);
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
        }
    }

    /**
     * Options to be used with Android Pay. Must be called *before*
     * {@link PaymentButton#initialize(Activity, Braintree)} if you would like to use Android Pay.
     * Failure to do so will result in Android Pay not present in the {@link PaymentButton}.
     *
     * @param cart The {@link Cart} to use with Android Pay
     */
    public void setAndroidPayOptions(Cart cart) {
        setAndroidPayOptions(cart, false, false, false);
    }

    /**
     * Options to be used with Android Pay. Must be called *before*
     * {@link PaymentButton#initialize(Activity, Braintree)} if you would like to use Android Pay.
     * Failure to do so will result in Android Pay not present in the {@link PaymentButton}.
     *
     * @param cart The {@link Cart} to use with Android Pay
     * @param isBillingAgreement Should a multiple use card be requested.
     * @param shippingAddressRequired Should the user be prompted for a shipping address.
     * @param phoneNumberRequired Should the user be prompted for a phone number.
     */
    public void setAndroidPayOptions(Cart cart, boolean isBillingAgreement,
            boolean shippingAddressRequired, boolean phoneNumberRequired) {
        mCart = cart;
        mIsBillingAgreement = isBillingAgreement;
        mShippingAddressRequired = shippingAddressRequired;
        mPhoneNumberRequired = phoneNumberRequired;
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

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.bt_paypal_button) {
            mBraintree.startPayWithPayPal(mActivity, mRequestCode, mAdditionalScopes);
        } else if (v.getId() == R.id.bt_venmo_button) {
            mBraintree.startPayWithVenmo(mActivity, mRequestCode);
        } else if (v.getId() == R.id.bt_coinbase_button) {
            mBraintree.startPayWithCoinbase(mActivity, mRequestCode);
        } else if (v.getId() == R.id.bt_android_pay_button) {
            mBraintree.performAndroidPayMaskedWalletRequest(mActivity, mRequestCode, mCart,
                    mIsBillingAgreement, mShippingAddressRequired, mPhoneNumberRequired);
        }

        performClick();
    }

    /**
     * Extracts payment information from activity results and posts {@link com.braintreepayments.api.models.PaymentMethod}s
     * or nonces to listeners as appropriate.
     *
     * @param requestCode Request code from {@link Activity#onActivityResult(int, int, android.content.Intent)}
     * @param responseCode Result code from {@link Activity#onActivityResult(int, int, android.content.Intent)}
     * @param data {@link android.content.Intent} from {@link Activity#onActivityResult(int, int, android.content.Intent)}
     */
    public void onActivityResult(int requestCode, int responseCode, Intent data) {
        if (requestCode == mRequestCode) {
            mBraintree.onActivityResult(mActivity, requestCode, responseCode, data);
        }
    }

    private void enableButton(View view, int buttonCount) {
        view.setVisibility(VISIBLE);
        view.setOnClickListener(this);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.MATCH_PARENT, 3f / buttonCount);
        view.setLayoutParams(params);
    }
}
