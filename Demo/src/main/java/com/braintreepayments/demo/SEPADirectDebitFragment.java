package com.braintreepayments.demo;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.navigation.fragment.NavHostFragment;

import com.braintreepayments.api.BraintreeClient;
import com.braintreepayments.api.PostalAddress;
import com.braintreepayments.api.SEPADirectDebitClient;
import com.braintreepayments.api.SEPADirectDebitListener;
import com.braintreepayments.api.SEPADirectDebitMandateType;
import com.braintreepayments.api.SEPADirectDebitNonce;
import com.braintreepayments.api.SEPADirectDebitRequest;

import java.math.BigInteger;
import java.util.UUID;

public class SEPADirectDebitFragment extends BaseFragment implements SEPADirectDebitListener {

    private SEPADirectDebitClient sepaDirectDebitClient;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_sepa_direct_debit, container, false);
        Button button = view.findViewById(R.id.sepa_direct_debit_button);
        button.setOnClickListener(this::launchSEPADirectDebit);

        BraintreeClient braintreeClient = getBraintreeClient();
        sepaDirectDebitClient = new SEPADirectDebitClient(this, braintreeClient);
        sepaDirectDebitClient.setListener(this);

        return view;
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
        request.setIban("FR7618106000321234566666608");
        request.setMandateType(SEPADirectDebitMandateType.RECURRENT);
        request.setBillingAddress(billingAddress);
        request.setMerchantAccountId("EUR-sepa-direct-debit");

        sepaDirectDebitClient.tokenize(requireActivity(), request);
    }

    private String generateRandomCustomerId() {
        return UUID.randomUUID().toString().substring(0,20);
    }

    @Override
    public void onSEPADirectDebitSuccess(@NonNull SEPADirectDebitNonce sepaDirectDebitNonce) {
        super.onPaymentMethodNonceCreated(sepaDirectDebitNonce);

        SEPADirectDebitFragmentDirections.ActionSepaDirectDebitFragmentToDisplayNonceFragment action =
                SEPADirectDebitFragmentDirections.actionSepaDirectDebitFragmentToDisplayNonceFragment(sepaDirectDebitNonce);
        NavHostFragment.findNavController(this).navigate(action);
    }

    @Override
    public void onSEPADirectDebitFailure(@NonNull Exception error) {
        handleError(error);
    }
}
