package com.braintreepayments.demo;

import static com.braintreepayments.demo.BraintreeClientFactory.createBraintreeClient;

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
import com.braintreepayments.api.VenmoAccountNonce;
import com.braintreepayments.api.VenmoClient;
import com.braintreepayments.api.VenmoListener;
import com.braintreepayments.api.VenmoPaymentMethodUsage;
import com.braintreepayments.api.VenmoRequest;

public class VenmoFragment extends BaseFragment implements VenmoListener {

    private ImageButton venmoButton;
    private VenmoClient venmoClient;
    private BraintreeClient braintreeClient;

    private AlertPresenter alertPresenter = new AlertPresenter();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_venmo, container, false);
        venmoButton = view.findViewById(R.id.venmo_button);
        venmoButton.setOnClickListener(this::launchVenmo);

        braintreeClient = createBraintreeClient(requireContext());
        venmoClient = new VenmoClient(this, braintreeClient);
        venmoClient.setListener(this);

        return view;
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

        braintreeClient.getConfiguration((configuration, error) -> {
            if (venmoClient.isVenmoAppSwitchAvailable(activity)) {
                VenmoRequest venmoRequest = new VenmoRequest(VenmoPaymentMethodUsage.SINGLE_USE);
                venmoRequest.setProfileId(null);
                venmoRequest.setShouldVault(shouldVault);

                venmoClient.tokenizeVenmoAccount(activity, venmoRequest);
            } else if (configuration.isVenmoEnabled()) {
                alertPresenter.showDialog(this, "Please install the Venmo app first.");
            } else {
                alertPresenter.showDialog(this, "Venmo is not enabled for the current merchant.");
            }
        });
    }

    @Override
    public void onVenmoSuccess(@NonNull VenmoAccountNonce venmoAccountNonce) {
        handleVenmoResult(venmoAccountNonce);
    }

    @Override
    public void onVenmoFailure(@NonNull Exception error) {
        alertPresenter.showErrorDialog(this, error);
    }
}
