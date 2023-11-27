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

import com.braintreepayments.api.BraintreeClient;
import com.braintreepayments.api.UserCanceledException;
import com.braintreepayments.api.VenmoAccountNonce;
import com.braintreepayments.api.VenmoClient;
import com.braintreepayments.api.VenmoLauncher;
import com.braintreepayments.api.VenmoLineItem;
import com.braintreepayments.api.VenmoPaymentAuthRequest;
import com.braintreepayments.api.VenmoPaymentMethodUsage;
import com.braintreepayments.api.VenmoResult;
import com.braintreepayments.api.VenmoRequest;

import java.util.ArrayList;

public class VenmoFragment extends BaseFragment {

    private ImageButton venmoButton;
    private VenmoClient venmoClient;
    private VenmoLauncher venmoLauncher;
    private BraintreeClient braintreeClient;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_venmo, container, false);
        venmoButton = view.findViewById(R.id.venmo_button);
        venmoButton.setOnClickListener(this::launchVenmo);

        venmoLauncher = new VenmoLauncher(this, venmoAuthChallengeResult ->
                venmoClient.tokenize(venmoAuthChallengeResult, this::handleVenmoResult));

        return view;
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
            braintreeClient = getBraintreeClient();
            venmoClient = new VenmoClient(braintreeClient);
        }

        FragmentActivity activity = getActivity();

        boolean shouldVault =
                Settings.vaultVenmo(activity) && !TextUtils.isEmpty(Settings.getCustomerId(activity));

        braintreeClient.getConfiguration((configuration, error) -> {
            if (venmoClient.isVenmoAppSwitchAvailable(activity)) {
                VenmoRequest venmoRequest = new VenmoRequest(VenmoPaymentMethodUsage.SINGLE_USE);
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
                venmoRequest.setLineItems(lineItems);

                venmoClient.createPaymentAuthRequest(requireActivity(), venmoRequest, (paymentAuthRequest) -> {
                    if (paymentAuthRequest instanceof VenmoPaymentAuthRequest.Failure) {
                        handleError(((VenmoPaymentAuthRequest.Failure) paymentAuthRequest).getError());
                    } else if (paymentAuthRequest instanceof VenmoPaymentAuthRequest.ReadyToLaunch) {
                        venmoLauncher.launch((VenmoPaymentAuthRequest.ReadyToLaunch) paymentAuthRequest);
                    }
                });
            } else if (configuration.isVenmoEnabled()) {
                showDialog("Please install the Venmo app first.");
            } else {
                showDialog("Venmo is not enabled for the current merchant.");
            }
        });
    }
}
