package com.braintreepayments.demo;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
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
import com.braintreepayments.api.AmericanExpressRewardsBalance;
import com.braintreepayments.api.BraintreeClient;
import com.braintreepayments.api.BrowserSwitchCallback;
import com.braintreepayments.api.BrowserSwitchResult;
import com.braintreepayments.api.CardClient;
import com.braintreepayments.api.CardTokenizeCallback;
import com.braintreepayments.api.ConfigurationCallback;
import com.braintreepayments.api.DataCollector;
import com.braintreepayments.api.DataCollectorCallback;
import com.braintreepayments.api.ThreeDSecureAdditionalInformation;
import com.braintreepayments.api.ThreeDSecureClient;
import com.braintreepayments.api.ThreeDSecureLookup;
import com.braintreepayments.api.ThreeDSecureLookupCallback;
import com.braintreepayments.api.ThreeDSecurePostalAddress;
import com.braintreepayments.api.ThreeDSecureRequest;
import com.braintreepayments.api.ThreeDSecureResultCallback;
import com.braintreepayments.api.ThreeDSecureV1UiCustomization;
import com.braintreepayments.api.UnionPayClient;
import com.braintreepayments.api.UnionPayEnrollCallback;
import com.braintreepayments.api.UnionPayEnrollment;
import com.braintreepayments.api.UnionPayFetchCapabilitiesCallback;
import com.braintreepayments.api.UnionPayTokenizeCallback;
import com.braintreepayments.api.InvalidArgumentException;
import com.braintreepayments.api.Authorization;
import com.braintreepayments.api.CardBuilder;
import com.braintreepayments.api.CardNonce;
import com.braintreepayments.api.Configuration;
import com.braintreepayments.api.PaymentMethodNonce;
import com.braintreepayments.api.UnionPayCapabilities;
import com.braintreepayments.api.UnionPayCardBuilder;
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

public class CardActivity extends BaseActivity implements OnCardFormSubmitListener, OnCardFormFieldFocusedListener, BrowserSwitchCallback {

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
    private CardClient cardClient;
    private ThreeDSecureClient threeDSecureClient;
    private UnionPayClient unionPayClient;
    private DataCollector dataCollector;

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
            cardClient = new CardClient(braintreeClient);
            threeDSecureClient = new ThreeDSecureClient(braintreeClient, RETURN_URL_SCHEME);
            unionPayClient = new UnionPayClient(braintreeClient);
            dataCollector = new DataCollector(braintreeClient);

        } catch (InvalidArgumentException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onBraintreeInitialized() {
        mPurchaseButton.setEnabled(true);

        braintreeClient.getConfiguration(new ConfigurationCallback() {
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
                    dataCollector.collectDeviceData(CardActivity.this, new DataCollectorCallback() {
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
                    unionPayClient.fetchCapabilities(cardNumber, new UnionPayFetchCapabilitiesCallback() {
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
        braintreeClient.getConfiguration(new ConfigurationCallback() {
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

        unionPayClient.enroll(unionPayCardBuilder, new UnionPayEnrollCallback() {
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

            unionPayClient.tokenize(unionPayCardBuilder, new UnionPayTokenizeCallback() {
                @Override
                public void onResult(CardNonce cardNonce, Exception error) {
                    if (cardNonce != null) {
                        handlePaymentMethodNonceCreated(cardNonce);
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

            cardClient.tokenize(this, cardBuilder, new CardTokenizeCallback() {
                @Override
                public void onResult(CardNonce cardNonce, Exception error) {
                    if (cardNonce != null) {
                        handlePaymentMethodNonceCreated(cardNonce);
                    } else {
                        onBraintreeError(error);
                    }
                }
            });
        }
    }

    private void handleThreeDSecureResult(PaymentMethodNonce paymentMethodNonce, Exception error) {
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

            threeDSecureClient.performVerification(this, threeDSecureRequest(paymentMethodNonce), new ThreeDSecureLookupCallback() {
                @Override
                public void onResult(ThreeDSecureRequest request, ThreeDSecureLookup lookup, Exception error) {
                    if (request != null && lookup != null) {
                        threeDSecureClient.continuePerformVerification(CardActivity.this, request, lookup, new ThreeDSecureResultCallback() {
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

    @Override
    public void onResult(int requestCode, BrowserSwitchResult browserSwitchResult, @Nullable Uri uri) {
        threeDSecureClient.onBrowserSwitchResult(browserSwitchResult, uri, new ThreeDSecureResultCallback() {
            @Override
            public void onResult(@Nullable PaymentMethodNonce paymentMethodNonce, @Nullable Exception error) {
                handleThreeDSecureResult(paymentMethodNonce, error);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        threeDSecureClient.onActivityResult(resultCode, data, new ThreeDSecureResultCallback() {
            @Override
            public void onResult(@Nullable PaymentMethodNonce paymentMethodNonce, @Nullable Exception error) {
                handleThreeDSecureResult(paymentMethodNonce, error);
            }
        });
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
