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

import com.braintreepayments.api.AmericanExpressClient;
import com.braintreepayments.api.AmericanExpressRewardsBalance;
import com.braintreepayments.api.BraintreeClient;
import com.braintreepayments.api.PaymentMethodNonce;
import com.braintreepayments.api.BrowserSwitchResult;
import com.braintreepayments.api.Card;
import com.braintreepayments.api.CardClient;
import com.braintreepayments.api.CardNonce;
import com.braintreepayments.api.DataCollector;
import com.braintreepayments.api.ThreeDSecureAdditionalInformation;
import com.braintreepayments.api.ThreeDSecureClient;
import com.braintreepayments.api.ThreeDSecurePostalAddress;
import com.braintreepayments.api.ThreeDSecureRequest;
import com.braintreepayments.api.ThreeDSecureResult;
import com.braintreepayments.api.ThreeDSecureV1UiCustomization;
import com.braintreepayments.api.ThreeDSecureV2ToolbarCustomization;
import com.braintreepayments.api.ThreeDSecureV2UiCustomization;
import com.braintreepayments.api.UnionPayCapabilities;
import com.braintreepayments.api.UnionPayCard;
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

    private String deviceData;
    private boolean isUnionPay;
    private String enrollmentId;
    private boolean threeDSecureRequested;

    private ProgressDialog loading;
    private CardForm cardForm;
    private TextInputLayout smsCodeContainer;
    private EditText smsCode;
    private Button sendSmsButton;
    private Button purchaseButton;
    private Button autofillButton;

    private CardType cardType;

    private AmericanExpressClient americanExpressClient;
    private CardClient cardClient;
    private ThreeDSecureClient threeDSecureClient;
    private UnionPayClient unionPayClient;
    private DataCollector dataCollector;

    private String cardFormActionLabel;

    @Override
    public void onCreate(Bundle onSaveInstanceState) {
        super.onCreate(onSaveInstanceState);

        BraintreeClient braintreeClient = getBraintreeClient();
        americanExpressClient = new AmericanExpressClient(braintreeClient);
        cardClient = new CardClient(braintreeClient);
        threeDSecureClient = new ThreeDSecureClient(braintreeClient);
        unionPayClient = new UnionPayClient(braintreeClient);
        dataCollector = new DataCollector(braintreeClient);

        if (onSaveInstanceState != null) {
            threeDSecureRequested = onSaveInstanceState.getBoolean(EXTRA_THREE_D_SECURE_REQUESTED);
            isUnionPay = onSaveInstanceState.getBoolean(EXTRA_UNIONPAY);
            enrollmentId = onSaveInstanceState.getString(EXTRA_UNIONPAY_ENROLLMENT_ID);

            if (isUnionPay) {
                sendSmsButton.setVisibility(VISIBLE);
            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_card, container, false);

        cardForm = view.findViewById(R.id.card_form);
        cardForm.setOnFormFieldFocusedListener(this);
        cardForm.setOnCardFormSubmitListener(this);

        cardFormActionLabel = getString(R.string.purchase);

        smsCodeContainer = view.findViewById(R.id.sms_code_container);
        smsCode = view.findViewById(R.id.sms_code);
        sendSmsButton = view.findViewById(R.id.unionpay_enroll_button);
        purchaseButton = view.findViewById(R.id.purchase_button);
        autofillButton = view.findViewById(R.id.autofill_button);

        sendSmsButton.setOnClickListener(this::sendSms);
        purchaseButton.setOnClickListener(this::onPurchase);
        autofillButton.setOnClickListener(this::onAutofill);

        if (isUnionPay) {
            sendSmsButton.setVisibility(VISIBLE);
        }

        DemoViewModel viewModel = new ViewModelProvider(getActivity()).get(DemoViewModel.class);
        viewModel.getThreeDSecureBrowserSwitchResult().observe(getViewLifecycleOwner(), this::handleThreeDSecureBrowserSwitchResult);
        viewModel.getThreeDSecureActivityResult().observe(getViewLifecycleOwner(), this::handleThreeDSecureActivityResult);

        purchaseButton.setEnabled(true);
        autofillButton.setEnabled(true);

        configureCardForm();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        safelyCloseLoadingView();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(EXTRA_THREE_D_SECURE_REQUESTED, threeDSecureRequested);
        outState.putBoolean(EXTRA_UNIONPAY, isUnionPay);
        outState.putString(EXTRA_UNIONPAY_ENROLLMENT_ID, enrollmentId);
    }

    private void configureCardForm() {

        final AppCompatActivity activity = (AppCompatActivity) getActivity();
        BraintreeClient braintreeClient = getBraintreeClient();

        // check if union pay is enabled
        braintreeClient.getConfiguration((configuration, configError) -> {
            if (configuration != null) {
                cardForm.cardRequired(true)
                        .expirationRequired(true)
                        .cvvRequired(configuration.isCvvChallengePresent())
                        .postalCodeRequired(configuration.isPostalCodeChallengePresent())
                        .mobileNumberRequired(false)
                        .actionLabel(cardFormActionLabel)
                        .setup(activity);

                if (getArguments().getBoolean(MainFragment.EXTRA_COLLECT_DEVICE_DATA, false)) {
                    dataCollector.collectDeviceData(activity, (deviceData, e) -> this.deviceData = deviceData);
                }
            } else {
                handleError(configError);
            }
        });
    }

    @Override
    protected void handleError(Exception error) {
        super.handleError(error);
        threeDSecureRequested = false;
    }

    @Override
    public void onCancel(int requestCode) {
        super.onCancel(requestCode);

        threeDSecureRequested = false;

        Toast.makeText(getActivity(), "3DS canceled", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onCardFormFieldFocused(View field) {
        if (!(field instanceof CardEditText) && !TextUtils.isEmpty(cardForm.getCardNumber())) {
            CardType cardType = CardType.forCardNumber(cardForm.getCardNumber());
            if (this.cardType != cardType) {
                this.cardType = cardType;

                if (!Settings.useTokenizationKey(getActivity().getApplicationContext())) {
                    String cardNumber = cardForm.getCardNumber();
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
        smsCodeContainer.setVisibility(GONE);
        smsCode.setText("");

        BraintreeClient braintreeClient = getBraintreeClient();
        final AppCompatActivity activity = (AppCompatActivity) getActivity();

        braintreeClient.getConfiguration((configuration, error) -> {
            if (capabilities.isUnionPay()) {
                if (!capabilities.isSupported()) {
                    cardForm.setCardNumberError("Card not accepted");
                    return;
                }
                isUnionPay = true;
                enrollmentId = null;

                cardForm.cardRequired(true)
                        .expirationRequired(true)
                        .cvvRequired(true)
                        .postalCodeRequired(configuration.isPostalCodeChallengePresent())
                        .mobileNumberRequired(true)
                        .actionLabel(cardFormActionLabel)
                        .setup(activity);

                sendSmsButton.setVisibility(VISIBLE);
            } else {
                isUnionPay = false;

                cardForm.cardRequired(true)
                        .expirationRequired(true)
                        .cvvRequired(configuration.isCvvChallengePresent())
                        .postalCodeRequired(configuration.isPostalCodeChallengePresent())
                        .mobileNumberRequired(false)
                        .actionLabel(cardFormActionLabel)
                        .setup(activity);

                if (!configuration.isCvvChallengePresent()) {
                    ((EditText) activity.findViewById(R.id.bt_card_form_cvv)).setText("");
                }
            }
        });
    }

    public void sendSms(View v) {
        UnionPayCard unionPayCard = new UnionPayCard();
        unionPayCard.setNumber(cardForm.getCardNumber());
        unionPayCard.setExpirationMonth(cardForm.getExpirationMonth());
        unionPayCard.setExpirationYear(cardForm.getExpirationYear());
        unionPayCard.setCvv(cardForm.getCvv());
        unionPayCard.setPostalCode(cardForm.getPostalCode());
        unionPayCard.setMobileCountryCode(cardForm.getCountryCode());
        unionPayCard.setMobilePhoneNumber(cardForm.getMobileNumber());

        unionPayClient.enroll(unionPayCard, (enrollment, error) -> {
            enrollmentId = enrollment.getId();
            if (enrollment.isSmsCodeRequired()) {
                smsCodeContainer.setVisibility(VISIBLE);
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
        if (isUnionPay) {
            UnionPayCard unionPayCard = new UnionPayCard();
            unionPayCard.setNumber(cardForm.getCardNumber());
            unionPayCard.setExpirationMonth(cardForm.getExpirationMonth());
            unionPayCard.setExpirationYear(cardForm.getExpirationYear());
            unionPayCard.setCvv(cardForm.getCvv());
            unionPayCard.setPostalCode(cardForm.getPostalCode());
            unionPayCard.setMobileCountryCode(cardForm.getCountryCode());
            unionPayCard.setMobilePhoneNumber(cardForm.getMobileNumber());
            unionPayCard.setSmsCode(smsCode.getText().toString());
            unionPayCard.setEnrollmentId(enrollmentId);

            unionPayClient.tokenize(unionPayCard, (cardNonce, tokenizeError) -> {
                if (cardNonce != null) {
                    handlePaymentMethodNonceCreated(cardNonce);
                } else {
                    handleError(tokenizeError);
                }
            });

        } else {
            Card card = new Card();
            card.setNumber(cardForm.getCardNumber());
            card.setExpirationMonth(cardForm.getExpirationMonth());
            card.setExpirationYear(cardForm.getExpirationYear());
            card.setCvv(cardForm.getCvv());
            card.setShouldValidate(false); // TODO GQL currently only returns the bin if validate = false
            card.setPostalCode(cardForm.getPostalCode());

            cardClient.tokenize(card, (cardNonce, tokenizeError) -> {
                if (cardNonce != null) {
                    handlePaymentMethodNonceCreated(cardNonce);
                } else {
                    handleError(tokenizeError);
                }
            });
        }
    }

    public void onAutofill(View v) {
        AutofillHelper autofillHelper = new AutofillHelper(cardForm);
        autofillHelper.fillCardNumber("4111111111111111");
        autofillHelper.fillExpirationDate("01/27");
        autofillHelper.fillCVV("123");
        autofillHelper.fillPostalCode("12345");
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
        if (!threeDSecureRequested && paymentMethodNonce instanceof CardNonce && Settings.isThreeDSecureEnabled(activity)) {
            threeDSecureRequested = true;
            loading = ProgressDialog.show(activity, getString(R.string.loading), getString(R.string.loading), true, false);

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
            loading = ProgressDialog.show(activity, getString(R.string.loading), getString(R.string.loading), true, false);
            String nonce = paymentMethodNonce.getString();

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
            action.setDeviceData(deviceData);

            NavHostFragment.findNavController(this).navigate(action);
        }
    }

    private void safelyCloseLoadingView() {
        if (loading != null && loading.isShowing()) {
            loading.dismiss();
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
        billingAddress.setGivenName("Jill");
        billingAddress.setSurname("Doe");
        billingAddress.setPhoneNumber("5551234567");
        billingAddress.setStreetAddress("555 Smith St");
        billingAddress.setExtendedAddress("#2");
        billingAddress.setLocality("Chicago");
        billingAddress.setRegion("IL");
        billingAddress.setPostalCode("12345");
        billingAddress.setCountryCodeAlpha2("US");

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
        threeDSecureRequest.setAmount("10");
        threeDSecureRequest.setEmail("test@email.com");
        threeDSecureRequest.setBillingAddress(billingAddress);
        threeDSecureRequest.setNonce(cardNonce.getString());
        threeDSecureRequest.setVersionRequested(ThreeDSecureRequest.VERSION_2);
        threeDSecureRequest.setAdditionalInformation(additionalInformation);
        threeDSecureRequest.setV2UiCustomization(v2UiCustomization);
        threeDSecureRequest.setV1UiCustomization(v1UiCustomization);

        return threeDSecureRequest;
    }
}
