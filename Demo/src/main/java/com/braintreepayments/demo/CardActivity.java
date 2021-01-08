package com.braintreepayments.demo;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.braintreepayments.api.AmericanExpressClient;
import com.braintreepayments.api.AmericanExpressGetRewardsBalanceCallback;
import com.braintreepayments.api.BraintreeClient;
import com.braintreepayments.api.BraintreeDataCollectorCallback;
import com.braintreepayments.api.CardTokenizeCallback;
import com.braintreepayments.api.ConfigurationCallback;
import com.braintreepayments.api.ThreeDSecureVerificationCallback;
import com.braintreepayments.api.UnionPayFetchCapabilitiesCallback;
import com.braintreepayments.api.UnionPayEnrollment;
import com.braintreepayments.api.UnionPayEnrollCallback;
import com.braintreepayments.api.UnionPayTokenizeCallback;
import com.braintreepayments.api.exceptions.InvalidArgumentException;
import com.braintreepayments.api.interfaces.ThreeDSecureLookupCallback;
import com.braintreepayments.api.AmericanExpressRewardsBalance;
import com.braintreepayments.api.models.Authorization;
import com.braintreepayments.api.models.CardBuilder;
import com.braintreepayments.api.models.CardNonce;
import com.braintreepayments.api.models.Configuration;
import com.braintreepayments.api.models.PaymentMethodNonce;
import com.braintreepayments.api.models.ThreeDSecureAdditionalInformation;
import com.braintreepayments.api.models.ThreeDSecureLookup;
import com.braintreepayments.api.models.ThreeDSecurePostalAddress;
import com.braintreepayments.api.models.ThreeDSecureRequest;
import com.braintreepayments.api.models.ThreeDSecureV1UiCustomization;
import com.braintreepayments.api.models.UnionPayCapabilities;
import com.braintreepayments.api.models.UnionPayCardBuilder;
import com.braintreepayments.cardform.OnCardFormFieldFocusedListener;
import com.braintreepayments.cardform.OnCardFormSubmitListener;
import com.braintreepayments.cardform.utils.CardType;
import com.braintreepayments.cardform.view.CardEditText;
import com.braintreepayments.cardform.view.CardForm;
import com.cardinalcommerce.shared.userinterfaces.ToolbarCustomization;
import com.cardinalcommerce.shared.userinterfaces.UiCustomization;
import com.google.android.material.textfield.TextInputLayout;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

public class CardActivity extends BaseActivity implements OnCardFormSubmitListener, OnCardFormFieldFocusedListener {

    private static final String EXTRA_THREE_D_SECURE_REQUESTED = "com.braintreepayments.demo.EXTRA_THREE_D_SECURE_REQUESTED";
    private static final String EXTRA_UNIONPAY = "com.braintreepayments.demo.EXTRA_UNIONPAY";
    private static final String EXTRA_UNIONPAY_ENROLLMENT_ID = "com.braintreepayments.demo.EXTRA_UNIONPAY_ENROLLMENT_ID";

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

    private BraintreeClient braintreeClient;
    private AmericanExpressClient americanExpressClient;

