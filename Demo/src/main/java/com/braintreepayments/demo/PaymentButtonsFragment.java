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

import com.braintreepayments.api.core.UserCanceledException;
import com.braintreepayments.api.uicomponents.PayPalButton;
import com.braintreepayments.api.uicomponents.PayPalButtonColor;
import com.braintreepayments.api.uicomponents.VenmoButton;
import com.braintreepayments.api.uicomponents.VenmoButtonColor;
import com.braintreepayments.api.venmo.VenmoAccountNonce;
import com.braintreepayments.api.venmo.VenmoClient;
import com.braintreepayments.api.venmo.VenmoLauncher;
import com.braintreepayments.api.venmo.VenmoLineItem;
import com.braintreepayments.api.venmo.VenmoLineItemKind;
import com.braintreepayments.api.venmo.VenmoPaymentAuthRequest;
import com.braintreepayments.api.venmo.VenmoPaymentAuthResult;
import com.braintreepayments.api.venmo.VenmoPaymentMethodUsage;
import com.braintreepayments.api.venmo.VenmoPendingRequest;
import com.braintreepayments.api.venmo.VenmoRequest;
import com.braintreepayments.api.venmo.VenmoResult;
import com.google.android.material.button.MaterialButtonToggleGroup;

import java.util.ArrayList;

public class PaymentButtonsFragment extends BaseFragment {

    private PayPalButton payPalButton;
    private MaterialButtonToggleGroup payPalToggleGroup;
    private VenmoButton venmoButton;
    private VenmoClient venmoClient;
    private VenmoLauncher venmoLauncher;
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

        venmoButton = view.findViewById(R.id.venmo_payment_button);
        venmoToggleGroup = view.findViewById(R.id.venmo_button_toggle_group);
        venmoButton.setVenmoClickListener(new VenmoButton.OnVenmoClickListener() {
            @Override
            public void onVenmoClick(VenmoButton view) {
                launchVenmo(view);
            }
        });
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

        if (venmoClient == null) {
            if (Settings.useAppLinkReturn(requireContext())) {
                venmoClient = new VenmoClient(
                        requireContext(),
                        super.getAuthStringArg(),
                        Uri.parse("https://mobile-sdk-demo-site-838cead5d3ab.herokuapp.com/braintree-payments"),
                        "com.braintreepayments.demo.braintree"
                );
            } else {
                venmoClient = new VenmoClient(requireContext(), super.getAuthStringArg());
            }
        }
        venmoLauncher = new VenmoLauncher();


        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        venmoButton.handleReturnToApp(requireActivity().getIntent());
//        VenmoPendingRequest.Started venmoPendingRequest = getVenmoPendingRequest();
//        if (venmoPendingRequest != null) {
//            VenmoPaymentAuthResult paymentAuthResult = venmoLauncher.handleReturnToApp(venmoPendingRequest, requireActivity().getIntent());
//            if (paymentAuthResult instanceof VenmoPaymentAuthResult.Success) {
//                completeVenmoFlow((VenmoPaymentAuthResult.Success) paymentAuthResult);
//            } else {
//                handleError(new Exception("User did not complete payment flow"));
//            }
//            clearVenmoPendingRequest();
//        }
    }

    private void handleVenmoResult(VenmoResult result) {
        if (result instanceof VenmoResult.Success) {
            handleVenmoAccountNonce(((VenmoResult.Success) result).getNonce());
        } else if (result instanceof VenmoResult.Failure) {
            handleError(((VenmoResult.Failure) result).getError());
        } else if (result instanceof VenmoResult.Cancel) {
            handleError(new UserCanceledException("User canceled Venmo"));
        }
    }

    private void handleVenmoAccountNonce(VenmoAccountNonce venmoAccountNonce) {
        super.onPaymentMethodNonceCreated(venmoAccountNonce);

        NavDirections action = PaymentButtonsFragmentDirections.actionPaymentButtonsFragmentToDisplayNonceFragment(venmoAccountNonce);
        NavHostFragment.findNavController(this).navigate(action);
    }

//    public void launchVenmo(View v) {
//        FragmentActivity activity = getActivity();
//
//        getActivity().setProgressBarIndeterminateVisibility(true);
//
//        boolean shouldVault =
//                Settings.vaultVenmo(activity) && !TextUtils.isEmpty(Settings.getCustomerId(activity));
//
//        VenmoPaymentMethodUsage venmoPaymentMethodUsage = shouldVault ?
//                VenmoPaymentMethodUsage.MULTI_USE : VenmoPaymentMethodUsage.SINGLE_USE;
//        VenmoRequest venmoRequest = new VenmoRequest(venmoPaymentMethodUsage);
//        venmoRequest.setProfileId(null);
//        venmoRequest.setShouldVault(shouldVault);
//        venmoRequest.setCollectCustomerBillingAddress(true);
//        venmoRequest.setCollectCustomerShippingAddress(true);
//        venmoRequest.setTotalAmount("20");
//        venmoRequest.setSubTotalAmount("18");
//        venmoRequest.setTaxAmount("1");
//        venmoRequest.setShippingAmount("1");
//        ArrayList<VenmoLineItem> lineItems = new ArrayList<>();
//        lineItems.add(new VenmoLineItem(VenmoLineItemKind.CREDIT, "Some Item", 1, "2"));
//        lineItems.add(new VenmoLineItem(VenmoLineItemKind.DEBIT, "Two Items", 2, "10"));
//        venmoRequest.setLineItems(lineItems);
//
//        startVenmoFlow(venmoRequest);
//    }

    private VenmoRequest createVenmoRequest() {
        FragmentActivity activity = getActivity();

        getActivity().setProgressBarIndeterminateVisibility(true);

        boolean shouldVault =
                Settings.vaultVenmo(activity) && !TextUtils.isEmpty(Settings.getCustomerId(activity));

        VenmoPaymentMethodUsage venmoPaymentMethodUsage = shouldVault ?
                VenmoPaymentMethodUsage.MULTI_USE : VenmoPaymentMethodUsage.SINGLE_USE;
        VenmoRequest venmoRequest = new VenmoRequest(venmoPaymentMethodUsage);
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

        return venmoRequest;
    }

    private void startVenmoFlow(VenmoRequest venmoRequest) {
        venmoClient.createPaymentAuthRequest(requireActivity(), venmoRequest, (paymentAuthRequest) -> {
            if (paymentAuthRequest instanceof VenmoPaymentAuthRequest.Failure) {
                handleError(((VenmoPaymentAuthRequest.Failure) paymentAuthRequest).getError());
            } else if (paymentAuthRequest instanceof VenmoPaymentAuthRequest.ReadyToLaunch) {
                VenmoPendingRequest pendingRequest = venmoLauncher.launch(requireActivity(), (VenmoPaymentAuthRequest.ReadyToLaunch) paymentAuthRequest);
                if (pendingRequest instanceof VenmoPendingRequest.Started) {
                    storeVenmoPendingRequest((VenmoPendingRequest.Started) pendingRequest);
                } else if (pendingRequest instanceof VenmoPendingRequest.Failure) {
                    handleError(((VenmoPendingRequest.Failure) pendingRequest).getError());
                }
            }
        });
    }

    private void completeVenmoFlow(VenmoPaymentAuthResult.Success paymentAuthResult) {
        venmoClient.tokenize(paymentAuthResult, this::handleVenmoResult);
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
