package com.braintreepayments.demo;

import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.braintreepayments.api.models.CardNonce;
import com.braintreepayments.api.models.GooglePaymentCardNonce;
import com.braintreepayments.api.models.LocalPaymentResult;
import com.braintreepayments.api.models.PayPalAccountNonce;
import com.braintreepayments.api.models.PaymentMethodNonce;
import com.braintreepayments.api.models.VenmoAccountNonce;
import com.braintreepayments.api.models.VisaCheckoutNonce;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

public class MainActivity extends BaseActivity {

    static final String EXTRA_PAYMENT_RESULT = "payment_result";
    static final String EXTRA_DEVICE_DATA = "device_data";
    static final String EXTRA_COLLECT_DEVICE_DATA = "collect_device_data";

    private static final int DROP_IN_REQUEST = 1;
    private static final int GOOGLE_PAYMENT_REQUEST = 2;
    private static final int CARDS_REQUEST = 3;
    private static final int PAYPAL_REQUEST = 4;
    private static final int VENMO_REQUEST = 5;
    private static final int VISA_CHECKOUT_REQUEST = 6;
    private static final int LOCAL_PAYMENTS_REQUEST = 7;
    private static final int PAYPAL_TWO_FACTOR_REQUEST = 8;
    private static final int PREFERRED_PAYMENT_METHODS_REQUEST = 9;

    private static final String KEY_NONCE = "nonce";

    private PaymentMethodNonce mNonce;

    private ImageView mNonceIcon;
    private TextView mNonceString;
    private TextView mNonceDetails;
    private TextView mDeviceData;

    private Button mGooglePaymentButton;
    private Button mCardsButton;
    private Button mPayPalButton;
    private Button mVenmoButton;
    private Button mVisaCheckoutButton;
    private Button mCreateTransactionButton;
    private Button mLocalPaymentsButton;
    private Button mPreferredPaymentMethods;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        mNonceIcon = findViewById(R.id.nonce_icon);
        mNonceString = findViewById(R.id.nonce);
        mNonceDetails = findViewById(R.id.nonce_details);
        mDeviceData = findViewById(R.id.device_data);

