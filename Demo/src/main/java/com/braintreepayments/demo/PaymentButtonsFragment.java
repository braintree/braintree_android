package com.braintreepayments.demo;

import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.navigation.NavDirections;
import androidx.navigation.fragment.NavHostFragment;

import com.braintreepayments.api.core.PaymentMethodNonce;
import com.braintreepayments.api.paypal.PayPalPendingRequest;
import com.braintreepayments.api.paypal.PayPalRequest;
import com.braintreepayments.api.paypal.PayPalResult;
import com.braintreepayments.api.uicomponents.PayPalButton;
import com.braintreepayments.api.uicomponents.PayPalButtonColor;
import com.braintreepayments.api.uicomponents.VenmoButton;
import com.braintreepayments.api.uicomponents.VenmoButtonColor;
import com.braintreepayments.api.venmo.VenmoAccountNonce;
import com.braintreepayments.api.venmo.VenmoLineItem;
import com.braintreepayments.api.venmo.VenmoLineItemKind;
import com.braintreepayments.api.venmo.VenmoPaymentMethodUsage;
import com.braintreepayments.api.venmo.VenmoPendingRequest;
import com.braintreepayments.api.venmo.VenmoRequest;
import com.braintreepayments.api.venmo.VenmoResult;
import com.google.android.material.button.MaterialButtonToggleGroup;

import java.util.ArrayList;

public class PaymentButtonsFragment extends BaseFragment {

    private PayPalButton payPalButton;
    private MaterialButtonToggleGroup toggleGroup;
    private MaterialButtonToggleGroup payPalToggleGroup;
    private VenmoButton venmoButton;
    private VenmoRequest venmoRequest;
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

        FragmentActivity activity = getActivity();

        getActivity().setProgressBarIndeterminateVisibility(true);

        boolean shouldVault =
                Settings.vaultVenmo(activity) && !TextUtils.isEmpty(Settings.getCustomerId(activity));

        VenmoPaymentMethodUsage venmoPaymentMethodUsage = shouldVault ?
                VenmoPaymentMethodUsage.MULTI_USE : VenmoPaymentMethodUsage.SINGLE_USE;
        venmoRequest = new VenmoRequest(venmoPaymentMethodUsage);
        venmoRequest.setProfileId(null);
        venmoRequest.setShouldVault(shouldVault);
        venmoRequest.setCollectCustomerBillingAddress(true);
        venmoRequest.setCollectCustomerShippingAddress(true);
        venmoRequest.setTotalAmount("20");
        venmoRequest.setSubTotalAmount("18");
        venmoRequest.setTaxAmount("1");
        venmoRequest.setShippingAmount("1");
        ArrayList<VenmoLineItem> lineItems = new ArrayList<>();
        lineItems.add(new VenmoLineItem(VenmoLineItemKind.CREDIT, "Some Item", 1, "2"));
        lineItems.add(new VenmoLineItem(VenmoLineItemKind.DEBIT, "Two Items", 2, "10"));
        venmoRequest.setLineItems(lineItems);

        venmoButton.initialize(
                super.getAuthStringArg(),
                Uri.parse("https://mobile-sdk-demo-site-838cead5d3ab.herokuapp.com/braintree-payments"),
                "com.braintreepayments.demo.braintree"
        );

        venmoButton.setVenmoRequest(venmoRequest);
        venmoButton.setVenmoLaunchCallback(request -> {
            if (request instanceof VenmoPendingRequest.Started) {
                storeVenmoPendingRequest((VenmoPendingRequest.Started) request);
            } else if (request instanceof  VenmoPendingRequest.Failure) {
                handleError(((VenmoPendingRequest.Failure) request).getError());
            }
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        PayPalPendingRequest.Started pendingRequest = getPendingRequest();
        if (pendingRequest != null) {
            payPalButton.handleReturnToApp( pendingRequest, requireActivity().getIntent(),
                    payPalResult -> {
                        if (payPalResult instanceof PayPalResult.Success) {
                            handlePayPalResult(((PayPalResult.Success) payPalResult).getNonce());
                        } else if (payPalResult instanceof PayPalResult.Cancel) {
                            handleError(new Exception("User did not complete payment flow"));
                        } else if (payPalResult instanceof PayPalResult.Failure)  {
                            handleError(((PayPalResult.Failure) payPalResult).getError());
                        }
                    }
            );
            clearPendingRequest();
        }
        VenmoPendingRequest.Started venmoPendingRequest = getVenmoPendingRequest();
        if (venmoPendingRequest != null) {
            venmoButton.handleReturnToApp(venmoPendingRequest, requireActivity().getIntent(), venmoResult -> {
                if (venmoResult instanceof VenmoResult.Success) {
                    handleVenmoAccountNonce(((VenmoResult.Success) venmoResult).getNonce());
                } else if (venmoResult instanceof VenmoResult.Failure) {
                    handleError(((VenmoResult.Failure) venmoResult).getError());
                } else if (venmoResult instanceof VenmoResult.Cancel) {
                    handleError(new Exception("User did not complete payment flow"));
                }
            });
            clearVenmoPendingRequest();
        }
    }

    private void handleVenmoAccountNonce(VenmoAccountNonce venmoAccountNonce) {
        super.onPaymentMethodNonceCreated(venmoAccountNonce);

        NavDirections action = PaymentButtonsFragmentDirections
                .actionPaymentButtonsFragmentToDisplayNonceFragment(venmoAccountNonce)
                .setVenmoNonce(venmoAccountNonce.getString());
        NavHostFragment.findNavController(this).navigate(action);
    }

    private void storeVenmoPendingRequest(VenmoPendingRequest.Started request) {
        PendingRequestStore.getInstance().putVenmoPendingRequest(requireContext(), request);
    }

    private VenmoPendingRequest.Started getVenmoPendingRequest() {
        return PendingRequestStore.getInstance().getVenmoPendingRequest(requireContext());
    }
    private void clearVenmoPendingRequest() {
        PendingRequestStore.getInstance().clearVenmoPendingRequest(requireContext());
    }
}
    private void storePendingRequest(PayPalPendingRequest.Started request) {
        PendingRequestStore.getInstance().putPayPalPendingRequest(requireContext(), request);
    }

    private PayPalPendingRequest.Started getPendingRequest() {
        return PendingRequestStore.getInstance().getPayPalPendingRequest(requireContext());
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