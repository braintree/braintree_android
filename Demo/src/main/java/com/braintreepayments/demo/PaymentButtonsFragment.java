package com.braintreepayments.demo;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.braintreepayments.api.paypal.PayPalPaymentAuthResult;
import com.braintreepayments.api.paypal.PayPalPendingRequest;
import com.braintreepayments.api.paypal.PayPalRequest;
import com.braintreepayments.api.uicomponents.PayPalButton;
import com.braintreepayments.api.uicomponents.PayPalButtonColor;
import com.braintreepayments.api.uicomponents.VenmoButton;
import com.braintreepayments.api.uicomponents.VenmoButtonColor;
import com.google.android.material.button.MaterialButtonToggleGroup;

public class PaymentButtonsFragment extends BaseFragment {

    private PayPalButton payPalButton;
    private MaterialButtonToggleGroup toggleGroup;
    private MaterialButtonToggleGroup payPalToggleGroup;
    private VenmoButton venmoButton;
    private MaterialButtonToggleGroup venmoToggleGroup;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_payment_buttons, container, false);
        payPalButton = view.findViewById(R.id.pp_payment_button);
        payPalToggleGroup = view.findViewById(R.id.pp_button_toggle_group);

        payPalToggleGroup.addOnButtonCheckedListener(
            new MaterialButtonToggleGroup.OnButtonCheckedListener() {
                @Override
                public void onButtonChecked(MaterialButtonToggleGroup group, int checkedId, boolean isChecked) {
                    if (!isChecked) return;
                    switch (checkedId) {
                        case R.id.button_pp_blue:
                            payPalButton.setButtonColor(PayPalButtonColor.BLUE);
                            break;
                        case R.id.button_pp_black:
                            payPalButton.setButtonColor(PayPalButtonColor.BLACK);
                            break;
                        case R.id.button_pp_white:
                            payPalButton.setButtonColor(PayPalButtonColor.WHITE);
                            break;
                    }
                }
            }
        );

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

        payPalButton.initialize(
            super.getAuthStringArg(),
            Uri.parse("https://mobile-sdk-demo-site-838cead5d3ab.herokuapp.com/braintree-payments"),
            "com.braintreepayments.demo.braintree"
        );
        payPalButton.setPayPalRequest(payPalRequest);
        payPalButton.setPayPalLaunchCallback(request -> {
            if (request instanceof PayPalPendingRequest.Started) {
                storePendingRequest((PayPalPendingRequest.Started) request);
            } else if (request instanceof PayPalPendingRequest.Failure) {
                handleError(((PayPalPendingRequest.Failure) request).getError());
            }
        });

//        payPalButton.updatePayPalRequest(payPalRequest);
//            PayPalPaymentAuthCallback payPalPaymentAuthCallback = new PayPalPaymentAuthCallback() {
//                @Override
//                public void onPayPalPaymentAuthRequest(@NonNull PayPalPaymentAuthRequest paymentAuthRequest) {
//                    if (paymentAuthRequest instanceof PayPalPaymentAuthRequest.Failure) {
//                        handleError(((PayPalPaymentAuthRequest.Failure) paymentAuthRequest).getError());
//                    } else if (paymentAuthRequest instanceof PayPalPaymentAuthRequest.ReadyToLaunch){
//                        PayPalPendingRequest request = payPalLauncher.launch(requireActivity(),
//                                ((PayPalPaymentAuthRequest.ReadyToLaunch) paymentAuthRequest));
//                        if (request instanceof PayPalPendingRequest.Started) {
//                            storePendingRequest((PayPalPendingRequest.Started) request);
//                        } else if (request instanceof PayPalPendingRequest.Failure) {
//                            handleError(((PayPalPendingRequest.Failure) request).getError());
//                        }
//                    }
//                }
//            };
//            payPalButtonView.setOnClickListener(v -> {
//                payPalClient.createPaymentAuthRequest(requireContext(), payPalRequest, payPalPaymentAuthCallback);
//            });


        venmoButton = view.findViewById(R.id.venmo_payment_button);
        venmoToggleGroup = view.findViewById(R.id.venmo_button_toggle_group);

        venmoToggleGroup.addOnButtonCheckedListener(
            new MaterialButtonToggleGroup.OnButtonCheckedListener() {
                @Override
                public void onButtonChecked(MaterialButtonToggleGroup group, int checkedId, boolean isChecked) {
                    if (!isChecked) return;
                    switch (checkedId) {
                        case R.id.button_venmo_blue:
                            venmoButton.setButtonColor(VenmoButtonColor.BLUE);
                            break;
                        case R.id.button_venmo_black:
                            venmoButton.setButtonColor(VenmoButtonColor.BLACK);
                            break;
                        case R.id.button_venmo_white:
                            venmoButton.setButtonColor(VenmoButtonColor.WHITE);
                            break;
                    }
                }
            }
        );

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        PayPalPendingRequest.Started pendingRequest = getPendingRequest();
        if (pendingRequest != null) {
            payPalButton.handleReturnToApp(pendingRequest, requireActivity().getIntent());

//            PayPalPaymentAuthResult paymentAuthResult = payPalLauncher.handleReturnToApp(pendingRequest, requireActivity().getIntent());
//            if (paymentAuthResult instanceof PayPalPaymentAuthResult.Success) {
//                completePayPalFlow((PayPalPaymentAuthResult.Success) paymentAuthResult);
//            } else {
//                handleError(new Exception("User did not complete payment flow"));
//            }
            clearPendingRequest();
        }
    }

    private void storePendingRequest(PayPalPendingRequest.Started request) {
        PendingRequestStore.getInstance().putPayPalPendingRequest(requireContext(), request);
    }

    private PayPalPendingRequest.Started getPendingRequest() {
        return PendingRequestStore.getInstance().getPayPalPendingRequest(requireContext());
    }

//    private void completePayPalFlow(PayPalPaymentAuthResult.Success paymentAuthResult) {
//        payPalButton.tokenize(paymentAuthResult, payPalResult -> {
//            if (payPalResult instanceof PayPalResult.Failure) {
//                handleError(((PayPalResult.Failure) payPalResult).getError());
//            } else if (payPalResult instanceof PayPalResult.Success) {
//                handlePayPalResult(((PayPalResult.Success) payPalResult).getNonce());
//            }
//        });
//    }
//
//    private void handlePayPalResult(PaymentMethodNonce paymentMethodNonce) {
//        if (paymentMethodNonce != null) {
//            super.onPaymentMethodNonceCreated(paymentMethodNonce);
//
//            PaymentButtonsFragmentDirections.ActionPaymentButtonsFragmentToDisplayNonceFragment action =
//                PaymentButtonsFragmentDirections.actionPaymentButtonsFragmentToDisplayNonceFragment(
//                    paymentMethodNonce
//                );
//            NavHostFragment.findNavController(this).navigate(action);
//        }
//    }
//
    private void clearPendingRequest() {
        PendingRequestStore.getInstance().clearPayPalPendingRequest(requireContext());
    }

}