        mGooglePaymentButton = findViewById(R.id.google_payment);
        mCardsButton = findViewById(R.id.card);
        mPayPalButton = findViewById(R.id.paypal);
        mVenmoButton = findViewById(R.id.venmo);
        mVisaCheckoutButton = findViewById(R.id.visa_checkout);
        mLocalPaymentsButton = findViewById(R.id.local_payments);
        mPreferredPaymentMethods = findViewById(R.id.preferred_payment_methods);
        mCreateTransactionButton = findViewById(R.id.create_transaction);

        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(KEY_NONCE)) {
                mNonce = savedInstanceState.getParcelable(KEY_NONCE);
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mNonce != null) {
            outState.putParcelable(KEY_NONCE, mNonce);
        }
    }

    public void launchGooglePayment(View v) {
        Intent intent = new Intent(this, GooglePaymentActivity.class);
        startActivityForResult(intent, GOOGLE_PAYMENT_REQUEST);
    }

    public void launchCards(View v) {
        Intent intent = new Intent(this, CardActivity.class)
                .putExtra(EXTRA_COLLECT_DEVICE_DATA, Settings.shouldCollectDeviceData(this));
        startActivityForResult(intent, CARDS_REQUEST);
    }

    public void launchPayPal(View v) {
        Intent intent = new Intent(this, PayPalActivity.class)
                .putExtra(EXTRA_COLLECT_DEVICE_DATA, Settings.shouldCollectDeviceData(this));
        startActivityForResult(intent, PAYPAL_REQUEST);
    }

    public void launchVenmo(View v) {
        Intent intent = new Intent(this, VenmoActivity.class);
        startActivityForResult(intent, VENMO_REQUEST);
    }

    public void launchVisaCheckout(View v) {
        Intent intent = new Intent(this, VisaCheckoutActivity.class);
        startActivityForResult(intent, VISA_CHECKOUT_REQUEST);
    }

    public void launchLocalPayments(View v) {
        Intent intent = new Intent(this, LocalPaymentsActivity.class);
        startActivityForResult(intent, LOCAL_PAYMENTS_REQUEST);
    }

    public void launchPreferredPaymentMethods(View v) {
        Intent intent = new Intent(this, PreferredPaymentMethodsActivity.class);
        startActivityForResult(intent, PREFERRED_PAYMENT_METHODS_REQUEST);
    }

    public void createTransaction(View v) {
        Intent intent = new Intent(this, CreateTransactionActivity.class)
                .putExtra(CreateTransactionActivity.EXTRA_PAYMENT_METHOD_NONCE, mNonce);
        startActivity(intent);

        mCreateTransactionButton.setEnabled(false);
        clearNonce();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            Parcelable returnedData = data.getParcelableExtra(EXTRA_PAYMENT_RESULT);
            String deviceData = data.getStringExtra(EXTRA_DEVICE_DATA);
            if (returnedData instanceof PaymentMethodNonce) {
                displayNonce((PaymentMethodNonce) returnedData, deviceData);
            }

            mCreateTransactionButton.setEnabled(true);
        }
    }

    @Override
    protected void reset() {
        enableButtons(false);
        mCreateTransactionButton.setEnabled(false);

        clearNonce();
    }

    @Override
    protected void onBraintreeInitialized() {
        enableButtons(true);
    }

    private void displayNonce(PaymentMethodNonce paymentMethodNonce, String deviceData) {
        mNonce = paymentMethodNonce;
        Log.d("KANYE", mNonce.getNonce());

        mNonceIcon.setVisibility(VISIBLE);

        mNonceString.setText(getString(R.string.nonce_placeholder, mNonce.getNonce()));
        mNonceString.setVisibility(VISIBLE);

        String details = "";
        if (mNonce instanceof CardNonce) {
            details = CardActivity.getDisplayString((CardNonce) mNonce);
        } else if (mNonce instanceof PayPalAccountNonce) {
            details = PayPalActivity.getDisplayString((PayPalAccountNonce) mNonce);
        } else if (mNonce instanceof GooglePaymentCardNonce) {
            details = GooglePaymentActivity.getDisplayString((GooglePaymentCardNonce) mNonce);
        } else if (mNonce instanceof VisaCheckoutNonce) {
            details = VisaCheckoutActivity.getDisplayString((VisaCheckoutNonce) mNonce);
        } else if (mNonce instanceof VenmoAccountNonce) {
            details = VenmoActivity.getDisplayString((VenmoAccountNonce) mNonce);
        } else if (mNonce instanceof LocalPaymentResult) {
            details = LocalPaymentsActivity.getDisplayString((LocalPaymentResult) mNonce);
        }

        mNonceDetails.setText(details);
        mNonceDetails.setVisibility(VISIBLE);

        mDeviceData.setText(getString(R.string.device_data_placeholder, deviceData));
        mDeviceData.setVisibility(VISIBLE);

        mCreateTransactionButton.setEnabled(true);
    }

    private void clearNonce() {
        mNonceIcon.setVisibility(GONE);
        mNonceString.setVisibility(GONE);
        mNonceDetails.setVisibility(GONE);
        mDeviceData.setVisibility(GONE);
        mCreateTransactionButton.setEnabled(false);
    }

    private void enableButtons(boolean enable) {
        mGooglePaymentButton.setEnabled(enable);
        mCardsButton.setEnabled(enable);
        mPayPalButton.setEnabled(enable);
        mVenmoButton.setEnabled(enable);
        mVisaCheckoutButton.setEnabled(enable);
        mLocalPaymentsButton.setEnabled(enable);
        mPreferredPaymentMethods.setEnabled(enable);
    }
}
