package com.braintreepayments.demo;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.braintreepayments.api.AndroidPay;
import com.braintreepayments.api.BraintreeFragment;
import com.braintreepayments.api.BraintreePaymentActivity;
import com.braintreepayments.api.Card;
import com.braintreepayments.api.DataCollector;
import com.braintreepayments.api.PayPal;
import com.braintreepayments.api.UnionPay;
import com.braintreepayments.api.exceptions.InvalidArgumentException;
import com.braintreepayments.api.interfaces.BraintreeErrorListener;
import com.braintreepayments.api.interfaces.BraintreeResponseListener;
import com.braintreepayments.api.interfaces.ConfigurationListener;
import com.braintreepayments.api.interfaces.PaymentMethodNonceCreatedListener;
import com.braintreepayments.api.interfaces.TokenizationParametersListener;
import com.braintreepayments.api.interfaces.UnionPayListener;
import com.braintreepayments.api.models.CardBuilder;
import com.braintreepayments.api.models.Configuration;
import com.braintreepayments.api.models.PayPalRequest;
import com.braintreepayments.api.models.PaymentMethodNonce;
import com.braintreepayments.api.models.UnionPayCapabilities;
import com.braintreepayments.api.models.UnionPayCardBuilder;
import com.braintreepayments.cardform.OnCardFormFieldFocusedListener;
import com.braintreepayments.cardform.OnCardFormSubmitListener;
import com.braintreepayments.cardform.utils.CardType;
import com.braintreepayments.cardform.view.CardEditText;
import com.braintreepayments.cardform.view.CardForm;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.identity.intents.model.CountrySpecification;
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

