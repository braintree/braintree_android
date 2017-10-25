package com.braintreepayments.demo;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.braintreepayments.api.AmericanExpress;
import com.braintreepayments.api.BraintreeFragment;
import com.braintreepayments.api.Card;
import com.braintreepayments.api.DataCollector;
import com.braintreepayments.api.ThreeDSecure;
import com.braintreepayments.api.UnionPay;
import com.braintreepayments.api.exceptions.InvalidArgumentException;
import com.braintreepayments.api.interfaces.AmericanExpressListener;
import com.braintreepayments.api.interfaces.BraintreeErrorListener;
import com.braintreepayments.api.interfaces.BraintreeResponseListener;
import com.braintreepayments.api.interfaces.ConfigurationListener;
import com.braintreepayments.api.interfaces.PaymentMethodNonceCreatedListener;
import com.braintreepayments.api.interfaces.UnionPayListener;
import com.braintreepayments.api.models.AmericanExpressRewardsBalance;
import com.braintreepayments.api.models.CardBuilder;
import com.braintreepayments.api.models.CardNonce;
import com.braintreepayments.api.models.Configuration;
import com.braintreepayments.api.models.PaymentMethodNonce;
import com.braintreepayments.api.models.UnionPayCapabilities;
import com.braintreepayments.api.models.UnionPayCardBuilder;
import com.braintreepayments.cardform.OnCardFormFieldFocusedListener;
import com.braintreepayments.cardform.OnCardFormSubmitListener;
import com.braintreepayments.cardform.utils.CardType;
import com.braintreepayments.cardform.view.CardEditText;
import com.braintreepayments.cardform.view.CardForm;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

