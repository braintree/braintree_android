package com.braintreepayments.demo;

import static com.braintreepayments.demo.BraintreeClientFactory.createBraintreeClient;
import static com.braintreepayments.demo.PayPalRequestFactory.createPayPalCheckoutRequest;
import static com.braintreepayments.demo.PayPalRequestFactory.createPayPalVaultRequest;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.navigation.fragment.NavHostFragment;

import com.braintreepayments.api.BraintreeClient;
import com.braintreepayments.api.DataCollector;
import com.braintreepayments.api.PayPalAccountNonce;
import com.braintreepayments.api.PayPalClient;
import com.braintreepayments.api.PayPalListener;
import com.braintreepayments.api.PaymentMethodNonce;

public class PayPalFragment extends Fragment implements PayPalListener {

    private String deviceData;
    private String amount;

    private BraintreeClient braintreeClient;
    private PayPalClient payPalClient;
    private DataCollector dataCollector;

    private AlertPresenter alertPresenter = new AlertPresenter();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_paypal, container, false);
        Button billingAgreementButton = view.findViewById(R.id.paypal_billing_agreement_button);
        Button singlePaymentButton = view.findViewById(R.id.paypal_single_payment_button);

        billingAgreementButton.setOnClickListener(this::launchBillingAgreement);
        singlePaymentButton.setOnClickListener(this::launchSinglePayment);

        braintreeClient = createBraintreeClient(requireContext());
        payPalClient = new PayPalClient(this, braintreeClient);
        payPalClient.setListener(this);

        amount = RandomDollarAmount.getNext();
        return view;
    }

    public void launchSinglePayment(View v) {
        launchPayPal(false);
    }

    public void launchBillingAgreement(View v) {
        launchPayPal(true);
    }

    private void launchPayPal(boolean isBillingAgreement) {
        FragmentActivity activity = requireActivity();
        dataCollector = new DataCollector(braintreeClient);

        braintreeClient.getConfiguration((configuration, configError) -> {
            if (Settings.shouldCollectDeviceData(requireActivity())) {
                dataCollector.collectDeviceData(requireActivity(), (deviceDataResult, error) -> {
                    if (deviceDataResult != null) {
                        deviceData = deviceDataResult;
                    }
                    if (isBillingAgreement) {
                        payPalClient.tokenizePayPalAccount(activity, createPayPalVaultRequest(activity));
                    } else {
                        payPalClient.tokenizePayPalAccount(activity, createPayPalCheckoutRequest(activity, amount));
                    }
                });
            } else {
                if (isBillingAgreement) {
                    payPalClient.tokenizePayPalAccount(activity, createPayPalVaultRequest(activity));
                } else {
                    payPalClient.tokenizePayPalAccount(activity, createPayPalCheckoutRequest(activity, amount));
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
        alertPresenter.showErrorDialog(this, error);
    }
}
