package com.braintreepayments.demo;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.braintreepayments.InitializeFeatureClientsCallback;
import com.braintreepayments.api.AmericanExpressClient;
import com.braintreepayments.api.AmericanExpressRewardsBalance;
import com.braintreepayments.api.BrowserSwitchResult;
import com.braintreepayments.api.CardBuilder;
import com.braintreepayments.api.CardClient;
import com.braintreepayments.api.CardNonce;
import com.braintreepayments.api.DataCollector;
import com.braintreepayments.api.PaymentMethodNonce;
import com.braintreepayments.api.ThreeDSecureAdditionalInformation;
import com.braintreepayments.api.ThreeDSecureClient;
import com.braintreepayments.api.ThreeDSecurePostalAddress;
import com.braintreepayments.api.ThreeDSecureRequest;
import com.braintreepayments.api.ThreeDSecureResult;
import com.braintreepayments.api.ThreeDSecureV1UiCustomization;
import com.braintreepayments.api.ThreeDSecureV2ToolbarCustomization;
import com.braintreepayments.api.ThreeDSecureV2UiCustomization;
import com.braintreepayments.api.UnionPayCapabilities;
import com.braintreepayments.api.UnionPayCardBuilder;
import com.braintreepayments.api.UnionPayClient;
import com.braintreepayments.cardform.OnCardFormFieldFocusedListener;
import com.braintreepayments.cardform.OnCardFormSubmitListener;
import com.braintreepayments.cardform.utils.CardType;
import com.braintreepayments.cardform.view.CardEditText;
import com.braintreepayments.cardform.view.CardForm;
import com.google.android.material.textfield.TextInputLayout;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

public class CardFragment extends BaseFragment implements OnCardFormSubmitListener, OnCardFormFieldFocusedListener {

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

