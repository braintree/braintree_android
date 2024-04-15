package com.braintreepayments.demo;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.navigation.fragment.NavHostFragment;

import com.braintreepayments.api.core.PostalAddress;
import com.braintreepayments.api.sepadirectdebit.SEPADirectDebitClient;
import com.braintreepayments.api.sepadirectdebit.SEPADirectDebitLauncher;
import com.braintreepayments.api.sepadirectdebit.SEPADirectDebitMandateType;
import com.braintreepayments.api.sepadirectdebit.SEPADirectDebitNonce;
import com.braintreepayments.api.sepadirectdebit.SEPADirectDebitPaymentAuthRequest;
import com.braintreepayments.api.sepadirectdebit.SEPADirectDebitPaymentAuthResult;
import com.braintreepayments.api.sepadirectdebit.SEPADirectDebitPendingRequest;
import com.braintreepayments.api.sepadirectdebit.SEPADirectDebitRequest;
import com.braintreepayments.api.sepadirectdebit.SEPADirectDebitResult;
import com.braintreepayments.api.core.UserCanceledException;

import java.util.UUID;

public class SEPADirectDebitFragment extends BaseFragment {

    private SEPADirectDebitClient sepaDirectDebitClient;
    private final SEPADirectDebitLauncher sepaDirectDebitLauncher = new SEPADirectDebitLauncher();


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_sepa_direct_debit, container, false);
        Button button = view.findViewById(R.id.sepa_direct_debit_button);
        button.setOnClickListener(this::launchSEPADirectDebit);

        sepaDirectDebitClient =
                new SEPADirectDebitClient(requireContext(), super.getAuthStringArg());

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        SEPADirectDebitPendingRequest.Started pendingRequest = getPendingRequest();
        if (pendingRequest != null) {
            SEPADirectDebitPaymentAuthResult paymentAuthResult =
                    sepaDirectDebitLauncher.handleReturnToAppFromBrowser(pendingRequest,
                            requireActivity().getIntent());
            if (paymentAuthResult instanceof SEPADirectDebitPaymentAuthResult.Success) {
                completeSEPAFlow((SEPADirectDebitPaymentAuthResult.Success) paymentAuthResult);
            } else {
                handleError(new Exception("User did not complete payment flow"));
            }
            clearPendingRequest();
        }
    }

    public void launchSEPADirectDebit(View view) {
        PostalAddress billingAddress = new PostalAddress();
        billingAddress.setStreetAddress("Kantstraße 70");
        billingAddress.setExtendedAddress("#170");
        billingAddress.setLocality("Freistaat Sachsen");
        billingAddress.setRegion("Annaberg-buchholz");
        billingAddress.setPostalCode("09456");
        billingAddress.setCountryCodeAlpha2("FR");

        SEPADirectDebitRequest request = new SEPADirectDebitRequest();
        request.setAccountHolderName("John Doe");
        request.setCustomerId(generateRandomCustomerId());
        request.setIban(SEPADirectDebitTestHelper.generateSandboxIBAN());
        request.setMandateType(SEPADirectDebitMandateType.RECURRENT);
        request.setBillingAddress(billingAddress);
        request.setMerchantAccountId("EUR-sepa-direct-debit");

        sepaDirectDebitClient.createPaymentAuthRequest(request, (paymentAuthRequest) -> {
            if (paymentAuthRequest instanceof SEPADirectDebitPaymentAuthRequest.Failure) {
                handleError(
                        ((SEPADirectDebitPaymentAuthRequest.Failure) paymentAuthRequest).getError());
            } else if (paymentAuthRequest instanceof SEPADirectDebitPaymentAuthRequest.LaunchNotRequired) {
                handleSEPANonce(
                        ((SEPADirectDebitPaymentAuthRequest.LaunchNotRequired) paymentAuthRequest).getNonce());
            } else if (paymentAuthRequest instanceof SEPADirectDebitPaymentAuthRequest.ReadyToLaunch) {
                SEPADirectDebitPendingRequest pendingRequest =
                        sepaDirectDebitLauncher.launch(requireActivity(),
                                (SEPADirectDebitPaymentAuthRequest.ReadyToLaunch) paymentAuthRequest);
                if (pendingRequest instanceof SEPADirectDebitPendingRequest.Started) {
                    storePendingRequest((SEPADirectDebitPendingRequest.Started) pendingRequest);
                } else {
                    handleError(
                            ((SEPADirectDebitPendingRequest.Failure) pendingRequest).getError());
                }
            }
        });
    }

    private void completeSEPAFlow(SEPADirectDebitPaymentAuthResult.Success paymentAuthResult) {
        sepaDirectDebitClient.tokenize(paymentAuthResult, (result) -> {
            if (result instanceof SEPADirectDebitResult.Failure) {
                handleError(((SEPADirectDebitResult.Failure) result).getError());
            } else if (result instanceof SEPADirectDebitResult.Cancel) {
                handleError(new UserCanceledException("User canceled SEPA Direct Debit"));
            } else if (result instanceof SEPADirectDebitResult.Success) {
                handleSEPANonce(((SEPADirectDebitResult.Success) result).getNonce());
            }
        });
    }

    private String generateRandomCustomerId() {
        return UUID.randomUUID().toString().substring(0, 20);
    }

    private void handleSEPANonce(@NonNull SEPADirectDebitNonce sepaDirectDebitNonce) {
        super.onPaymentMethodNonceCreated(sepaDirectDebitNonce);

        SEPADirectDebitFragmentDirections.ActionSepaDirectDebitFragmentToDisplayNonceFragment
                action =
                SEPADirectDebitFragmentDirections.actionSepaDirectDebitFragmentToDisplayNonceFragment(
                        sepaDirectDebitNonce);
        NavHostFragment.findNavController(this).navigate(action);
    }

    private void storePendingRequest(SEPADirectDebitPendingRequest.Started request) {
        PendingRequestStore.getInstance().putSEPADirectDebitPendingRequest(requireContext(), request);
    }
    private SEPADirectDebitPendingRequest.Started getPendingRequest() {
        return PendingRequestStore.getInstance().getSEPADirectDebitPendingRequest(requireContext());
    }

    private void clearPendingRequest() {
        PendingRequestStore.getInstance().clearSEPADirectDebitPendingRequest(requireContext());
    }
}
