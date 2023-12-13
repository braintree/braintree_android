package com.braintreepayments.demo;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.navigation.fragment.NavHostFragment;

import com.braintreepayments.api.LocalPaymentAuthRequest;
import com.braintreepayments.api.LocalPaymentAuthResult;
import com.braintreepayments.api.LocalPaymentClient;
import com.braintreepayments.api.LocalPaymentLauncher;
import com.braintreepayments.api.LocalPaymentNonce;
import com.braintreepayments.api.LocalPaymentRequest;
import com.braintreepayments.api.LocalPaymentResult;
import com.braintreepayments.api.PostalAddress;
import com.braintreepayments.api.UserCanceledException;

public class LocalPaymentFragment extends BaseFragment {

    private LocalPaymentClient localPaymentClient;
    private LocalPaymentLauncher localPaymentLauncher;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_local_payment, container, false);
        Button mIdealButton = view.findViewById(R.id.ideal_button);
        mIdealButton.setOnClickListener(this::launchIdeal);

        localPaymentClient = new LocalPaymentClient(requireContext(), super.getAuthStringArg());
        localPaymentLauncher = new LocalPaymentLauncher(
                localPaymentResult -> localPaymentClient.tokenize(requireContext(),
                        localPaymentResult,
                        this::handleLocalPaymentResult));
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        localPaymentLauncher.handleReturnToAppFromBrowser(requireContext(),
                requireActivity().getIntent());
    }

    public void launchIdeal(View v) {
        if (!Settings.SANDBOX_ENV_NAME.equals(Settings.getEnvironment(getActivity()))) {
            handleError(new Exception("To use this feature, enable the \"Sandbox\" environment."));
            return;
        }

        PostalAddress address = new PostalAddress();
        address.setStreetAddress("Stadhouderskade 78");
        address.setCountryCodeAlpha2("NL");
        address.setLocality("Amsterdam");
        address.setPostalCode("1072 AE");

        LocalPaymentRequest request = new LocalPaymentRequest();
        request.setPaymentType("ideal");
        request.setAmount("1.10");
        request.setAddress(address);
        request.setPhone("207215300");
        request.setEmail("android-test-buyer@paypal.com");
        request.setGivenName("Test");
        request.setSurname("Buyer");
        request.setShippingAddressRequired(true);
        request.setMerchantAccountId("altpay_eur");
        request.setCurrencyCode("EUR");

        localPaymentClient.createPaymentAuthRequest(request, (paymentAuthRequest) -> {
            if (paymentAuthRequest instanceof LocalPaymentAuthRequest.ReadyToLaunch) {
                localPaymentLauncher.launch(requireActivity(),
                        (LocalPaymentAuthRequest.ReadyToLaunch) paymentAuthRequest);
            } else if (paymentAuthRequest instanceof LocalPaymentAuthRequest.Failure) {
                handleError(((LocalPaymentAuthRequest.Failure) paymentAuthRequest).getError());
            }
        });
    }

    protected void handleLocalPaymentResult(LocalPaymentResult localPaymentResult) {
        if (localPaymentResult instanceof LocalPaymentResult.Success) {
            onPaymentMethodNonceCreated(((LocalPaymentResult.Success) localPaymentResult).getNonce());
        } else if (localPaymentResult instanceof LocalPaymentResult.Failure) {
            handleError(((LocalPaymentResult.Failure) localPaymentResult).getError());
        } else if (localPaymentResult instanceof LocalPaymentResult.Cancel) {
            handleError(new UserCanceledException("User canceled Local Payment"));
        }
    }

    protected void onPaymentMethodNonceCreated(LocalPaymentNonce localPaymentNonce) {
        super.onPaymentMethodNonceCreated(localPaymentNonce);

        LocalPaymentFragmentDirections.ActionLocalPaymentFragmentToDisplayNonceFragment action =
                LocalPaymentFragmentDirections.actionLocalPaymentFragmentToDisplayNonceFragment(
                        localPaymentNonce);
        NavHostFragment.findNavController(this).navigate(action);
    }
}
