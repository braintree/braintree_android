package com.braintreepayments.demo;

import static com.braintreepayments.demo.PayPalRequestFactory.createPayPalCheckoutRequest;
import static com.braintreepayments.demo.PayPalRequestFactory.createPayPalVaultRequest;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Switch;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.navigation.fragment.NavHostFragment;

import com.braintreepayments.api.BrowserSwitchException;
import com.braintreepayments.api.core.PaymentMethodNonce;
import com.braintreepayments.api.datacollector.DataCollector;
import com.braintreepayments.api.datacollector.DataCollectorRequest;
import com.braintreepayments.api.datacollector.DataCollectorResult;
import com.braintreepayments.api.paypal.PayPalClient;
import com.braintreepayments.api.paypal.PayPalLauncher;
import com.braintreepayments.api.paypal.PayPalPaymentAuthRequest;
import com.braintreepayments.api.paypal.PayPalPaymentAuthResult;
import com.braintreepayments.api.paypal.PayPalPendingRequest;
import com.braintreepayments.api.paypal.PayPalRequest;
import com.braintreepayments.api.paypal.PayPalResult;
import com.google.android.material.textfield.TextInputEditText;

public class PayPalFragment extends BaseFragment {

    private String deviceData;
    private String amount;

    private PayPalClient payPalClient;
    private PayPalLauncher payPalLauncher;
    private Boolean isPayLaterSelected = false;

    private DataCollector dataCollector;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PayPalPendingRequest.Started payPalPendingRequest = PendingRequestStore.getInstance().getPayPalPendingRequest(getContext());
        if (payPalPendingRequest != null) {
            String pendingRequest = payPalPendingRequest.getPendingRequestString();
            payPalLauncher = new PayPalLauncher(this);
            try {
                payPalLauncher.restorePendingRequest(pendingRequest);
            } catch (BrowserSwitchException e) {
                PendingRequestStore.getInstance().clearPayPalPendingRequest(getContext());
            }
        } else {
            payPalLauncher = new PayPalLauncher(this);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_paypal, container, false);
        TextInputEditText buyerEmailEditText = view.findViewById(R.id.buyer_email_edit_text);
        TextInputEditText buyerPhoneCountryCodeEditText = view.findViewById(R.id.buyer_phone_country_code_edit_text);
        TextInputEditText buyerPhoneNationalNumberEditText = view.findViewById(R.id.buyer_phone_national_number_edit_text);
        Button billingAgreementButton = view.findViewById(R.id.paypal_billing_agreement_button);
        Button singlePaymentButton = view.findViewById(R.id.paypal_single_payment_button);
        Button singlePaymentPayLaterButton = view.findViewById(R.id.paypal_single_payment_pay_later_button);
        Button paymentWithCreditButton = view.findViewById(R.id.paypal_pay_with_credit_button);
        Switch contactInformationSwitch = view.findViewById(R.id.contact_info_switch);
        Switch amountBreakdownSwitch = view.findViewById(R.id.amount_breakdown_switch);

        singlePaymentButton.setOnClickListener(v -> {
            launchPayPal(
                false,
                buyerEmailEditText.getText().toString(),
                buyerPhoneCountryCodeEditText.getText().toString(),
                buyerPhoneNationalNumberEditText.getText().toString(),
                contactInformationSwitch.isChecked(),
                false,
                false,
                amountBreakdownSwitch.isChecked()
            );
        });

        paymentWithCreditButton.setOnClickListener(v -> {
            launchPayPal(
                false,
                buyerEmailEditText.getText().toString(),
                buyerPhoneCountryCodeEditText.getText().toString(),
                buyerPhoneNationalNumberEditText.getText().toString(),
                contactInformationSwitch.isChecked(),
                false,
                true,
                amountBreakdownSwitch.isChecked()
            );
            isPayLaterSelected = false;
        });

        billingAgreementButton.setOnClickListener(v -> {
            launchPayPal(
                    true,
                    buyerEmailEditText.getText().toString(),
                    buyerPhoneCountryCodeEditText.getText().toString(),
                    buyerPhoneNationalNumberEditText.getText().toString(),
                    contactInformationSwitch.isChecked(),
                    false,
                    false,
                    amountBreakdownSwitch.isChecked()
            );
            isPayLaterSelected = false;
        });

        singlePaymentPayLaterButton.setOnClickListener(v -> {
            launchPayPal(
                    false,
                    buyerEmailEditText.getText().toString(),
                    buyerPhoneCountryCodeEditText.getText().toString(),
                    buyerPhoneNationalNumberEditText.getText().toString(),
                    contactInformationSwitch.isChecked(),
                    true,
                    false,
                    amountBreakdownSwitch.isChecked()
            );
            isPayLaterSelected = true;
        });

        payPalClient = new PayPalClient(
            requireContext(),
            super.getAuthStringArg(),
            Uri.parse("https://mobile-sdk-demo-site-838cead5d3ab.herokuapp.com/braintree-payments"),
            "com.braintreepayments.demo.braintree"
        );

