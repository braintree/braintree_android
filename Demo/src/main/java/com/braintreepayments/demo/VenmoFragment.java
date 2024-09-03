package com.braintreepayments.demo;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.navigation.NavDirections;
import androidx.navigation.fragment.NavHostFragment;

import com.braintreepayments.api.venmo.VenmoAccountNonce;
import com.braintreepayments.api.venmo.VenmoClient;
import com.braintreepayments.api.venmo.VenmoLauncher;
import com.braintreepayments.api.venmo.VenmoLineItem;
import com.braintreepayments.api.venmo.VenmoPaymentAuthRequest;
import com.braintreepayments.api.venmo.VenmoPaymentAuthResult;
import com.braintreepayments.api.venmo.VenmoPaymentMethodUsage;
import com.braintreepayments.api.venmo.VenmoPendingRequest;
import com.braintreepayments.api.venmo.VenmoRequest;
import com.braintreepayments.api.venmo.VenmoResult;
import com.braintreepayments.api.core.UserCanceledException;

import java.util.ArrayList;

public class VenmoFragment extends BaseFragment {

    private ImageButton venmoButton;
    private VenmoClient venmoClient;
    private VenmoLauncher venmoLauncher;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_venmo, container, false);
        venmoButton = view.findViewById(R.id.venmo_button);
        venmoButton.setOnClickListener(this::launchVenmo);

        venmoLauncher = new VenmoLauncher();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        VenmoPendingRequest.Started pendingRequest = getPendingRequest();
        if (pendingRequest != null) {
            VenmoPaymentAuthResult paymentAuthResult = venmoLauncher.handleReturnToApp(pendingRequest, requireActivity().getIntent());
            if (paymentAuthResult instanceof VenmoPaymentAuthResult.Success) {
                completeVenmoFlow((VenmoPaymentAuthResult.Success) paymentAuthResult);
            } else {
                handleError(new Exception("User did not complete payment flow"));
            }
            clearPendingRequest();
        }
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

        NavDirections action =
                VenmoFragmentDirections.actionVenmoFragmentToDisplayNonceFragment(venmoAccountNonce);
        NavHostFragment.findNavController(this).navigate(action);
    }

    public void launchVenmo(View v) {
        getActivity().setProgressBarIndeterminateVisibility(true);
        if (venmoClient == null) {
            venmoClient = new VenmoClient(requireContext(), super.getAuthStringArg(), null);
        }

        FragmentActivity activity = getActivity();

        boolean shouldVault =
                Settings.vaultVenmo(activity) && !TextUtils.isEmpty(Settings.getCustomerId(activity));

        int venmoPaymentMethodUsage = shouldVault ?
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
        lineItems.add(new VenmoLineItem(VenmoLineItem.KIND_CREDIT, "Some Item", 1, "2"));
        lineItems.add(new VenmoLineItem(VenmoLineItem.KIND_DEBIT, "Two Items", 2, "10"));
        venmoRequest.setVenmoLineItem(lineItems);

        startVenmoFlow(venmoRequest);
    }

    private void startVenmoFlow(VenmoRequest venmoRequest) {
        venmoClient.createPaymentAuthRequest(requireActivity(), venmoRequest, (paymentAuthRequest) -> {
            if (paymentAuthRequest instanceof VenmoPaymentAuthRequest.Failure) {
                handleError(((VenmoPaymentAuthRequest.Failure) paymentAuthRequest).getError());
            } else if (paymentAuthRequest instanceof VenmoPaymentAuthRequest.ReadyToLaunch) {
                VenmoPendingRequest pendingRequest = venmoLauncher.launch(requireActivity(), (VenmoPaymentAuthRequest.ReadyToLaunch) paymentAuthRequest);
                if (pendingRequest instanceof VenmoPendingRequest.Started) {
                    storePendingRequest((VenmoPendingRequest.Started) pendingRequest);
                } else if (pendingRequest instanceof VenmoPendingRequest.Failure) {
                    handleError(((VenmoPendingRequest.Failure) pendingRequest).getError());
                }
            }
        });
    }

    private void completeVenmoFlow(VenmoPaymentAuthResult.Success paymentAuthResult) {
        venmoClient.tokenize(paymentAuthResult, this::handleVenmoResult);
    }

    private void storePendingRequest(VenmoPendingRequest.Started request) {
        PendingRequestStore.getInstance().putVenmoPendingRequest(requireContext(), request);
    }
    private VenmoPendingRequest.Started getPendingRequest() {
        return PendingRequestStore.getInstance().getVenmoPendingRequest(requireContext());
    }

    private void clearPendingRequest() {
        PendingRequestStore.getInstance().clearVenmoPendingRequest(requireContext());
    }
}
