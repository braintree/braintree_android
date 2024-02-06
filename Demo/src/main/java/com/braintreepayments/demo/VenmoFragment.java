package com.braintreepayments.demo;

import android.app.Activity;
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
import com.braintreepayments.api.BrowserSwitchResult;
import com.braintreepayments.api.VenmoAccountNonce;
import com.braintreepayments.api.VenmoClient;
import com.braintreepayments.api.VenmoLineItem;
import com.braintreepayments.api.VenmoListener;
import com.braintreepayments.api.VenmoPaymentMethodUsage;
import com.braintreepayments.api.VenmoRequest;
import java.util.ArrayList;

public class VenmoFragment extends BaseFragment implements VenmoListener {

    private ImageButton venmoButton;
    private VenmoClient venmoClient;
    private BraintreeClient braintreeClient;

    private boolean useManualBrowserSwitch;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_venmo, container, false);
        venmoButton = view.findViewById(R.id.venmo_button);
        venmoButton.setOnClickListener(this::launchVenmo);

        useManualBrowserSwitch = Settings.isManualBrowserSwitchingEnabled(requireActivity());
        braintreeClient = getBraintreeClient();
        venmoClient = new VenmoClient(this, braintreeClient);

        if (!useManualBrowserSwitch) {
            venmoClient.setListener(this);
        }

        return view;
    }

    private void handleVenmoResult(VenmoAccountNonce venmoAccountNonce) {
        if (venmoAccountNonce != null) {
            super.onPaymentMethodNonceCreated(venmoAccountNonce);

            NavDirections action =
                    VenmoFragmentDirections.actionVenmoFragmentToDisplayNonceFragment(venmoAccountNonce);
            NavHostFragment.findNavController(this).navigate(action);
        }
    }

    public void launchVenmo(View v) {
        getActivity().setProgressBarIndeterminateVisibility(true);

        FragmentActivity activity = getActivity();

        boolean shouldVault =
                Settings.vaultVenmo(activity) && !TextUtils.isEmpty(Settings.getCustomerId(activity));
        boolean fallbackToWeb = Settings.venmoFallbackToWeb(activity);

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
        venmoRequest.setLineItems(lineItems);

        braintreeClient.getConfiguration((configuration, error) -> {
            if (fallbackToWeb) {
                venmoRequest.setFallbackToWeb(true);
                venmoClient.tokenizeVenmoAccount(activity, venmoRequest);
            } else {
                if (venmoClient.isVenmoAppSwitchAvailable(activity)) {
                    venmoClient.tokenizeVenmoAccount(activity, venmoRequest);
                } else if (configuration.isVenmoEnabled()) {
                    showDialog("Please install the Venmo app first.");
                } else {
                    showDialog("Venmo is not enabled for the current merchant.");
                }
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (useManualBrowserSwitch) {
            Activity activity = requireActivity();
            BrowserSwitchResult browserSwitchResult =
                    venmoClient.parseBrowserSwitchResult(activity, activity.getIntent());
            if (browserSwitchResult != null) {
                handleBrowserSwitchResult(browserSwitchResult);
            }
        }
    }

    private void handleBrowserSwitchResult(BrowserSwitchResult browserSwitchResult) {
        venmoClient.onBrowserSwitchResult(browserSwitchResult, ((venmoAccountNonce, error) -> {
            if (venmoAccountNonce != null) {
                handleVenmoResult(venmoAccountNonce);
            } else if (error != null) {
                handleError(error);
            }
        }));
        venmoClient.clearActiveBrowserSwitchRequests(requireContext());
    }

    @Override
    public void onVenmoSuccess(@NonNull VenmoAccountNonce venmoAccountNonce) {
        handleVenmoResult(venmoAccountNonce);
    }

    @Override
    public void onVenmoFailure(@NonNull Exception error) {
        handleError(error);
    }
}
