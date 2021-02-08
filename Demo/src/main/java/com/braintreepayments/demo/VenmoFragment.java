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
import com.braintreepayments.api.Configuration;
import com.braintreepayments.api.ConfigurationCallback;
import com.braintreepayments.api.VenmoAccountNonce;
import com.braintreepayments.api.VenmoAuthorizeAccountCallback;
import com.braintreepayments.api.VenmoClient;
import com.braintreepayments.api.VenmoOnActivityResultCallback;

public class VenmoFragment extends BaseFragment {

    private ImageButton mVenmoButton;
    private VenmoClient venmoClient;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_venmo, container, false);
        mVenmoButton = view.findViewById(R.id.venmo_button);
        mVenmoButton.setOnClickListener(this::launchVenmo);

        DemoViewModel viewModel = new ViewModelProvider(getActivity()).get(DemoViewModel.class);
        viewModel.getVenmoActivityResult().observe(getViewLifecycleOwner(), this::handleVenmoActivityResult);

        return view;
    }

    public void handleVenmoActivityResult(ActivityResult activityResult) {
        venmoClient.onActivityResult(getActivity(), activityResult.getResultCode(), activityResult.getData(), new VenmoOnActivityResultCallback() {
            @Override
            public void onResult(@Nullable VenmoAccountNonce venmoAccountNonce, @Nullable Exception error) {
                handleVenmoResult(venmoAccountNonce);
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

        getBraintreeClient(new BraintreeClientCallback() {
            @Override
            public void onResult(@Nullable BraintreeClient braintreeClient) {
               if (braintreeClient != null) {
                   venmoClient = new VenmoClient(braintreeClient);

                   braintreeClient.getConfiguration(new ConfigurationCallback() {
                       @Override
                       public void onResult(@Nullable Configuration configuration, @Nullable Exception error) {
                           if (venmoClient.isVenmoAppSwitchAvailable(activity)) {
                               venmoClient.authorizeAccount(activity, shouldVault, null, new VenmoAuthorizeAccountCallback() {
                                   @Override
                                   public void onResult(Exception error) {
                                       if (error != null) {
                                           handleError(error);
                                       }
                                   }
                               });
                           } else if (configuration.getPayWithVenmo().isAccessTokenValid()) {
                               showDialog("Please install the Venmo app first.");
                           } else {
                               showDialog("Venmo is not enabled for the current merchant.");
                           }
                       }
                   });
               }
            }
        });
    }

}
