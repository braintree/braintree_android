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

import com.braintreepayments.api.DataCollector;
import com.braintreepayments.api.DataCollectorResult;
import com.braintreepayments.api.PayPalClient;
import com.braintreepayments.api.PayPalLauncher;
import com.braintreepayments.api.PayPalPaymentAuthRequest;
import com.braintreepayments.api.PayPalPaymentAuthResult;
import com.braintreepayments.api.PayPalPaymentAuthResultInfo;
import com.braintreepayments.api.PayPalPendingRequest;
import com.braintreepayments.api.PayPalRequest;
import com.braintreepayments.api.PayPalResult;
import com.braintreepayments.api.PaymentMethodNonce;

public class PayPalFragment extends BaseFragment {

    private String deviceData;
    private String amount;

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

        payPalClient = new PayPalClient(requireContext(), super.getAuthStringArg());
        payPalLauncher = new PayPalLauncher();

        amount = RandomDollarAmount.getNext();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        PayPalPendingRequest.Started pendingRequest = getPendingRequest();
        if (pendingRequest != null) {
            PayPalPaymentAuthResult paymentAuthResult = payPalLauncher.handleReturnToAppFromBrowser(pendingRequest, requireActivity().getIntent());
            if (paymentAuthResult instanceof PayPalPaymentAuthResult.Success) {
                completePayPalFlow((PayPalPaymentAuthResult.Success) paymentAuthResult);
            } else {
                handleError(new Exception("User did not complete payment flow"));
            }
            clearPendingRequest();
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

    public void launchSinglePayment(View v) {
        launchPayPal(false);
    }

    public void launchBillingAgreement(View v) {
        launchPayPal(true);
    }

    private void launchPayPal(boolean isBillingAgreement) {
        FragmentActivity activity = getActivity();
        activity.setProgressBarIndeterminateVisibility(true);

        dataCollector = new DataCollector(requireContext(), super.getAuthStringArg());

        if (Settings.shouldCollectDeviceData(requireActivity())) {
            dataCollector.collectDeviceData(requireActivity(), (dataCollectorResult) -> {
                if (dataCollectorResult instanceof DataCollectorResult.Success) {
                    deviceData = ((DataCollectorResult.Success) dataCollectorResult).getDeviceData();
                }
                launchPayPal(activity, isBillingAgreement, amount);
            });
        } else {
            launchPayPal(activity, isBillingAgreement, amount);
        }
    }

    private void launchPayPal(FragmentActivity activity, boolean isBillingAgreement,
                              String amount) {
        PayPalRequest payPalRequest;
        if (isBillingAgreement) {
            payPalRequest = createPayPalVaultRequest(activity);
        } else {
            payPalRequest = createPayPalCheckoutRequest(activity, amount);
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
                    PayPalFragmentDirections.actionPayPalFragmentToDisplayNonceFragment(
                            paymentMethodNonce);
            action.setTransactionAmount(amount);
            action.setDeviceData(deviceData);

            NavHostFragment.findNavController(this).navigate(action);
        }
    }
}