    private AmericanExpressClient americanExpressClient;
    private CardClient cardClient;
    private ThreeDSecureClient threeDSecureClient;
    private UnionPayClient unionPayClient;
    private DataCollector dataCollector;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_card, container, false);

        mCardForm = view.findViewById(R.id.card_form);
        mCardForm.setOnFormFieldFocusedListener(this);
        mCardForm.setOnCardFormSubmitListener(this);

        mSmsCodeContainer = view.findViewById(R.id.sms_code_container);
        mSmsCode = view.findViewById(R.id.sms_code);
        mSendSmsButton = view.findViewById(R.id.unionpay_enroll_button);
        mPurchaseButton = view.findViewById(R.id.purchase_button);

        mSendSmsButton.setOnClickListener(this::sendSms);
        mPurchaseButton.setOnClickListener(this::onPurchase);

        if (mIsUnionPay) {
            mSendSmsButton.setVisibility(VISIBLE);
        }

        DemoViewModel viewModel = new ViewModelProvider(getActivity()).get(DemoViewModel.class);
        viewModel.getThreeDSecureBrowserSwitchResult().observe(getViewLifecycleOwner(), this::handleThreeDSecureBrowserSwitchResult);
        viewModel.getThreeDSecureActivityResult().observe(getViewLifecycleOwner(), this::handleThreeDSecureActivityResult);

        return view;
    }

    @Override
    public void onCreate(Bundle onSaveInstanceState) {
        super.onCreate(onSaveInstanceState);

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
    public void onResume() {
        super.onResume();
        safelyCloseLoadingView();

        // initializing clients checks if union pay is enabled
        initializeFeatureClients(error -> { /* do nothing */ });
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(EXTRA_THREE_D_SECURE_REQUESTED, mThreeDSecureRequested);
        outState.putBoolean(EXTRA_UNIONPAY, mIsUnionPay);
        outState.putString(EXTRA_UNIONPAY_ENROLLMENT_ID, mEnrollmentId);
    }

    private void initializeFeatureClients(InitializeFeatureClientsCallback callback) {
        getBraintreeClient(braintreeClient -> {
            final AppCompatActivity activity = (AppCompatActivity) getActivity();
            americanExpressClient = new AmericanExpressClient(braintreeClient);
            cardClient = new CardClient(braintreeClient);
            threeDSecureClient = new ThreeDSecureClient(braintreeClient);
            unionPayClient = new UnionPayClient(braintreeClient);
            dataCollector = new DataCollector(braintreeClient);

            mPurchaseButton.setEnabled(true);

            braintreeClient.getConfiguration((configuration, configError) -> {
                mCardForm.cardRequired(true)
                        .expirationRequired(true)
                        .cvvRequired(configuration.isCvvChallengePresent())
                        .postalCodeRequired(configuration.isPostalCodeChallengePresent())
                        .mobileNumberRequired(false)
                        .actionLabel(getString(R.string.purchase))
                        .setup(activity);

                if (getArguments().getBoolean(MainFragment.EXTRA_COLLECT_DEVICE_DATA, false)) {
                    dataCollector.collectDeviceData(activity, (deviceData, e) -> mDeviceData = deviceData);
                }
            });

        });
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

        Toast.makeText(getActivity(), "3DS canceled", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onCardFormFieldFocused(View field) {
        if (!(field instanceof CardEditText) && !TextUtils.isEmpty(mCardForm.getCardNumber())) {
            CardType cardType = CardType.forCardNumber(mCardForm.getCardNumber());
            if (mCardType != cardType) {
                mCardType = cardType;

                if (!Settings.useTokenizationKey(getActivity().getApplicationContext())) {
                    String cardNumber = mCardForm.getCardNumber();
                    unionPayClient.fetchCapabilities(cardNumber, (capabilities, error) -> {
                        if (capabilities != null) {
                            handleUnionPayCapabilitiesFetched(capabilities);
                        } else {
                            handleError(error);
                        }
                    });
                }
            }
        }
    }

    private void handleUnionPayCapabilitiesFetched(final UnionPayCapabilities capabilities) {
        mSmsCodeContainer.setVisibility(GONE);
        mSmsCode.setText("");

        final AppCompatActivity activity = (AppCompatActivity) getActivity();
        getBraintreeClient((braintreeClient) -> braintreeClient.getConfiguration((configuration, error) -> {
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
                    ((EditText) activity.findViewById(R.id.bt_card_form_cvv)).setText("");
                }
            }
        }));
    }

    public void sendSms(View v) {
        UnionPayCardBuilder unionPayCardBuilder = new UnionPayCardBuilder();
        unionPayCardBuilder.setCardNumber(mCardForm.getCardNumber());
        unionPayCardBuilder.setExpirationMonth(mCardForm.getExpirationMonth());
        unionPayCardBuilder.setExpirationYear(mCardForm.getExpirationYear());
        unionPayCardBuilder.setCVV(mCardForm.getCvv());
        unionPayCardBuilder.setPostalCode(mCardForm.getPostalCode());
        unionPayCardBuilder.setMobileCountryCode(mCardForm.getCountryCode());
        unionPayCardBuilder.setMobilePhoneNumber(mCardForm.getMobileNumber());

        unionPayClient.enroll(unionPayCardBuilder, (enrollment, error) -> {
            mEnrollmentId = enrollment.getId();
            if (enrollment.isSmsCodeRequired()) {
                mSmsCodeContainer.setVisibility(VISIBLE);
            } else {
                onCardFormSubmit();
            }
        });
    }

    @Override
    public void onCardFormSubmit() {
        onPurchase(null);
    }

    public void onPurchase(View v) {
        getActivity().setProgressBarIndeterminateVisibility(true);
        if (mIsUnionPay) {
            UnionPayCardBuilder unionPayCardBuilder = new UnionPayCardBuilder();
            unionPayCardBuilder.setCardNumber(mCardForm.getCardNumber());
            unionPayCardBuilder.setExpirationMonth(mCardForm.getExpirationMonth());
            unionPayCardBuilder.setExpirationYear(mCardForm.getExpirationYear());
            unionPayCardBuilder.setCVV(mCardForm.getCvv());
            unionPayCardBuilder.setPostalCode(mCardForm.getPostalCode());
            unionPayCardBuilder.setMobileCountryCode(mCardForm.getCountryCode());
            unionPayCardBuilder.setMobilePhoneNumber(mCardForm.getMobileNumber());
            unionPayCardBuilder.setSmsCode(mSmsCode.getText().toString());
            unionPayCardBuilder.setEnrollmentId(mEnrollmentId);

            unionPayClient.tokenize(unionPayCardBuilder, (cardNonce, tokenizeError) -> {
                if (cardNonce != null) {
                    handlePaymentMethodNonceCreated(cardNonce);
                } else {
                    handleError(tokenizeError);
                }
            });

        } else {
            CardBuilder cardBuilder = new CardBuilder();
            cardBuilder.setCardNumber(mCardForm.getCardNumber());
            cardBuilder.setExpirationMonth(mCardForm.getExpirationMonth());
            cardBuilder.setExpirationYear(mCardForm.getExpirationYear());
            cardBuilder.setCVV(mCardForm.getCvv());
            cardBuilder.setValidate(false); // TODO GQL currently only returns the bin if validate = false
            cardBuilder.setPostalCode(mCardForm.getPostalCode());

            cardClient.tokenize(getActivity(), cardBuilder, (cardNonce, tokenizeError) -> {
                if (cardNonce != null) {
                    handlePaymentMethodNonceCreated(cardNonce);
                } else {
                    handleError(tokenizeError);
                }
            });
        }
    }

    private void handleThreeDSecureResult(ThreeDSecureResult threeDSecureResult, Exception error) {
        safelyCloseLoadingView();
        if (threeDSecureResult != null) {
            PaymentMethodNonce paymentMethodNonce = threeDSecureResult.getTokenizedCard();
            handlePaymentMethodNonceCreated(paymentMethodNonce);
        } else {
            handleError(error);
        }
    }

    private void handleThreeDSecureBrowserSwitchResult(BrowserSwitchResult browserSwitchResult) {
        if (browserSwitchResult != null) {
            threeDSecureClient.onBrowserSwitchResult(browserSwitchResult, this::handleThreeDSecureResult);
        }
    }

    private void handleThreeDSecureActivityResult(ActivityResult activityResult) {
        threeDSecureClient.onActivityResult(activityResult.getResultCode(), activityResult.getData(), this::handleThreeDSecureResult);
    }

    private void handlePaymentMethodNonceCreated(PaymentMethodNonce paymentMethodNonce) {
        super.onPaymentMethodNonceCreated(paymentMethodNonce);

        final FragmentActivity activity = getActivity();
        if (!mThreeDSecureRequested && paymentMethodNonce instanceof CardNonce && Settings.isThreeDSecureEnabled(activity)) {
            mThreeDSecureRequested = true;
            mLoading = ProgressDialog.show(activity, getString(R.string.loading), getString(R.string.loading), true, false);

            ThreeDSecureRequest threeDSecureRequest = threeDSecureRequest(paymentMethodNonce);
            threeDSecureClient.performVerification(activity, threeDSecureRequest, (threeDSecureResult, error) -> {
                if (threeDSecureResult != null) {
                    threeDSecureClient.continuePerformVerification(activity, threeDSecureRequest, threeDSecureResult, this::handleThreeDSecureResult);
                } else {
                    handleError(error);
                    safelyCloseLoadingView();
                }
            });
        } else if (paymentMethodNonce instanceof CardNonce && Settings.isAmexRewardsBalanceEnabled(activity)) {
            mLoading = ProgressDialog.show(activity, getString(R.string.loading), getString(R.string.loading), true, false);
            String nonce = paymentMethodNonce.getNonce();

            americanExpressClient.getRewardsBalance(nonce, "USD", (rewardsBalance, error) -> {
                if (rewardsBalance != null) {
                    safelyCloseLoadingView();
                    showDialog(getAmexRewardsBalanceString(rewardsBalance));
                } else if (error != null) {
                    handleError(error);
                }
            });
        } else {

            CardFragmentDirections.ActionCardFragmentToDisplayNonceFragment action =
                    CardFragmentDirections.actionCardFragmentToDisplayNonceFragment(paymentMethodNonce);
            action.setDeviceData(mDeviceData);

            NavHostFragment.findNavController(this).navigate(action);
        }
    }

    private void safelyCloseLoadingView() {
        if (mLoading != null && mLoading.isShowing()) {
            mLoading.dismiss();
        }
    }

    public static String getAmexRewardsBalanceString(AmericanExpressRewardsBalance rewardsBalance) {
        return "Amex Rewards Balance: \n" +
                "- amount: " + rewardsBalance.getRewardsAmount() + "\n" +
                "- errorCode: " + rewardsBalance.getErrorCode();
    }

    private ThreeDSecureRequest threeDSecureRequest(PaymentMethodNonce paymentMethodNonce) {
        CardNonce cardNonce = (CardNonce) paymentMethodNonce;

        ThreeDSecurePostalAddress billingAddress = new ThreeDSecurePostalAddress();
        billingAddress.givenName("Jill");
        billingAddress.surname("Doe");
        billingAddress.phoneNumber("5551234567");
        billingAddress.streetAddress("555 Smith St");
        billingAddress.extendedAddress("#2");
        billingAddress.locality("Chicago");
        billingAddress.region("IL");
        billingAddress.postalCode("12345");
        billingAddress.countryCodeAlpha2("US");

        ThreeDSecureAdditionalInformation additionalInformation = new ThreeDSecureAdditionalInformation();
        additionalInformation.setAccountId("account-id");

        ThreeDSecureV2ToolbarCustomization toolbarCustomization = new ThreeDSecureV2ToolbarCustomization();
        toolbarCustomization.setHeaderText("Braintree 3DS Checkout");
        toolbarCustomization.setBackgroundColor("#FF5A5F");
        toolbarCustomization.setButtonText("Close");
        toolbarCustomization.setTextColor("#222222");
        toolbarCustomization.setTextFontSize(18);

        ThreeDSecureV2UiCustomization v2UiCustomization = new ThreeDSecureV2UiCustomization();
        v2UiCustomization.setToolbarCustomization(toolbarCustomization);

        ThreeDSecureV1UiCustomization v1UiCustomization = new ThreeDSecureV1UiCustomization();
        v1UiCustomization.setRedirectButtonText("Return to Demo App");
        v1UiCustomization.setRedirectDescription("Please use the button above if you are not automatically redirected to the app. (This text can contain accéntéd chàractèrs.)");

        ThreeDSecureRequest threeDSecureRequest = new ThreeDSecureRequest();
        threeDSecureRequest.amount("10");
        threeDSecureRequest.email("test@email.com");
        threeDSecureRequest.billingAddress(billingAddress);
        threeDSecureRequest.nonce(cardNonce.getNonce());
        threeDSecureRequest.versionRequested(ThreeDSecureRequest.VERSION_2);
        threeDSecureRequest.additionalInformation(additionalInformation);
        threeDSecureRequest.v2UiCustomization(v2UiCustomization);
        threeDSecureRequest.v1UiCustomization(v1UiCustomization);

        return threeDSecureRequest;
    }
}
