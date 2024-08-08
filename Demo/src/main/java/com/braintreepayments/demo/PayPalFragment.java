package com.braintreepayments.demo;

import static com.braintreepayments.demo.PayPalRequestFactory.createPayPalCheckoutRequest;
import static com.braintreepayments.demo.PayPalRequestFactory.createPayPalVaultRequest;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.navigation.fragment.NavHostFragment;

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

    private DataCollector dataCollector;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_paypal, container, false);
        TextInputEditText buyerEmailEditText = view.findViewById(R.id.buyer_email_edit_text);
        Button billingAgreementButton = view.findViewById(R.id.paypal_billing_agreement_button);
        Button singlePaymentButton = view.findViewById(R.id.paypal_single_payment_button);

        singlePaymentButton.setOnClickListener(v -> {
            launchPayPal(false, buyerEmailEditText.getText().toString());
        });
        billingAgreementButton.setOnClickListener(v -> {
            launchPayPal(true, buyerEmailEditText.getText().toString());
        });

        payPalClient = new PayPalClient(
                requireContext(),
                super.getAuthStringArg(),
                Uri.parse("https://mobile-sdk-demo-site-838cead5d3ab.herokuapp.com/braintree-payments")
        );
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

    private void launchPayPal(boolean isBillingAgreement, String buyerEmailAddress) {
        FragmentActivity activity = getActivity();
        activity.setProgressBarIndeterminateVisibility(true);

        dataCollector = new DataCollector(requireContext(), super.getAuthStringArg());

        if (Settings.shouldCollectDeviceData(requireActivity())) {
            dataCollector.collectDeviceData(requireActivity(), new DataCollectorRequest(true), (dataCollectorResult) -> {
                if (dataCollectorResult instanceof DataCollectorResult.Success) {
                    deviceData = ((DataCollectorResult.Success) dataCollectorResult).getDeviceData();
                }
                launchPayPal(activity, isBillingAgreement, amount, buyerEmailAddress);
            });
        } else {
            launchPayPal(activity, isBillingAgreement, amount, buyerEmailAddress);
        }
    }

    private void launchPayPal(
        FragmentActivity activity,
        boolean isBillingAgreement,
        String amount,
        String buyerEmailAddress
    ) {
        PayPalRequest payPalRequest;
        if (isBillingAgreement) {
            payPalRequest = createPayPalVaultRequest(activity, buyerEmailAddress);
        } else {
            payPalRequest = createPayPalCheckoutRequest(activity, amount, buyerEmailAddress);
        }
        payPalClient.createPaymentAuthRequest(requireContext(), payPalRequest,
                (paymentAuthRequest) -> {
                    if (paymentAuthRequest instanceof PayPalPaymentAuthRequest.Failure) {
                        handleError(((PayPalPaymentAuthRequest.Failure) paymentAuthRequest).getError());
                    } else if (paymentAuthRequest instanceof PayPalPaymentAuthRequest.ReadyToLaunch){
                        PayPalPendingRequest request = payPalLauncher.launch(requireActivity(),
                                ((PayPalPaymentAuthRequest.ReadyToLaunch) paymentAuthRequest));

                        String pairingId = ((PayPalPaymentAuthRequest.ReadyToLaunch) paymentAuthRequest).getRequestParams().getPairingId();

                        if (pairingId != null && !pairingId.isEmpty()) {
                            Toast.makeText(getActivity(), pairingId, Toast.LENGTH_LONG).show();
                        }

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

            NavHostFragment.findNavController(this).navigate(action);
        }
    }
}