public class CardActivity extends BaseActivity implements ConfigurationListener, UnionPayListener,
        PaymentMethodNonceCreatedListener, BraintreeErrorListener, OnCardFormSubmitListener,
        OnCardFormFieldFocusedListener, AmericanExpressListener {

    private static final String EXTRA_UNIONPAY = "com.braintreepayments.demo.EXTRA_UNIONPAY";
    private static final String EXTRA_UNIONPAY_ENROLLMENT_ID = "com.braintreepayments.demo.EXTRA_UNIONPAY_ENROLLMENT_ID";

    private Configuration mConfiguration;
    private String mDeviceData;
    private boolean mIsUnionPay;
    private String mEnrollmentId;
    private boolean mThreeDSecureRequested;

    private ProgressDialog mLoading;
    private CardForm mCardForm;
    private TextInputLayout mSmsCodeContainer;
    private EditText mSmsCode;
    private Button mSendSmsButton;
    private Button mPurchaseButton;

    private CardType mCardType;

    @Override
    protected void onCreate(Bundle onSaveInstanceState) {
        super.onCreate(onSaveInstanceState);

        setContentView(R.layout.custom_activity);
        setUpAsBack();

        mCardForm = (CardForm) findViewById(R.id.card_form);
        mCardForm.setOnFormFieldFocusedListener(this);
        mCardForm.setOnCardFormSubmitListener(this);

        mSmsCodeContainer = (TextInputLayout) findViewById(R.id.sms_code_container);
        mSmsCode = (EditText) findViewById(R.id.sms_code);
        mSendSmsButton = (Button) findViewById(R.id.unionpay_enroll_button);
        mPurchaseButton = (Button) findViewById(R.id.purchase_button);

        if (onSaveInstanceState != null) {
            mIsUnionPay = onSaveInstanceState.getBoolean(EXTRA_UNIONPAY);
            mEnrollmentId = onSaveInstanceState.getString(EXTRA_UNIONPAY_ENROLLMENT_ID);

            if (mIsUnionPay) {
                mSendSmsButton.setVisibility(VISIBLE);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        safelyCloseLoadingView();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(EXTRA_UNIONPAY, mIsUnionPay);
        outState.putString(EXTRA_UNIONPAY_ENROLLMENT_ID, mEnrollmentId);
    }

    @Override
    protected void reset() {
        mThreeDSecureRequested = false;
        mPurchaseButton.setEnabled(false);
    }

    @Override
    protected void onAuthorizationFetched() {
        try {
            mBraintreeFragment = BraintreeFragment.newInstance(this, mAuthorization);
        } catch (InvalidArgumentException e) {
            onError(e);
        }

        mPurchaseButton.setEnabled(true);
    }

    @Override
    public void onConfigurationFetched(Configuration configuration) {
        mConfiguration = configuration;

        mCardForm.cardRequired(true)
                .expirationRequired(true)
                .cvvRequired(configuration.isCvvChallengePresent())
                .postalCodeRequired(configuration.isPostalCodeChallengePresent())
                .mobileNumberRequired(false)
                .actionLabel(getString(R.string.purchase))
                .setup(this);

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
    public void onError(Exception error) {
        super.onError(error);

        mThreeDSecureRequested = false;
    }

    @Override
    public void onCancel(int requestCode) {
        super.onCancel(requestCode);

        mThreeDSecureRequested = false;
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
        mSmsCodeContainer.setVisibility(GONE);
        mSmsCode.setText("");

        if (capabilities.isUnionPay()) {
            if (!capabilities.isSupported()) {
                mCardForm.setCardNumberError(getString(R.string.bt_card_not_accepted));
                return;
            }
            mIsUnionPay = true;
            mEnrollmentId = null;

            mCardForm.cardRequired(true)
                    .expirationRequired(true)
                    .cvvRequired(true)
                    .postalCodeRequired(mConfiguration.isPostalCodeChallengePresent())
                    .mobileNumberRequired(true)
                    .actionLabel(getString(R.string.purchase))
                    .setup(this);

            mSendSmsButton.setVisibility(VISIBLE);
        } else {
            mIsUnionPay = false;

            mCardForm.cardRequired(true)
                    .expirationRequired(true)
                    .cvvRequired(mConfiguration.isCvvChallengePresent())
                    .postalCodeRequired(mConfiguration.isPostalCodeChallengePresent())
                    .mobileNumberRequired(false)
                    .actionLabel(getString(R.string.purchase))
                    .setup(this);

            if (!mConfiguration.isCvvChallengePresent()) {
                ((EditText) findViewById(R.id.bt_card_form_cvv)).setText("");
            }
        }
    }

    public void sendSms(View v) {
        UnionPayCardBuilder unionPayCardBuilder = new UnionPayCardBuilder()
                .cardNumber(mCardForm.getCardNumber())
                .expirationMonth(mCardForm.getExpirationMonth())
                .expirationYear(mCardForm.getExpirationYear())
                .cvv(mCardForm.getCvv())
                .postalCode(mCardForm.getPostalCode())
                .mobileCountryCode(mCardForm.getCountryCode())
                .mobilePhoneNumber(mCardForm.getMobileNumber());

        UnionPay.enroll(mBraintreeFragment, unionPayCardBuilder);
    }

    @Override
    public void onSmsCodeSent(String enrollmentId, boolean smsCodeRequired) {
        mEnrollmentId = enrollmentId;
        if (smsCodeRequired) {
            mSmsCodeContainer.setVisibility(VISIBLE);
        } else {
            onCardFormSubmit();
        }
    }

    @Override
    public void onCardFormSubmit() {
        onPurchase(null);
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
                    .mobileCountryCode(mCardForm.getCountryCode())
                    .mobilePhoneNumber(mCardForm.getMobileNumber())
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
    public void onPaymentMethodNonceCreated(PaymentMethodNonce paymentMethodNonce) {
        super.onPaymentMethodNonceCreated(paymentMethodNonce);

        if (!mThreeDSecureRequested && paymentMethodNonce instanceof CardNonce && Settings.isThreeDSecureEnabled(this)) {
            mThreeDSecureRequested = true;
            mLoading = ProgressDialog.show(this, getString(R.string.loading), getString(R.string.loading), true, false);
            ThreeDSecure.performVerification(mBraintreeFragment, paymentMethodNonce.getNonce(), "1");
        } else if (paymentMethodNonce instanceof CardNonce && Settings.isAmexRewardsBalanceEnabled(this)) {
            mLoading = ProgressDialog.show(this, getString(R.string.loading), getString(R.string.loading), true, false);
            AmericanExpress.getRewardsBalance(mBraintreeFragment, paymentMethodNonce.getNonce(), "USD");
        } else {
            Intent intent = new Intent()
                    .putExtra(MainActivity.EXTRA_PAYMENT_METHOD_NONCE, paymentMethodNonce)
                    .putExtra(MainActivity.EXTRA_DEVICE_DATA, mDeviceData);
            setResult(RESULT_OK, intent);
            finish();
        }
    }

    @Override
    public void onRewardsBalanceFetched(AmericanExpressRewardsBalance rewardsBalance) {
        safelyCloseLoadingView();
        ProgressDialog.show(this, null, getAmexRewardsBalanceString(rewardsBalance), true, true);
    }

    private void safelyCloseLoadingView() {
        if (mLoading != null && mLoading.isShowing()) {
            mLoading.dismiss();
        }
    }

    public static String getDisplayString(CardNonce nonce) {
        return "Card Last Two: " + nonce.getLastTwo() + "\n" +
                getDisplayString(nonce.getBinData()) + "\n" +
                "3DS: \n" +
                "         - isLiabilityShifted: " + nonce.getThreeDSecureInfo().isLiabilityShifted() + "\n" +
                "         - isLiabilityShiftPossible: " + nonce.getThreeDSecureInfo().isLiabilityShiftPossible();
    }

    public static String getAmexRewardsBalanceString(AmericanExpressRewardsBalance rewardsBalance) {
        return  "Amex Rewards Balance: \n" +
                "- amount: " + rewardsBalance.getRewardsAmount() + "\n" +
                "- errorCode: " + rewardsBalance.getErrorCode();
    }
}
