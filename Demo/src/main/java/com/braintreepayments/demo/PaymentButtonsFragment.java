package com.braintreepayments.demo;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.navigation.fragment.NavHostFragment;

import com.braintreepayments.api.core.PaymentMethodNonce;
import com.braintreepayments.api.paypal.PayPalClient;
import com.braintreepayments.api.paypal.PayPalLauncher;
import com.braintreepayments.api.paypal.PayPalPaymentAuthCallback;
import com.braintreepayments.api.paypal.PayPalPaymentAuthRequest;
import com.braintreepayments.api.paypal.PayPalPaymentAuthResult;
import com.braintreepayments.api.paypal.PayPalPendingRequest;
import com.braintreepayments.api.paypal.PayPalRequest;
import com.braintreepayments.api.paypal.PayPalResult;
import com.braintreepayments.api.uicomponents.PayPalButton;
import com.braintreepayments.api.uicomponents.PayPalButtonColor;
import com.braintreepayments.api.uicomponents.PayPalButtonView;
import com.google.android.material.button.MaterialButtonToggleGroup;

public class PaymentButtonsFragment extends BaseFragment {

    private PayPalButtonView payPalButtonView;
    private PayPalButton payPalButton;
    private PayPalLauncher payPalLauncher;
    private MaterialButtonToggleGroup toggleGroup;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_payment_buttons, container, false);
        payPalButtonView = view.findViewById(R.id.pp_payment_button);
        toggleGroup = view.findViewById(R.id.pp_button_toggle_group);

        toggleGroup.setSingleSelection(true);

        toggleGroup.addOnButtonCheckedListener(
                new MaterialButtonToggleGroup.OnButtonCheckedListener() {
                    @Override
                    public void onButtonChecked(MaterialButtonToggleGroup group, int checkedId, boolean isChecked) {
                        if (!isChecked) return;
                        switch (checkedId) {
                            case R.id.button_pp_blue:
                                payPalButtonView.setButtonColor(PayPalButtonColor.BLUE);
                                break;
                            case R.id.button_pp_black:
                                payPalButtonView.setButtonColor(PayPalButtonColor.BLACK);
                                break;
                            case R.id.button_pp_white:
                                payPalButtonView.setButtonColor(PayPalButtonColor.WHITE);
                                break;
                        }
                    }
                }
        );

        payPalLauncher = new PayPalLauncher();

        payPalButton = new PayPalButton(
                payPalButtonView,
                requireContext(),
                super.getAuthStringArg(),
                Uri.parse("https://mobile-sdk-demo-site-838cead5d3ab.herokuapp.com/braintree-payments"),
                "com.braintreepayments.demo.braintree"
        );

        PayPalClient payPalClient = new PayPalClient(
                requireContext(),
                super.getAuthStringArg(),
                Uri.parse("https://mobile-sdk-demo-site-838cead5d3ab.herokuapp.com/braintree-payments"),
                "com.braintreepayments.demo.braintree"
        );
        payPalButton.setPayPalClient(payPalClient);
        PayPalRequest payPalRequest = PayPalRequestFactory.createPayPalCheckoutRequest(
                    requireContext(),
                    "10.0",
                    null,
                    null,
                    null,
                    false,
                    null,
                    false,
                    false,
                    false
            );
        payPalButton.updatePayPalRequest(payPalRequest);
            PayPalPaymentAuthCallback payPalPaymentAuthCallback = new PayPalPaymentAuthCallback() {
                @Override
                public void onPayPalPaymentAuthRequest(@NonNull PayPalPaymentAuthRequest paymentAuthRequest) {
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
                }
            };
            payPalButtonView.setOnClickListener(v -> {
                payPalClient.createPaymentAuthRequest(requireContext(), payPalRequest, payPalPaymentAuthCallback);
            });

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
        }
    }

    private void storePendingRequest(PayPalPendingRequest.Started request) {
        PendingRequestStore.getInstance().putPayPalPendingRequest(requireContext(), request);
    }

    private PayPalPendingRequest.Started getPendingRequest() {
        return PendingRequestStore.getInstance().getPayPalPendingRequest(requireContext());
    }

    private void completePayPalFlow(PayPalPaymentAuthResult.Success paymentAuthResult) {
        payPalButton.tokenize(paymentAuthResult, payPalResult -> {
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

            PaymentButtonsFragmentDirections.ActionPaymentButtonsFragmentToDisplayNonceFragment action =
                PaymentButtonsFragmentDirections.actionPaymentButtonsFragmentToDisplayNonceFragment(
                    paymentMethodNonce
                );
            NavHostFragment.findNavController(this).navigate(action);
        }
    }

    private void clearPendingRequest() {
        PendingRequestStore.getInstance().clearPayPalPendingRequest(requireContext());
    }

}