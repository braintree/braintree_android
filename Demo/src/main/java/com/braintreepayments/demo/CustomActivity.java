package com.braintreepayments.demo;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageButton;

import com.braintreepayments.api.AndroidPay;
import com.braintreepayments.api.BraintreeFragment;
import com.braintreepayments.api.BraintreePaymentActivity;
import com.braintreepayments.api.Card;
import com.braintreepayments.api.DataCollector;
import com.braintreepayments.api.PayPal;
import com.braintreepayments.api.exceptions.InvalidArgumentException;
import com.braintreepayments.api.interfaces.BraintreeErrorListener;
import com.braintreepayments.api.interfaces.BraintreeResponseListener;
import com.braintreepayments.api.interfaces.ConfigurationListener;
import com.braintreepayments.api.interfaces.PaymentMethodNonceCreatedListener;
import com.braintreepayments.api.interfaces.TokenizationParametersListener;
import com.braintreepayments.api.models.CardBuilder;
import com.braintreepayments.api.models.Configuration;
import com.braintreepayments.api.models.PayPalRequest;
import com.braintreepayments.api.models.PaymentMethodNonce;
import com.braintreepayments.cardform.OnCardFormSubmitListener;
import com.braintreepayments.cardform.view.CardForm;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wallet.Cart;
import com.google.android.gms.wallet.FullWallet;
import com.google.android.gms.wallet.FullWalletRequest;
import com.google.android.gms.wallet.MaskedWallet;
import com.google.android.gms.wallet.MaskedWalletRequest;
import com.google.android.gms.wallet.PaymentMethodTokenizationParameters;
import com.google.android.gms.wallet.Wallet;
import com.google.android.gms.wallet.WalletConstants;

import java.util.Collection;
import java.util.Collections;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

