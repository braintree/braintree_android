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
import com.braintreepayments.api.SEPADebitClient;
import com.braintreepayments.api.SEPADebitListener;
import com.braintreepayments.api.SEPADebitMandateType;
import com.braintreepayments.api.SEPADebitNonce;
import com.braintreepayments.api.SEPADebitRequest;

import java.math.BigInteger;
import java.util.UUID;

public class SEPADebitFragment extends BaseFragment implements SEPADebitListener {

    private SEPADebitClient sepaDebitClient;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_sepa_debit, container, false);
        Button button = view.findViewById(R.id.sepa_debit_button);
        button.setOnClickListener(this::launchSEPADebit);

        BraintreeClient braintreeClient = getBraintreeClient();
        sepaDebitClient = new SEPADebitClient(this, braintreeClient);
        sepaDebitClient.setListener(this);

        return view;
    }

    public void launchSEPADebit(View view) {
        PostalAddress billingAddress = new PostalAddress();
        billingAddress.setStreetAddress("Kantstraße 70");
        billingAddress.setExtendedAddress("#170");
        billingAddress.setLocality("Freistaat Sachsen");
        billingAddress.setRegion("Annaberg-buchholz");
        billingAddress.setPostalCode("09456");
        billingAddress.setCountryCodeAlpha2("FR");

        SEPADebitRequest request = new SEPADebitRequest();
        request.setAccountHolderName("John Doe");
        request.setCustomerId(generateRandomCustomerId());
        request.setIban(generateRandomIban());
        request.setMandateType(SEPADebitMandateType.RECURRENT);
        request.setBillingAddress(billingAddress);
        request.setMerchantAccountId("eur_pwpp_multi_account_merchant_account");

        sepaDebitClient.tokenize(requireActivity(), request);
    }

    private String generateRandomCustomerId() {
        return UUID.randomUUID().toString().substring(0,20);
    }

    private String generateRandomIban() {
        return "FR" + String.format("%040d", new BigInteger(UUID.randomUUID().toString().replace("-", ""), 16)).substring(0,25);
    }

    @Override
    public void onSEPADebitSuccess(@NonNull SEPADebitNonce sepaDebitNonce) {
        super.onPaymentMethodNonceCreated(sepaDebitNonce);

        PayPalFragmentDirections.ActionPayPalFragmentToDisplayNonceFragment action =
                PayPalFragmentDirections.actionPayPalFragmentToDisplayNonceFragment(sepaDebitNonce);
        NavHostFragment.findNavController(this).navigate(action);
    }

    @Override
    public void onSEPADebitFailure(@NonNull Exception error) {
        handleError(error);
    }
}
