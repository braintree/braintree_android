package com.braintreepayments.demo;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentActivity;
import androidx.navigation.fragment.NavHostFragment;

import com.braintreepayments.api.AmericanExpressClient;
import com.braintreepayments.api.AmericanExpressResult;
import com.braintreepayments.api.AmericanExpressRewardsBalance;
import com.braintreepayments.api.Card;
import com.braintreepayments.api.CardClient;
import com.braintreepayments.api.CardNonce;
import com.braintreepayments.api.CardResult;
import com.braintreepayments.api.DataCollector;
import com.braintreepayments.api.DataCollectorCallback;
import com.braintreepayments.api.DataCollectorResult;
import com.braintreepayments.api.PaymentMethodNonce;
import com.braintreepayments.api.ThreeDSecureAdditionalInformation;
import com.braintreepayments.api.ThreeDSecureClient;
import com.braintreepayments.api.ThreeDSecureLauncher;
import com.braintreepayments.api.ThreeDSecureNonce;
import com.braintreepayments.api.ThreeDSecurePaymentAuthRequest;
import com.braintreepayments.api.ThreeDSecurePostalAddress;
import com.braintreepayments.api.ThreeDSecureRequest;
import com.braintreepayments.api.ThreeDSecureResult;
import com.braintreepayments.api.ThreeDSecureV2ButtonCustomization;
import com.braintreepayments.api.ThreeDSecureV2LabelCustomization;
import com.braintreepayments.api.ThreeDSecureV2TextBoxCustomization;
import com.braintreepayments.api.ThreeDSecureV2ToolbarCustomization;
import com.braintreepayments.api.ThreeDSecureV2UiCustomization;
import com.braintreepayments.api.UserCanceledException;
import com.braintreepayments.cardform.OnCardFormFieldFocusedListener;
import com.braintreepayments.cardform.OnCardFormSubmitListener;
import com.braintreepayments.cardform.utils.CardType;
import com.braintreepayments.cardform.view.CardEditText;
import com.braintreepayments.cardform.view.CardForm;

import java.util.Arrays;

