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
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavDirections;
import androidx.navigation.fragment.NavHostFragment;

import com.braintreepayments.api.BraintreeClient;
import com.braintreepayments.api.VenmoAccountNonce;
import com.braintreepayments.api.VenmoClient;
import com.braintreepayments.api.VenmoOnActivityResultCallback;
import com.braintreepayments.api.VenmoPaymentMethodUsage;
import com.braintreepayments.api.VenmoRequest;
import com.braintreepayments.api.VenmoTokenizeAccountCallback;

public class VenmoFragment extends BaseFragment {

    private ImageButton venmoButton;
    private VenmoClient venmoClient;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_venmo, container, false);
        venmoButton = view.findViewById(R.id.venmo_button);
        venmoButton.setOnClickListener(this::launchVenmo);

        DemoViewModel viewModel = new ViewModelProvider(getActivity()).get(DemoViewModel.class);
        viewModel.getVenmoActivityResult().observe(getViewLifecycleOwner(), this::handleVenmoActivityResult);

        return view;
    }

    public void handleVenmoActivityResult(ActivityResult activityResult) {
        venmoClient.onActivityResult(getActivity(), activityResult.getResultCode(), activityResult.getData(), new VenmoOnActivityResultCallback() {
            @Override
            public void onResult(@Nullable VenmoAccountNonce venmoAccountNonce, @Nullable Exception error) {
                if (error != null) {
                    handleError(error);
                } else {
                    handleVenmoResult(venmoAccountNonce);
                }
            }
        });
    }

    private void handleVenmoResult(VenmoAccountNonce venmoAccountNonce) {
        super.onPaymentMethodNonceCreated(venmoAccountNonce);

        NavDirections action =
                VenmoFragmentDirections.actionVenmoFragmentToDisplayNonceFragment(venmoAccountNonce);
        NavHostFragment.findNavController(this).navigate(action);
    }

    public void launchVenmo(View v) {
        getActivity().setProgressBarIndeterminateVisibility(true);

        FragmentActivity activity = getActivity();

        boolean shouldVault =
                Settings.vaultVenmo(activity) && !TextUtils.isEmpty(Settings.getCustomerId(activity));

        BraintreeClient braintreeClient = getBraintreeClient();
        venmoClient = new VenmoClient(braintreeClient);

        braintreeClient.getConfiguration((configuration, error) -> {
            if (venmoClient.isVenmoAppSwitchAvailable(activity)) {
                VenmoRequest venmoRequest = new VenmoRequest(VenmoPaymentMethodUsage.SINGLE_USE);
                venmoRequest.setProfileId(null);
                venmoRequest.setShouldVault(shouldVault);

                venmoClient.tokenizeVenmoAccount(activity, venmoRequest, new VenmoTokenizeAccountCallback() {
                    @Override
                    public void onResult(Exception error) {
                        if (error != null) {
                            handleError(error);
                        }
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
