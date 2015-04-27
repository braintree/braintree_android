package com.braintreepayments.api.dropin.view;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.RelativeLayout;

import com.braintreepayments.api.Braintree;
import com.braintreepayments.api.PayPalHelper;
import com.braintreepayments.api.dropin.R;

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
     * @param activity {@link android.app.Activity} hosting the button
     * @param braintree {@link com.braintreepayments.api.Braintree} instance being used to communicate with the Braintree gateway.
     * @param requestCode Unique identifier for launching activities via {@link android.app.Activity#startActivityForResult(android.content.Intent, int)}.
     *                      Use this in your {@link android.app.Activity} when overriding {@link android.app.Activity#onActivityResult(int, int, android.content.Intent)}
     *                      to call {@link com.braintreepayments.api.dropin.view.PaymentButton#onActivityResult(int, int, android.content.Intent)}.
     */
    public void initialize(Activity activity, Braintree braintree, int requestCode) {
        mActivity = activity;
        mBraintree = braintree;
        mRequestCode = requestCode;

        inflate(getContext(), R.layout.bt_payment_button, this);

        boolean isPayPalEnabled = mBraintree.isPayPalEnabled();
        boolean isAndroidPayEnabled = mBraintree.isAndroidPayEnabled();

        if (!isPayPalEnabled && !isAndroidPayEnabled) {
            setVisibility(GONE);
        } else {
            if (isPayPalEnabled) {
                ImageButton paypalButton = (ImageButton) findViewById(R.id.bt_paypal_button);
                paypalButton.setVisibility(VISIBLE);
                paypalButton.setOnClickListener(this);
            }

            if (isAndroidPayEnabled) {
                ImageButton googleWalletButton = (ImageButton) findViewById(R.id.bt_google_wallet_button);
                googleWalletButton.setVisibility(VISIBLE);
                googleWalletButton.setOnClickListener(this);
            }

            if (isPayPalEnabled && isAndroidPayEnabled) {
                findViewById(R.id.bt_payment_button_divider).setVisibility(VISIBLE);
            }
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.bt_paypal_button) {
            mBraintree.startPayWithPayPal(mActivity, mRequestCode);
        } else if (v.getId() == R.id.bt_google_wallet_button) {
            mBraintree.startPayWithGoogleWallet(mActivity, mRequestCode);
        }

        callOnClick();
    }

    /**
     * Extracts payment information from activity results and posts {@link com.braintreepayments.api.models.PaymentMethod}s
     * or nonces to listeners as appropriate.
     * @param requestCode Request code from {@link Activity#onActivityResult(int, int, android.content.Intent)}
     * @param resultCode Result code from {@link Activity#onActivityResult(int, int, android.content.Intent)}
     * @param data {@link android.content.Intent} from {@link Activity#onActivityResult(int, int, android.content.Intent)}
     */
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == mRequestCode && resultCode == Activity.RESULT_OK) {
            if(PayPalHelper.isPayPalIntent(data)) {
                mBraintree.finishPayWithPayPal(mActivity, resultCode, data);
            } else {
                mBraintree.finishPayWithGoogleWallet(resultCode, data);
            }
        }
    }
}
