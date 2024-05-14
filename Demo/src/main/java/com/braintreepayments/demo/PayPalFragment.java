package com.braintreepayments.demo;

import static com.braintreepayments.demo.PayPalRequestFactory.createPayPalCheckoutRequest;
import static com.braintreepayments.demo.PayPalRequestFactory.createPayPalVaultRequest;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.navigation.fragment.NavHostFragment;

import com.braintreepayments.api.BraintreeClient;
import com.braintreepayments.api.BrowserSwitchResult;
import com.braintreepayments.api.DataCollector;
import com.braintreepayments.api.PayPalAccountNonce;
import com.braintreepayments.api.PayPalClient;
import com.braintreepayments.api.PayPalListener;
import com.braintreepayments.api.PaymentMethodNonce;
import com.google.android.material.textfield.TextInputEditText;

public class PayPalFragment extends BaseFragment implements PayPalListener {

    private String deviceData;
    private String amount;

    private BraintreeClient braintreeClient;
    private PayPalClient payPalClient;

    private DataCollector dataCollector;

    private boolean useManualBrowserSwitch;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_paypal, container, false);
        TextInputEditText buyerEmailEditText = view.findViewById(R.id.buyer_email_edit_text);
        TextInputEditText buyerPhoneEditText = view.findViewById(R.id.buyer_phone_edit_text);
        Button billingAgreementButton = view.findViewById(R.id.paypal_billing_agreement_button);
        Button singlePaymentButton = view.findViewById(R.id.paypal_single_payment_button);

        singlePaymentButton.setOnClickListener(v -> {
            launchPayPal(
                false,
                buyerEmailEditText.getText().toString(),
                buyerPhoneEditText.getText().toString()
            );
        });
        billingAgreementButton.setOnClickListener(v -> {
            launchPayPal(true,
                buyerEmailEditText.getText().toString(),
                buyerPhoneEditText.getText().toString()
            );
        });

        braintreeClient = getBraintreeClient();

        useManualBrowserSwitch = Settings.isManualBrowserSwitchingEnabled(requireActivity());
        if (useManualBrowserSwitch) {
            payPalClient = new PayPalClient(braintreeClient);
        } else {
            payPalClient = new PayPalClient(this, braintreeClient);
            payPalClient.setListener(this);
        }

        amount = RandomDollarAmount.getNext();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (useManualBrowserSwitch) {
            Activity activity = requireActivity();
            BrowserSwitchResult browserSwitchResult =
                payPalClient.parseBrowserSwitchResult(activity, activity.getIntent());
            if (browserSwitchResult != null) {
                handleBrowserSwitchResult(browserSwitchResult);
            }
        }
    }

    private void handleBrowserSwitchResult(BrowserSwitchResult browserSwitchResult) {
        payPalClient.onBrowserSwitchResult(browserSwitchResult, ((payPalAccountNonce, error) -> {
            if (payPalAccountNonce != null) {
                handlePayPalResult(payPalAccountNonce);
            } else if (error != null) {
                handleError(error);
            }
        }));
        payPalClient.clearActiveBrowserSwitchRequests(requireContext());
    }

    private void launchPayPal(
        boolean isBillingAgreement,
        String buyerEmailAddress,
        String buyerPhoneNumber
    ) {
        FragmentActivity activity = getActivity();
        activity.setProgressBarIndeterminateVisibility(true);

        dataCollector = new DataCollector(braintreeClient);

        braintreeClient.getConfiguration((configuration, configError) -> {
            if (Settings.shouldCollectDeviceData(requireActivity())) {
                dataCollector.collectDeviceData(requireActivity(), (deviceDataResult, error) -> {
                    if (deviceDataResult != null) {
                        deviceData = deviceDataResult;
                    }
                    if (isBillingAgreement) {
                        payPalClient.tokenizePayPalAccount(activity, createPayPalVaultRequest(activity, buyerEmailAddress, buyerPhoneNumber));
                    } else {
                        payPalClient.tokenizePayPalAccount(activity, createPayPalCheckoutRequest(activity, amount, buyerEmailAddress, buyerPhoneNumber));
                    }
                });
            } else {
                if (isBillingAgreement) {
                    payPalClient.tokenizePayPalAccount(activity, createPayPalVaultRequest(activity, buyerEmailAddress, buyerPhoneNumber));
                } else {
                    payPalClient.tokenizePayPalAccount(activity, createPayPalCheckoutRequest(activity, amount, buyerEmailAddress, buyerPhoneNumber));
                }
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

            NavHostFragment.findNavController(this).navigate(action);
        }
    }

    @Override
    public void onPayPalSuccess(@NonNull PayPalAccountNonce payPalAccountNonce) {
        handlePayPalResult(payPalAccountNonce);
    }

    @Override
    public void onPayPalFailure(@NonNull Exception error) {
        handleError(error);
    }
}