    @Override
    protected void onCreate(Bundle onSaveInstanceState) {
        super.onCreate(onSaveInstanceState);

        setContentView(R.layout.custom_activity);
        setUpAsBack();

        mCardForm = findViewById(R.id.card_form);
        mCardForm.setOnFormFieldFocusedListener(this);
        mCardForm.setOnCardFormSubmitListener(this);

        mSmsCodeContainer = findViewById(R.id.sms_code_container);
        mSmsCode = findViewById(R.id.sms_code);
        mSendSmsButton = findViewById(R.id.unionpay_enroll_button);
        mPurchaseButton = findViewById(R.id.purchase_button);

        if (onSaveInstanceState != null) {
            mThreeDSecureRequested = onSaveInstanceState.getBoolean(EXTRA_THREE_D_SECURE_REQUESTED);
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
        outState.putBoolean(EXTRA_THREE_D_SECURE_REQUESTED, mThreeDSecureRequested);
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
            Authorization authorization = Authorization.fromString(mAuthorization);
            braintreeClient = new BraintreeClient(authorization, this, RETURN_URL_SCHEME);
            americanExpressClient = new AmericanExpressClient(braintreeClient);

        } catch (InvalidArgumentException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onBraintreeInitialized() {
        mPurchaseButton.setEnabled(true);

        getConfiguration(new ConfigurationCallback() {
            @Override
            public void onResult(@Nullable Configuration configuration, @Nullable Exception error) {
                mCardForm.cardRequired(true)
                        .expirationRequired(true)
                        .cvvRequired(configuration.isCvvChallengePresent())
                        .postalCodeRequired(configuration.isPostalCodeChallengePresent())
                        .mobileNumberRequired(false)
                        .actionLabel(getString(R.string.purchase))
                        .setup(CardActivity.this);

                if (getIntent().getBooleanExtra(MainActivity.EXTRA_COLLECT_DEVICE_DATA, false)) {
                    collectDeviceData(new BraintreeDataCollectorCallback() {
                        @Override
                        public void onResult(@Nullable String deviceData, @Nullable Exception error) {
                            mDeviceData = deviceData;
                        }
                    });
                }
            }
        });
    }

    @Override
    protected void onBraintreeError(Exception error) {
        handleError(error);
    }

    @Override
    protected void handleError(Exception error) {
        super.handleError(error);
        mThreeDSecureRequested = false;
    }

    @Override
    public void onCancel(int requestCode) {
        super.onCancel(requestCode);

        mThreeDSecureRequested = false;

        Toast.makeText(this, "3DS canceled", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onCardFormFieldFocused(View field) {

        if (!(field instanceof CardEditText) && !TextUtils.isEmpty(mCardForm.getCardNumber())) {
            CardType cardType = CardType.forCardNumber(mCardForm.getCardNumber());
            if (mCardType != cardType) {
                mCardType = cardType;

                if (!Settings.useTokenizationKey(getApplicationContext())) {
                    String cardNumber = mCardForm.getCardNumber();
                    fetchUnionPayCapabilities(cardNumber, new UnionPayFetchCapabilitiesCallback() {
                        @Override
                        public void onResult(UnionPayCapabilities capabilities, Exception error) {
                            if (capabilities != null) {
                                handleUnionPayCapabilitiesFetched(capabilities);
                            } else {
                                onBraintreeError(error);
                            }
                        }
                    });
                }
            }
        }
    }

    private void handleUnionPayCapabilitiesFetched(final UnionPayCapabilities capabilities) {
        mSmsCodeContainer.setVisibility(GONE);
        mSmsCode.setText("");

        final AppCompatActivity activity = this;
        getConfiguration(new ConfigurationCallback() {
            @Override
            public void onResult(@Nullable Configuration configuration, @Nullable Exception error) {
                if (capabilities.isUnionPay()) {
                    if (!capabilities.isSupported()) {
                        mCardForm.setCardNumberError("Card not accepted");
                        return;
                    }
                    mIsUnionPay = true;
                    mEnrollmentId = null;

                    mCardForm.cardRequired(true)
                            .expirationRequired(true)
                            .cvvRequired(true)
                            .postalCodeRequired(configuration.isPostalCodeChallengePresent())
                            .mobileNumberRequired(true)
                            .actionLabel(getString(R.string.purchase))
                            .setup(activity);

                    mSendSmsButton.setVisibility(VISIBLE);
                } else {
                    mIsUnionPay = false;

                    mCardForm.cardRequired(true)
                            .expirationRequired(true)
                            .cvvRequired(configuration.isCvvChallengePresent())
                            .postalCodeRequired(configuration.isPostalCodeChallengePresent())
                            .mobileNumberRequired(false)
                            .actionLabel(getString(R.string.purchase))
                            .setup(activity);

                    if (!configuration.isCvvChallengePresent()) {
                        ((EditText) findViewById(R.id.bt_card_form_cvv)).setText("");
                    }
                }
            }
        });
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

        enrollUnionPay(unionPayCardBuilder, new UnionPayEnrollCallback() {
            @Override
            public void onResult(@Nullable UnionPayEnrollment enrollment, @Nullable Exception error) {
                mEnrollmentId = enrollment.getId();
                if (enrollment.isSmsCodeRequired()) {
                    mSmsCodeContainer.setVisibility(VISIBLE);
                } else {
                    onCardFormSubmit();
                }
            }
        });
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

            tokenizeUnionPay(unionPayCardBuilder, new UnionPayTokenizeCallback() {
                @Override
                public void onResult(PaymentMethodNonce paymentMethodNonce, Exception error) {
                    if (paymentMethodNonce != null) {
                        handlePaymentMethodNonceCreated(paymentMethodNonce);
                    } else {
                        onBraintreeError(error);
                    }
                }
            });

        } else {
            CardBuilder cardBuilder = new CardBuilder()
                    .cardNumber(mCardForm.getCardNumber())
                    .expirationMonth(mCardForm.getExpirationMonth())
                    .expirationYear(mCardForm.getExpirationYear())
                    .cvv(mCardForm.getCvv())
                    .validate(false) // TODO GQL currently only returns the bin if validate = false
                    .postalCode(mCardForm.getPostalCode());

            tokenizeCard(cardBuilder, new CardTokenizeCallback() {
                @Override
                public void onResult(PaymentMethodNonce paymentMethodNonce, Exception error) {
                    if (paymentMethodNonce != null) {
                        handlePaymentMethodNonceCreated(paymentMethodNonce);
                    } else {
                        onBraintreeError(error);
                    }
                }
            });
        }
    }

    @Override
    protected void onThreeDSecureResult(PaymentMethodNonce paymentMethodNonce, Exception error) {
        if (paymentMethodNonce != null) {
            handlePaymentMethodNonceCreated(paymentMethodNonce);
        } else {
            onBraintreeError(error);
        }
    }

    private void handlePaymentMethodNonceCreated(PaymentMethodNonce paymentMethodNonce) {
        super.onPaymentMethodNonceCreated(paymentMethodNonce);

        if (!mThreeDSecureRequested && paymentMethodNonce instanceof CardNonce && Settings.isThreeDSecureEnabled(this)) {
            mThreeDSecureRequested = true;
            mLoading = ProgressDialog.show(this, getString(R.string.loading), getString(R.string.loading), true, false);

            performThreeDSecureVerification(threeDSecureRequest(paymentMethodNonce), new ThreeDSecureLookupCallback() {
                @Override
                public void onResult(ThreeDSecureRequest request, ThreeDSecureLookup lookup, Exception error) {
                    if (request != null && lookup != null) {
                        continuePerformVerification(request, lookup, new ThreeDSecureVerificationCallback() {
                            @Override
                            public void onResult(@Nullable PaymentMethodNonce paymentMethodNonce, @Nullable Exception error) {
                                if (paymentMethodNonce != null) {
                                    handlePaymentMethodNonceCreated(paymentMethodNonce);
                                } else {
                                    handleError(error);
                                }
                            }
                        });
                    } else {
                        handleError(error);
                    }
                }
            });
        } else if (paymentMethodNonce instanceof CardNonce && Settings.isAmexRewardsBalanceEnabled(this)) {
            mLoading = ProgressDialog.show(this, getString(R.string.loading), getString(R.string.loading), true, false);
            String nonce = paymentMethodNonce.getNonce();

            americanExpressClient.getRewardsBalance(nonce, "USD", new AmericanExpressGetRewardsBalanceCallback() {
                @Override
                public void onResult(@Nullable AmericanExpressRewardsBalance rewardsBalance, @Nullable Exception error) {
                    if (rewardsBalance != null) {
                        safelyCloseLoadingView();
                        showDialog(getAmexRewardsBalanceString(rewardsBalance));
                    } else if (error != null) {
                        handleError(error);
                    }
                }
            });
        } else {
            Intent intent = new Intent()
                    .putExtra(MainActivity.EXTRA_PAYMENT_RESULT, paymentMethodNonce)
                    .putExtra(MainActivity.EXTRA_DEVICE_DATA, mDeviceData);
            setResult(RESULT_OK, intent);
            finish();
        }
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
                "         - isLiabilityShiftPossible: " + nonce.getThreeDSecureInfo().isLiabilityShiftPossible() + "\n" +
                "         - wasVerified: " + nonce.getThreeDSecureInfo().wasVerified();
    }

    public static String getAmexRewardsBalanceString(AmericanExpressRewardsBalance rewardsBalance) {
        return "Amex Rewards Balance: \n" +
                "- amount: " + rewardsBalance.getRewardsAmount() + "\n" +
                "- errorCode: " + rewardsBalance.getErrorCode();
    }

    private ThreeDSecureRequest threeDSecureRequest(PaymentMethodNonce paymentMethodNonce) {
        CardNonce cardNonce = (CardNonce) paymentMethodNonce;

        ThreeDSecurePostalAddress billingAddress = new ThreeDSecurePostalAddress()
                .givenName("Jill")
                .surname("Doe")
                .phoneNumber("5551234567")
                .streetAddress("555 Smith St")
                .extendedAddress("#2")
                .locality("Chicago")
                .region("IL")
                .postalCode("12345")
                .countryCodeAlpha2("US");

        ThreeDSecureAdditionalInformation additionalInformation = new ThreeDSecureAdditionalInformation()
                .accountId("account-id");

        ToolbarCustomization toolbarCustomization = new ToolbarCustomization();
        toolbarCustomization.setHeaderText("Braintree 3DS Checkout");
        toolbarCustomization.setBackgroundColor("#FF5A5F");
        toolbarCustomization.setButtonText("Close");
        toolbarCustomization.setTextColor("#222222");
        toolbarCustomization.setTextFontSize(18);

        UiCustomization uiCustomization = new UiCustomization();
        uiCustomization.setToolbarCustomization(toolbarCustomization);

        ThreeDSecureV1UiCustomization v1UiCustomization = new ThreeDSecureV1UiCustomization()
                .redirectButtonText("Return to Demo App")
                .redirectDescription("Please use the button above if you are not automatically redirected to the app. (This text can contain accéntéd chàractèrs.)");

        return new ThreeDSecureRequest()
                .amount("10")
                .email("test@email.com")
                .billingAddress(billingAddress)
                .nonce(cardNonce.getNonce())
                .versionRequested(ThreeDSecureRequest.VERSION_2)
                .additionalInformation(additionalInformation)
                .uiCustomization(uiCustomization)
                .v1UiCustomization(v1UiCustomization);
    }
}