public class CustomActivity extends BaseActivity implements ConfigurationListener, UnionPayListener,
        PaymentMethodNonceCreatedListener, BraintreeErrorListener, OnCardFormSubmitListener,
        OnCardFormFieldFocusedListener {

    private static final String EXTRA_UNIONPAY = "com.braintreepayments.demo.EXTRA_UNIONPAY";
    private static final String EXTRA_UNIONPAY_ENROLLMENT_ID = "com.braintreepayments.demo.EXTRA_UNIONPAY_ENROLLMENT_ID";

    private static final int ANDROID_PAY_MASKED_WALLET_REQUEST_CODE = 1;
    private static final int ANDROID_PAY_FULL_WALLET_REQUEST_CODE = 2;

    private Configuration mConfiguration;
    private GoogleApiClient mGoogleApiClient;
    private Cart mCart;
    private String mDeviceData;
    private boolean mIsUnionPay;
    private String mEnrollmentId;

    private ImageButton mPayPalButton;
    private ImageButton mAndroidPayButton;
    private CardForm mCardForm;
    private EditText mCountryCode;
    private EditText mMobilePhone;
    private EditText mSmsCode;
    private Button mSendSmsButton;
    private Button mPurchaseButton;

    private CardType mCardType;

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
        mCardForm.setOnFormFieldFocusedListener(this);
        mCardForm.setOnCardFormSubmitListener(this);

        mCountryCode = (EditText) findViewById(R.id.country_code);
        mMobilePhone = (EditText) findViewById(R.id.mobile_phone);
        mSmsCode = (EditText) findViewById(R.id.sms_code);
        mSendSmsButton = (Button) findViewById(R.id.unionpay_enroll_button);
        mPurchaseButton = (Button) findViewById(R.id.purchase_button);

        if (onSaveInstanceState != null) {
            mIsUnionPay = onSaveInstanceState.getBoolean(EXTRA_UNIONPAY);
            mEnrollmentId = onSaveInstanceState.getString(EXTRA_UNIONPAY_ENROLLMENT_ID);

            if (mIsUnionPay) {
                mCountryCode.setVisibility(VISIBLE);
                mMobilePhone.setVisibility(VISIBLE);
                mSendSmsButton.setVisibility(VISIBLE);
            }
        }

        setProgressBarIndeterminateVisibility(true);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(EXTRA_UNIONPAY, mIsUnionPay);
        outState.putString(EXTRA_UNIONPAY_ENROLLMENT_ID, mEnrollmentId);
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
        mConfiguration = configuration;

        mCardForm.setRequiredFields(this, true, true, configuration.isCvvChallengePresent(),
                configuration.isPostalCodeChallengePresent(), getString(R.string.purchase));

        if (configuration.isPayPalEnabled()) {
            mPayPalButton.setVisibility(VISIBLE);
        }

        if (configuration.getAndroidPay().isEnabled(this)) {
            AndroidPay.isReadyToPay(mBraintreeFragment, new BraintreeResponseListener<Boolean>() {
                @Override
                public void onResponse(Boolean isReadyToPay) {
                    if (isReadyToPay) {
                        mAndroidPayButton.setVisibility(VISIBLE);
                    }
                }
            });
        }

        if (getIntent().getBooleanExtra(MainActivity.EXTRA_COLLECT_DEVICE_DATA, false)) {
            DataCollector.collectDeviceData(mBraintreeFragment, new BraintreeResponseListener<String>() {
                @Override
                public void onResponse(String deviceData) {
                    mDeviceData = deviceData;
                }
            });
        }
    }

    @Override
    public void onCardFormFieldFocused(View field) {
        if (!(field instanceof CardEditText) && !TextUtils.isEmpty(mCardForm.getCardNumber())) {
            CardType cardType = CardType.forCardNumber(mCardForm.getCardNumber());
            if (mCardType != cardType) {
                mCardType  = cardType;

                if (mConfiguration.getUnionPay().isEnabled()) {
                    UnionPay.fetchCapabilities(mBraintreeFragment, mCardForm.getCardNumber());
                }
            }
        }
    }

    @Override
    public void onCapabilitiesFetched(UnionPayCapabilities capabilities) {
        if (capabilities.isUnionPay()) {
            if (!capabilities.isSupported()) {
                mCardForm.setCardNumberError();
                Toast.makeText(this, "This card is not supported", Toast.LENGTH_SHORT).show();
                return;
            }
            mIsUnionPay = true;
            mEnrollmentId = null;

            mCardForm.setRequiredFields(this, true, true, true, mConfiguration.isPostalCodeChallengePresent(),
                    getString(R.string.purchase));

            mCountryCode.setVisibility(VISIBLE);
            mMobilePhone.setVisibility(VISIBLE);
            mSendSmsButton.setVisibility(VISIBLE);
        } else {
            mIsUnionPay = false;

            mCardForm.setRequiredFields(this, true, true, mConfiguration.isCvvChallengePresent(),
                    mConfiguration.isPostalCodeChallengePresent(), getString(R.string.purchase));

            if (!mConfiguration.isCvvChallengePresent()) {
                ((EditText) findViewById(R.id.bt_card_form_cvv)).setText("");
            }

            mCountryCode.setVisibility(GONE);
            mCountryCode.setText("");
            mMobilePhone.setVisibility(GONE);
            mMobilePhone.setText("");
            mSendSmsButton.setVisibility(GONE);
        }
    }

    public void sendSms(View v) {
        UnionPayCardBuilder unionPayCardBuilder = new UnionPayCardBuilder()
                .cardNumber(mCardForm.getCardNumber())
                .expirationMonth(mCardForm.getExpirationMonth())
                .expirationYear(mCardForm.getExpirationYear())
                .cvv(mCardForm.getCvv())
                .postalCode(mCardForm.getPostalCode())
                .mobileCountryCode(mCountryCode.getText().toString())
                .mobilePhoneNumber(mMobilePhone.getText().toString());

        UnionPay.enroll(mBraintreeFragment, unionPayCardBuilder);
    }

    @Override
    public void onSmsCodeSent(String enrollmentId, boolean smsCodeRequired) {
        mEnrollmentId = enrollmentId;
        if (smsCodeRequired) {
            mSmsCode.setVisibility(VISIBLE);
        } else {
            onCardFormSubmit();
        }
    }

    @Override
    public void onCardFormSubmit() {
        onPurchase(null);
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
        AndroidPay.getTokenizationParameters(mBraintreeFragment, new TokenizationParametersListener() {
            @Override
            public void onResult(PaymentMethodTokenizationParameters parameters,
                    Collection<Integer> allowedCardNetworks) {
                MaskedWalletRequest.Builder maskedWalletRequestBuilder =
                        MaskedWalletRequest.newBuilder()
                                .setMerchantName("Braintree")
                                .setCurrencyCode(mCart.getCurrencyCode())
                                .setCart(mCart)
                                .setEstimatedTotalPrice(mCart.getTotalPrice())
                                .setShippingAddressRequired(Settings.isAndroidPayShippingAddressRequired(CustomActivity.this))
                                .setPhoneNumberRequired(Settings.isAndroidPayPhoneNumberRequired(CustomActivity.this))
                                .setPaymentMethodTokenizationParameters(parameters)
                                .addAllowedCardNetworks(allowedCardNetworks);

                for (String country : Settings.getAndroidPayAllowedCountriesForShipping(CustomActivity.this)) {
                    maskedWalletRequestBuilder.addAllowedCountrySpecificationForShipping(
                            new CountrySpecification(country));
                }

                Wallet.Payments.loadMaskedWallet(mGoogleApiClient, maskedWalletRequestBuilder.build(),
                        ANDROID_PAY_MASKED_WALLET_REQUEST_CODE);
            }
        });
    }

    public void onPurchase(View v) {
        setProgressBarIndeterminateVisibility(true);

        if (mIsUnionPay) {
            UnionPayCardBuilder unionPayCardBuilder = new UnionPayCardBuilder()
                    .cardNumber(mCardForm.getCardNumber())
                    .expirationMonth(mCardForm.getExpirationMonth())
                    .expirationYear(mCardForm.getExpirationYear())
                    .cvv(mCardForm.getCvv())
                    .postalCode(mCardForm.getPostalCode())
                    .mobileCountryCode(mCountryCode.getText().toString())
                    .mobilePhoneNumber(mMobilePhone.getText().toString())
                    .smsCode(mSmsCode.getText().toString())
                    .enrollmentId(mEnrollmentId);

            UnionPay.tokenize(mBraintreeFragment, unionPayCardBuilder);
        } else {
            CardBuilder cardBuilder = new CardBuilder()
                    .cardNumber(mCardForm.getCardNumber())
                    .expirationMonth(mCardForm.getExpirationMonth())
                    .expirationYear(mCardForm.getExpirationYear())
                    .cvv(mCardForm.getCvv())
                    .postalCode(mCardForm.getPostalCode());

            Card.tokenize(mBraintreeFragment, cardBuilder);
        }
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