public class CardFragment extends BaseFragment implements OnCardFormSubmitListener,
        OnCardFormFieldFocusedListener {

    private static final String EXTRA_THREE_D_SECURE_REQUESTED =
            "com.braintreepayments.demo.EXTRA_THREE_D_SECURE_REQUESTED";

    private String deviceData;
    private boolean threeDSecureRequested;

    private ProgressDialog loading;
    private CardForm cardForm;
    private Button purchaseButton;
    private Button autofillButton;

    private CardType cardType;

    private AmericanExpressClient americanExpressClient;
    private CardClient cardClient;
    private ThreeDSecureClient threeDSecureClient;
    private ThreeDSecureLauncher threeDSecureLauncher;
    private DataCollector dataCollector;

    private String cardFormActionLabel;

    @Override
    public void onCreate(Bundle onSaveInstanceState) {
        super.onCreate(onSaveInstanceState);

        americanExpressClient = new AmericanExpressClient(requireContext(), super.getAuthStringArg());
        cardClient = new CardClient(requireContext(), super.getAuthStringArg());
        threeDSecureClient = new ThreeDSecureClient(requireContext(), super.getAuthStringArg());

        dataCollector = new DataCollector(requireContext(), super.getAuthStringArg());

        if (onSaveInstanceState != null) {
            threeDSecureRequested = onSaveInstanceState.getBoolean(EXTRA_THREE_D_SECURE_REQUESTED);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_card, container, false);

        threeDSecureLauncher = new ThreeDSecureLauncher(this,
                paymentAuthResult -> {
                    threeDSecureClient.tokenize(paymentAuthResult, threeDSecureResult -> {
                        if (threeDSecureResult instanceof ThreeDSecureResult.Success) {
                            handlePaymentMethodNonceCreated(((ThreeDSecureResult.Success) threeDSecureResult).getNonce());
                        } else if (threeDSecureResult instanceof ThreeDSecureResult.Failure) {
                            handleError(((ThreeDSecureResult.Failure) threeDSecureResult).getError());
                        } else if (threeDSecureResult instanceof ThreeDSecureResult.Cancel) {
                            handleError(new UserCanceledException("User canceled 3DS."));
                        }
                    });
                });

        cardForm = view.findViewById(R.id.card_form);
        cardForm.setOnFormFieldFocusedListener(this);
        cardForm.setOnCardFormSubmitListener(this);

        cardFormActionLabel = getString(R.string.purchase);

        purchaseButton = view.findViewById(R.id.purchase_button);
        autofillButton = view.findViewById(R.id.autofill_button);

        purchaseButton.setOnClickListener(this::onPurchase);
        autofillButton.setOnClickListener(this::onAutofill);

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
    }

    private void configureCardForm() {
        final AppCompatActivity activity = (AppCompatActivity) getActivity();

        // TODO: Configure card form via settings
        cardForm.cardRequired(true)
                .expirationRequired(true)
                .cvvRequired(true)
                .postalCodeRequired(true)
                .mobileNumberRequired(false)
                .actionLabel(cardFormActionLabel)
                .setup(activity);

        if (getArguments().getBoolean(MainFragment.EXTRA_COLLECT_DEVICE_DATA, false)) {
            dataCollector.collectDeviceData(activity, dataCollectorResult -> {
                if (dataCollectorResult instanceof DataCollectorResult.Success) {
                    deviceData = ((DataCollectorResult.Success) dataCollectorResult).getDeviceData();
                }
            });
        }
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
            }
        }
    }

    @Override
    public void onCardFormSubmit() {
        onPurchase(null);
    }

    public void onPurchase(View v) {
        getActivity().setProgressBarIndeterminateVisibility(true);

        Card card = new Card();
        card.setNumber(cardForm.getCardNumber());
        card.setExpirationMonth(cardForm.getExpirationMonth());
        card.setExpirationYear(cardForm.getExpirationYear());
        card.setCvv(cardForm.getCvv());
        // TODO: GQL currently only returns the bin if validate = false
        card.setShouldValidate(false); 
        card.setPostalCode(cardForm.getPostalCode());

        cardClient.tokenize(card, (cardResult) -> {
            if (cardResult instanceof CardResult.Success) {
                handlePaymentMethodNonceCreated(((CardResult.Success) cardResult).getNonce());
            } else if (cardResult instanceof CardResult.Failure) {
                handleError(((CardResult.Failure) cardResult).getError());
            }
        });
    }

    public void onAutofill(View v) {
        AutofillHelper autofillHelper = new AutofillHelper(cardForm);
        autofillHelper.fillCardNumber("4111111111111111");
        autofillHelper.fillExpirationDate("01/27");
        autofillHelper.fillCVV("123");
        autofillHelper.fillPostalCode("12345");
    }

    private void handlePaymentMethodNonceCreated(PaymentMethodNonce paymentMethodNonce) {
        super.onPaymentMethodNonceCreated(paymentMethodNonce);

        final FragmentActivity activity = getActivity();
        if (!threeDSecureRequested && paymentMethodNonce instanceof CardNonce &&
                Settings.isThreeDSecureEnabled(activity) && !(paymentMethodNonce instanceof ThreeDSecureNonce)) {
            threeDSecureRequested = true;
            loading = ProgressDialog.show(activity, getString(R.string.loading),
                    getString(R.string.loading), true, false);

            ThreeDSecureRequest threeDSecureRequest = threeDSecureRequest(paymentMethodNonce);
            threeDSecureClient.createPaymentAuthRequest(requireContext(), threeDSecureRequest,
                    (paymentAuthRequest) -> {
                        if (paymentAuthRequest instanceof ThreeDSecurePaymentAuthRequest.ReadyToLaunch) {
                            threeDSecureLauncher.launch(
                                    (ThreeDSecurePaymentAuthRequest.ReadyToLaunch) paymentAuthRequest);
                        } else if (paymentAuthRequest instanceof ThreeDSecurePaymentAuthRequest.LaunchNotRequired) {
                            handlePaymentMethodNonceCreated(((ThreeDSecurePaymentAuthRequest.LaunchNotRequired) paymentAuthRequest).getNonce());
                        } else if (paymentAuthRequest instanceof ThreeDSecurePaymentAuthRequest.Failure) {
                            handleError(((ThreeDSecurePaymentAuthRequest.Failure) paymentAuthRequest).getError());
                        }
                        safelyCloseLoadingView();
                    });
        } else if (paymentMethodNonce instanceof CardNonce &&
                Settings.isAmexRewardsBalanceEnabled(activity)) {
            loading = ProgressDialog.show(activity, getString(R.string.loading),
                    getString(R.string.loading), true, false);
            String nonce = paymentMethodNonce.getString();

            americanExpressClient.getRewardsBalance(nonce, "USD", (americanExpressResult) -> {
                if (americanExpressResult instanceof AmericanExpressResult.Success) {
                    safelyCloseLoadingView();
                    showDialog(getAmexRewardsBalanceString(
                            ((AmericanExpressResult.Success) americanExpressResult).getRewardsBalance()));
                } else if (americanExpressResult instanceof AmericanExpressResult.Failure) {
                    handleError(((AmericanExpressResult.Failure) americanExpressResult).getError());
                }
            });
        } else {

            CardFragmentDirections.ActionCardFragmentToDisplayNonceFragment action =
                    CardFragmentDirections.actionCardFragmentToDisplayNonceFragment(
                            paymentMethodNonce);
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

        ThreeDSecureAdditionalInformation additionalInformation =
                new ThreeDSecureAdditionalInformation();
        additionalInformation.setAccountId("account-id");

        ThreeDSecureV2ButtonCustomization submitButtonCustomization =
                new ThreeDSecureV2ButtonCustomization();
        submitButtonCustomization.setBackgroundColor("#D3D3D3");
        submitButtonCustomization.setTextColor("#000000");

        ThreeDSecureV2ToolbarCustomization toolbarCustomization =
                new ThreeDSecureV2ToolbarCustomization();
        toolbarCustomization.setHeaderText("Braintree 3DS Checkout");
        toolbarCustomization.setBackgroundColor("#FF5A5F");
        toolbarCustomization.setButtonText("Close");
        toolbarCustomization.setTextColor("#222222");
        toolbarCustomization.setTextFontSize(18);

        ThreeDSecureV2LabelCustomization labelCustomization =
                new ThreeDSecureV2LabelCustomization();
        labelCustomization.setHeadingTextColor("#0082CB");
        labelCustomization.setTextFontSize(14);

        ThreeDSecureV2TextBoxCustomization textBoxCustomization =
                new ThreeDSecureV2TextBoxCustomization();
        textBoxCustomization.setBorderColor("#0082CB");

        ThreeDSecureV2UiCustomization v2UiCustomization = new ThreeDSecureV2UiCustomization();
        v2UiCustomization.setLabelCustomization(labelCustomization);
        v2UiCustomization.setTextBoxCustomization(textBoxCustomization);
        v2UiCustomization.setToolbarCustomization(toolbarCustomization);
        v2UiCustomization.setButtonCustomization(submitButtonCustomization,
                ThreeDSecureV2UiCustomization.BUTTON_TYPE_VERIFY);

        ThreeDSecureRequest threeDSecureRequest = new ThreeDSecureRequest();
        threeDSecureRequest.setAmount("10");
        threeDSecureRequest.setEmail("test@email.com");
        threeDSecureRequest.setBillingAddress(billingAddress);
        threeDSecureRequest.setNonce(cardNonce.getString());
        threeDSecureRequest.setRequestedExemptionType(ThreeDSecureRequest.LOW_VALUE);
        threeDSecureRequest.setAdditionalInformation(additionalInformation);
        threeDSecureRequest.setV2UiCustomization(v2UiCustomization);

        threeDSecureRequest.setUiType(ThreeDSecureRequest.BOTH);

        threeDSecureRequest.setRenderTypes(Arrays.asList(
                ThreeDSecureRequest.OTP,
                ThreeDSecureRequest.SINGLE_SELECT,
                ThreeDSecureRequest.MULTI_SELECT,
                ThreeDSecureRequest.OOB,
                ThreeDSecureRequest.RENDER_HTML
        ));

        return threeDSecureRequest;
    }
}