        amount = RandomDollarAmount.getNext();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        PayPalPendingRequest.Started pendingRequest = getPendingRequest();
        if (pendingRequest != null) {
            PayPalPaymentAuthResult paymentAuthResult = payPalLauncher.handleReturnToApp(pendingRequest, requireActivity().getIntent());
            if (paymentAuthResult instanceof PayPalPaymentAuthResult.Success) {
                completePayPalFlow((PayPalPaymentAuthResult.Success) paymentAuthResult);
            } else {
                handleError(new Exception("User did not complete payment flow"));
            }
            clearPendingRequest();
            requireActivity().getIntent().setData(null);
        }
    }

    private void storePendingRequest(PayPalPendingRequest.Started request) {
        PendingRequestStore.getInstance().putPayPalPendingRequest(requireContext(), request);
    }
    private PayPalPendingRequest.Started getPendingRequest() {
        return PendingRequestStore.getInstance().getPayPalPendingRequest(requireContext());
    }

    private void clearPendingRequest() {
        PendingRequestStore.getInstance().clearPayPalPendingRequest(requireContext());
    }

    private void launchPayPal(
        boolean isBillingAgreement,
        String buyerEmailAddress,
        String buyerPhoneCountryCode,
        String buyerPhoneNationalNumber,
        Boolean isContactInformationEnabled,
        Boolean offerPayLater,
        Boolean offerCredit,
        Boolean isAmountBreakdownEnabled
    ) {
        FragmentActivity activity = getActivity();
        activity.setProgressBarIndeterminateVisibility(true);

        dataCollector = new DataCollector(requireContext(), super.getAuthStringArg());

        if (Settings.shouldCollectDeviceData(requireActivity())) {
            dataCollector.collectDeviceData(requireActivity(), new DataCollectorRequest(true), (dataCollectorResult) -> {
                if (dataCollectorResult instanceof DataCollectorResult.Success) {
                    deviceData = ((DataCollectorResult.Success) dataCollectorResult).getDeviceData();
                }
                launchPayPal(activity, isBillingAgreement, amount, buyerEmailAddress, buyerPhoneCountryCode, buyerPhoneNationalNumber, isContactInformationEnabled, offerPayLater, offerCredit, isAmountBreakdownEnabled);
            });
        } else {
            launchPayPal(activity, isBillingAgreement, amount, buyerEmailAddress, buyerPhoneCountryCode, buyerPhoneNationalNumber, isContactInformationEnabled, offerPayLater, offerCredit, isAmountBreakdownEnabled);
        }
    }

    private void launchPayPal(
        FragmentActivity activity,
        boolean isBillingAgreement,
        String amount,
        String buyerEmailAddress,
        String buyerPhoneCountryCode,
        String buyerPhoneNationalNumber,
        boolean isContactInformationEnabled,
        boolean offerPayLater,
        boolean offerCredit,
        boolean isAmountBreakdownEnabled
    ) {
        PayPalRequest payPalRequest;
        if (isBillingAgreement) {
            payPalRequest = createPayPalVaultRequest(
                activity,
                buyerEmailAddress,
                buyerPhoneCountryCode,
                buyerPhoneNationalNumber,
                null
            );
        } else {
            payPalRequest = createPayPalCheckoutRequest(
                activity,
                amount,
                buyerEmailAddress,
                buyerPhoneCountryCode,
                buyerPhoneNationalNumber,
                isContactInformationEnabled,
                    null,
                    offerPayLater,
                    offerCredit,
                    isAmountBreakdownEnabled
            );
        }
        payPalClient.createPaymentAuthRequest(requireContext(), payPalRequest,
            (paymentAuthRequest) -> {
                if (paymentAuthRequest instanceof PayPalPaymentAuthRequest.Failure) {
                    handleError(((PayPalPaymentAuthRequest.Failure) paymentAuthRequest).getError());
                } else if (paymentAuthRequest instanceof PayPalPaymentAuthRequest.ReadyToLaunch){
                    PayPalPendingRequest request = payPalLauncher.launch(requireActivity(),
                            ((PayPalPaymentAuthRequest.ReadyToLaunch) paymentAuthRequest));
                    if (request instanceof PayPalPendingRequest.Started) {
                        storePendingRequest((PayPalPendingRequest.Started) request);
                    } else if (request instanceof PayPalPendingRequest.Failure) {
                        handleError(((PayPalPendingRequest.Failure) request).getError());
                    }
                }
            });
    }

    private void completePayPalFlow(PayPalPaymentAuthResult.Success paymentAuthResult) {
        payPalClient.tokenize(paymentAuthResult, payPalResult -> {
            if (payPalResult instanceof PayPalResult.Failure) {
                handleError(((PayPalResult.Failure) payPalResult).getError());
            } else if (payPalResult instanceof PayPalResult.Success) {
                handlePayPalResult(((PayPalResult.Success) payPalResult).getNonce());
            }
        });
    }

    private void handlePayPalResult(PaymentMethodNonce paymentMethodNonce) {
        if (paymentMethodNonce != null) {
            super.onPaymentMethodNonceCreated(paymentMethodNonce);

            PayPalFragmentDirections.ActionPayPalFragmentToDisplayNonceFragment action =
                PayPalFragmentDirections.actionPayPalFragmentToDisplayNonceFragment(paymentMethodNonce);
            action.setTransactionAmount(amount);
            action.setDeviceData(deviceData);
            action.setIsPayLaterSelected(isPayLaterSelected);

            NavHostFragment.findNavController(this).navigate(action);
        }
    }
}