public class CustomActivity extends BaseActivity implements ConfigurationListener,
        PaymentMethodNonceCreatedListener, BraintreeErrorListener, OnCardFormSubmitListener {

    private static final int ANDROID_PAY_MASKED_WALLET_REQUEST_CODE = 1;
    private static final int ANDROID_PAY_FULL_WALLET_REQUEST_CODE = 2;

    private GoogleApiClient mGoogleApiClient;
    private Cart mCart;
    private String mDeviceData;

    private ImageButton mPayPalButton;
    private ImageButton mAndroidPayButton;
    private CardForm mCardForm;
    private Button mPurchaseButton;

    @Override
    protected void onCreate(Bundle onSaveInstanceState) {
        super.onCreate(onSaveInstanceState);

        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.custom_activity);
        setUpAsBack();

        mCart = getIntent().getParcelableExtra(MainActivity.EXTRA_ANDROID_PAY_CART);

        mPayPalButton = (ImageButton) findViewById(R.id.paypal_button);
        mAndroidPayButton = (ImageButton) findViewById(R.id.android_pay_button);

        mCardForm = (CardForm) findViewById(R.id.card_form);
        mCardForm.setRequiredFields(this, true, true, false, false, getString(R.string.purchase));
        mCardForm.setOnCardFormSubmitListener(this);

        mPurchaseButton = (Button) findViewById(R.id.purchase_button);

        setProgressBarIndeterminateVisibility(true);
    }

    @Override
    protected void reset() {
        setProgressBarIndeterminateVisibility(true);
        mPayPalButton.setVisibility(GONE);
        mAndroidPayButton.setVisibility(GONE);
        mPurchaseButton.setEnabled(false);
    }

    @Override
    protected void onAuthorizationFetched() {
        try {
            mBraintreeFragment = BraintreeFragment.newInstance(this, mAuthorization);
        } catch (InvalidArgumentException e) {
            onError(e);
        }

        setProgressBarIndeterminateVisibility(false);
        mPurchaseButton.setEnabled(true);
    }

    @Override
    public void onConfigurationFetched(Configuration configuration) {
        if (configuration.isPayPalEnabled()) {
            mPayPalButton.setVisibility(VISIBLE);
        }

        if (configuration.getAndroidPay().isEnabled(this)) {
            mAndroidPayButton.setVisibility(VISIBLE);
        }

        if (getIntent().getBooleanExtra(MainActivity.EXTRA_COLLECT_DEVICE_DATA, false)) {
            mDeviceData = DataCollector.collectDeviceData(mBraintreeFragment);
        }
    }

    public void launchPayPal(View v) {
        setProgressBarIndeterminateVisibility(true);

        String paymentType = Settings.getPayPalPaymentType(this);
        if (paymentType.equals(getString(R.string.paypal_billing_agreement))) {
            PayPal.requestBillingAgreement(mBraintreeFragment, new PayPalRequest());
        } else if (paymentType.equals(getString(R.string.paypal_future_payment))) {
            if (Settings.isPayPalAddressScopeRequested(this)) {
                PayPal.authorizeAccount(mBraintreeFragment, Collections.singletonList(PayPal.SCOPE_ADDRESS));
            } else {
                PayPal.authorizeAccount(mBraintreeFragment);
            }
        } else if (paymentType.equals(getString(R.string.paypal_single_payment))) {
            PayPal.requestOneTimePayment(mBraintreeFragment, new PayPalRequest("1.00"));
        }
    }

    public void launchAndroidPay(View v) {
        setProgressBarIndeterminateVisibility(true);

        mBraintreeFragment.getGoogleApiClient(new BraintreeResponseListener<GoogleApiClient>() {
            @Override
            public void onResponse(GoogleApiClient googleApiClient) {
                mGoogleApiClient = googleApiClient;
                requestAndroidPayMaskedWallet();
            }
        });
    }

    private void requestAndroidPayMaskedWallet() {
        AndroidPay.getTokenizationParameters(mBraintreeFragment,
                new TokenizationParametersListener() {
                    @Override
                    public void onResult(PaymentMethodTokenizationParameters parameters,
                            Collection<Integer> allowedCardNetworks) {
                        MaskedWalletRequest.Builder maskedWalletRequestBuilder =
                                MaskedWalletRequest.newBuilder()
                                        .setMerchantName("Braintree")
                                        .setCurrencyCode("USD")
                                        .setCart(mCart)
                                        .setEstimatedTotalPrice(mCart.getTotalPrice())
                                        .setShippingAddressRequired(Settings.isAndroidPayShippingAddressRequired(CustomActivity.this))
                                        .setPhoneNumberRequired(Settings.isAndroidPayPhoneNumberRequired(CustomActivity.this))
                                        .setPaymentMethodTokenizationParameters(parameters)
                                        .addAllowedCardNetworks(allowedCardNetworks);

                        Wallet.Payments.loadMaskedWallet(mGoogleApiClient,
                                maskedWalletRequestBuilder.build(), ANDROID_PAY_MASKED_WALLET_REQUEST_CODE);

                    }
                });
    }

    @Override
    public void onCardFormSubmit() {
        onPurchase(null);
    }

    public void onPurchase(View v) {
        setProgressBarIndeterminateVisibility(true);

        CardBuilder cardBuilder = new CardBuilder()
                .cardNumber(mCardForm.getCardNumber())
                .expirationMonth(mCardForm.getExpirationMonth())
                .expirationYear(mCardForm.getExpirationYear());

        Card.tokenize(mBraintreeFragment, cardBuilder);
    }

    @Override
    public void onCancel(int requestCode) {
        super.onCancel(requestCode);
        setProgressBarIndeterminateVisibility(false);
    }

    @Override
    public void onPaymentMethodNonceCreated(PaymentMethodNonce paymentMethodNonce) {
        super.onPaymentMethodNonceCreated(paymentMethodNonce);

        Intent intent = new Intent()
                .putExtra(BraintreePaymentActivity.EXTRA_PAYMENT_METHOD_NONCE, paymentMethodNonce)
                .putExtra(BraintreePaymentActivity.EXTRA_DEVICE_DATA, mDeviceData);
        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    public void onError(Exception error) {
        super.onError(error);
        setProgressBarIndeterminateVisibility(false);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == ANDROID_PAY_MASKED_WALLET_REQUEST_CODE) {
                String googleTransactionId =
                        ((MaskedWallet) data.getParcelableExtra(WalletConstants.EXTRA_MASKED_WALLET))
                                .getGoogleTransactionId();
                FullWalletRequest fullWalletRequest = FullWalletRequest.newBuilder()
                        .setGoogleTransactionId(googleTransactionId)
                        .setCart(mCart)
                        .build();

                Wallet.Payments.loadFullWallet(mGoogleApiClient, fullWalletRequest,
                        ANDROID_PAY_FULL_WALLET_REQUEST_CODE);
            } else if (requestCode == ANDROID_PAY_FULL_WALLET_REQUEST_CODE) {
                AndroidPay.tokenize(mBraintreeFragment,
                        (FullWallet) data.getParcelableExtra(WalletConstants.EXTRA_FULL_WALLET));
            }
        } else if (resultCode == RESULT_CANCELED) {
            onCancel(requestCode);
        } else {
            int errorCode = -1;
            if (data != null) {
                errorCode = data.getIntExtra(WalletConstants.EXTRA_ERROR_CODE, -1);
            }

            onError(new Exception("Request Code: " + requestCode + " Result Code: " + resultCode +
                    " Error Code: " + errorCode));
        }
    }
}
