package com.braintreepayments.demo;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.navigation.fragment.NavHostFragment;

import com.braintreepayments.api.localpayment.LocalPaymentAuthRequest;
import com.braintreepayments.api.localpayment.LocalPaymentAuthResult;
import com.braintreepayments.api.localpayment.LocalPaymentClient;
import com.braintreepayments.api.localpayment.LocalPaymentLauncher;
import com.braintreepayments.api.localpayment.LocalPaymentNonce;
import com.braintreepayments.api.localpayment.LocalPaymentPendingRequest;
import com.braintreepayments.api.localpayment.LocalPaymentRequest;
import com.braintreepayments.api.localpayment.LocalPaymentResult;
import com.braintreepayments.api.core.BraintreeException;
import com.braintreepayments.api.core.PostalAddress;
import com.braintreepayments.api.core.UserCanceledException;

public class LocalPaymentFragment extends BaseFragment {

    private LocalPaymentClient localPaymentClient;
    private final LocalPaymentLauncher localPaymentLauncher = new LocalPaymentLauncher();


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_local_payment, container, false);
        Button mIdealButton = view.findViewById(R.id.ideal_button);
        mIdealButton.setOnClickListener(this::launchIdeal);

        localPaymentClient = new LocalPaymentClient(requireContext(), super.getAuthStringArg(), null);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        LocalPaymentPendingRequest.Started pendingRequest = getPendingRequest();
        if (getPendingRequest() != null) {
            LocalPaymentAuthResult paymentAuthResult =
                    localPaymentLauncher.handleReturnToAppFromBrowser(pendingRequest,
                            requireActivity().getIntent());
            if (paymentAuthResult instanceof LocalPaymentAuthResult.Success) {
                localPaymentClient.tokenize(requireContext(),
                        (LocalPaymentAuthResult.Success) paymentAuthResult,
                        this::handleLocalPaymentResult);
            } else {
                handleError(new BraintreeException("User did not complete local payment flow"));
            }
            clearPendingRequest();
        }
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

        LocalPaymentRequest request = new LocalPaymentRequest(true);
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
                LocalPaymentPendingRequest pendingRequest = localPaymentLauncher.launch(requireActivity(),
                        (LocalPaymentAuthRequest.ReadyToLaunch) paymentAuthRequest);
                if (pendingRequest instanceof LocalPaymentPendingRequest.Started) {
                    storePendingRequest((LocalPaymentPendingRequest.Started) pendingRequest);
                } else if (pendingRequest instanceof LocalPaymentPendingRequest.Failure) {
                    handleError(((LocalPaymentPendingRequest.Failure) pendingRequest).getError());
                }
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

    private void storePendingRequest(LocalPaymentPendingRequest.Started request) {
        PendingRequestStore.getInstance().putLocalPaymentPendingRequest(requireContext(), request);
    }
    private LocalPaymentPendingRequest.Started getPendingRequest() {
        return PendingRequestStore.getInstance().getLocalPaymentPendingRequest(requireContext());
    }

    private void clearPendingRequest() {
        PendingRequestStore.getInstance().clearLocalPaymentPendingRequest(requireContext());
    }
}
