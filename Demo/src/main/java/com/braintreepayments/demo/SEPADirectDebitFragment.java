package com.braintreepayments.demo;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.navigation.fragment.NavHostFragment;

import com.braintreepayments.api.PostalAddress;
import com.braintreepayments.api.SEPADirectDebitClient;
import com.braintreepayments.api.SEPADirectDebitLauncher;
import com.braintreepayments.api.SEPADirectDebitMandateType;
import com.braintreepayments.api.SEPADirectDebitNonce;
import com.braintreepayments.api.SEPADirectDebitPaymentAuthRequest;
import com.braintreepayments.api.SEPADirectDebitRequest;
import com.braintreepayments.api.SEPADirectDebitResult;
import com.braintreepayments.api.UserCanceledException;

import java.util.UUID;

public class SEPADirectDebitFragment extends BaseFragment {

    private SEPADirectDebitClient sepaDirectDebitClient;
    private SEPADirectDebitLauncher sepaDirectDebitLauncher;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_sepa_direct_debit, container, false);
        Button button = view.findViewById(R.id.sepa_direct_debit_button);
        button.setOnClickListener(this::launchSEPADirectDebit);

        sepaDirectDebitClient = new SEPADirectDebitClient(requireContext(), super.getAuthStringArg());

        sepaDirectDebitLauncher = new SEPADirectDebitLauncher(sepaDirectDebitBrowserSwitchResult ->
            sepaDirectDebitClient.tokenize(sepaDirectDebitBrowserSwitchResult, (result) -> {
                if (result instanceof SEPADirectDebitResult.Failure) {
                    handleError(((SEPADirectDebitResult.Failure) result).getError());
                } else if (result instanceof SEPADirectDebitResult.Cancel) {
                    handleError(new UserCanceledException("User canceled SEPA Direct Debit"));
                } else if (result instanceof SEPADirectDebitResult.Success) {
                    handleSEPANonce(((SEPADirectDebitResult.Success) result).getNonce());
                }
            })
        );

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        sepaDirectDebitLauncher.handleReturnToAppFromBrowser(requireContext(), requireActivity().getIntent());
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
                handleError(((SEPADirectDebitPaymentAuthRequest.Failure) paymentAuthRequest).getError());
            } else if (paymentAuthRequest instanceof SEPADirectDebitPaymentAuthRequest.LaunchNotRequired) {
                handleSEPANonce(((SEPADirectDebitPaymentAuthRequest.LaunchNotRequired) paymentAuthRequest).getNonce());
            } else if (paymentAuthRequest instanceof SEPADirectDebitPaymentAuthRequest.ReadyToLaunch) {
                sepaDirectDebitLauncher.launch(requireActivity(), (SEPADirectDebitPaymentAuthRequest.ReadyToLaunch) paymentAuthRequest);
            }
        });
    }

    private String generateRandomCustomerId() {
        return UUID.randomUUID().toString().substring(0,20);
    }

    private void handleSEPANonce(@NonNull SEPADirectDebitNonce sepaDirectDebitNonce) {
        super.onPaymentMethodNonceCreated(sepaDirectDebitNonce);

        SEPADirectDebitFragmentDirections.ActionSepaDirectDebitFragmentToDisplayNonceFragment action =
                SEPADirectDebitFragmentDirections.actionSepaDirectDebitFragmentToDisplayNonceFragment(sepaDirectDebitNonce);
        NavHostFragment.findNavController(this).navigate(action);
    }
}
