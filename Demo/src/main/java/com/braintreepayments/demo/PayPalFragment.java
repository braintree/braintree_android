package com.braintreepayments.demo;

import static com.braintreepayments.demo.PayPalRequestFactory.createPayPalCheckoutRequest;
import static com.braintreepayments.demo.PayPalRequestFactory.createPayPalVaultRequest;

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
import com.braintreepayments.api.DataCollector;
import com.braintreepayments.api.Failure;
import com.braintreepayments.api.PayPalClient;
import com.braintreepayments.api.PayPalLauncher;
import com.braintreepayments.api.PayPalRequest;
import com.braintreepayments.api.PaymentMethodNonce;
import com.braintreepayments.api.Success;
import com.braintreepayments.api.Cancel;
import com.braintreepayments.api.UserCanceledException;

public class PayPalFragment extends BaseFragment {

    private String deviceData;
    private String amount;

    private BraintreeClient braintreeClient;
    private PayPalClient payPalClient;
    private PayPalLauncher payPalLauncher;

    private DataCollector dataCollector;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_paypal, container, false);
        Button billingAgreementButton = view.findViewById(R.id.paypal_billing_agreement_button);
        Button singlePaymentButton = view.findViewById(R.id.paypal_single_payment_button);

        billingAgreementButton.setOnClickListener(this::launchBillingAgreement);
        singlePaymentButton.setOnClickListener(this::launchSinglePayment);

        braintreeClient = getBraintreeClient();
        payPalClient = new PayPalClient(braintreeClient);
        payPalLauncher = new PayPalLauncher(
                payPalBrowserSwitchResult -> payPalClient.onBrowserSwitchResult(
                        payPalBrowserSwitchResult, (payPalResult) -> {
                            if (payPalResult instanceof Success) {
                                handlePayPalResult(((Success) payPalResult).getNonce());
                            } else if (payPalResult instanceof Failure) {
                                handleError(((Failure) payPalResult).getError());
                            } else if (payPalResult instanceof Cancel) {
                                handleError(new UserCanceledException("user cancelled"));
                            }
                        }));

        amount = RandomDollarAmount.getNext();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        payPalLauncher.handleReturnToAppFromBrowser(requireContext(), requireActivity().getIntent());
    }

    public void launchSinglePayment(View v) {
        launchPayPal(false);
    }

    public void launchBillingAgreement(View v) {
        launchPayPal(true);
    }

    private void launchPayPal(boolean isBillingAgreement) {
        FragmentActivity activity = getActivity();
        activity.setProgressBarIndeterminateVisibility(true);

        dataCollector = new DataCollector(braintreeClient);

        braintreeClient.getConfiguration((configuration, configError) -> {
            if (Settings.shouldCollectDeviceData(requireActivity())) {
                dataCollector.collectDeviceData(requireActivity(), (deviceDataResult, error) -> {
                    if (deviceDataResult != null) {
                        deviceData = deviceDataResult;
                    }
                    launchPayPal(activity, isBillingAgreement, amount);
                });
            } else {
                launchPayPal(activity, isBillingAgreement, amount);
            }
        });
    }

    private void launchPayPal(FragmentActivity activity, boolean isBillingAgreement,
                              String amount) {
        PayPalRequest payPalRequest;
        if (isBillingAgreement) {
            payPalRequest = createPayPalVaultRequest(activity);
        } else {
            payPalRequest = createPayPalCheckoutRequest(activity, amount);
        }
        payPalClient.tokenizePayPalAccount(activity, payPalRequest,
                (payPalResponse, error) -> {
                    if (error != null) {
                        handleError(error);
                    } else {
                        payPalLauncher.launch(requireActivity(), payPalResponse);
                    }
                });
    }

    private void handlePayPalResult(PaymentMethodNonce paymentMethodNonce) {
        if (paymentMethodNonce != null) {
            super.onPaymentMethodNonceCreated(paymentMethodNonce);

            PayPalFragmentDirections.ActionPayPalFragmentToDisplayNonceFragment action =
                    PayPalFragmentDirections.actionPayPalFragmentToDisplayNonceFragment(
                            paymentMethodNonce);
            action.setTransactionAmount(amount);
            action.setDeviceData(deviceData);

            NavHostFragment.findNavController(this).navigate(action);
        }
    }
}
