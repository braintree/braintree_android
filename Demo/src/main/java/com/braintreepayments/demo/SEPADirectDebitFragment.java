package com.braintreepayments.demo;

import android.os.Bundle;
import android.util.Log;
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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

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
        request.setIban(generateRandomIBAN());
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

    private String generateRandomIBAN() {
        String countryCode = "FR";
        long bankCode = 30006L;
        String branchCode = "00001";

        long accountNumber = ThreadLocalRandom.current().nextLong(10_000_000_000l, 100_000_000_000l);
        String accountNumberWithChecksum = accountNumberWithChecksum(bankCode, Long.parseLong(branchCode), accountNumber);
        String checksum = checksum(bankCode, branchCode, accountNumberWithChecksum);

        String result = countryCode + checksum + String.valueOf(bankCode) + branchCode + accountNumberWithChecksum;
        Log.d("asdf", result);
        return result;
    }

    private String accountNumberWithChecksum(long bankCode, long branchCode, long accountNumber) {
        long sum = 89 * bankCode + 15 * branchCode + 3 * accountNumber;
        long checksum = 97 - calculateMod97(sum);

        long numDigitsInChecksum = (long) (Math.log10(checksum) + 1);
        return String.valueOf(accountNumber) + String.valueOf(checksum);
    }

    private long calculateMod97(long accountNumber) {
        long copy = accountNumber;
        long result = 0;

        List<Long> arr = new ArrayList<>();

        while(copy > 0){
            Long lastDigit = copy % 10;
            arr.add(lastDigit);
            copy /= 10;
        }

        for(int i = arr.size() - 1; i >= 0; i--) {
            result = (result * 10 + arr.get(i)) % 97;
        }
        return result;
    }

    private String checksum(long bankCode, String branchCode, String accountNumber) {
        // 152700 is taken from the conversion table here: https://community.appway.com/screen/kb/article/generating-and-validating-an-iban-1683400256881#conversion-table
        // and is representative of the characters "FR" with 00 being added to the end for all bban's to calculate the checksum
        String bbanString = String.valueOf(bankCode) + branchCode + accountNumber + "152700";
        Log.d("asdf", "bbanString: " + bbanString);
        BigInteger bban = new BigInteger(bbanString);
        BigInteger modResult = bban.mod(BigInteger.valueOf(97));
        Log.d("asdf", "modResult = " + modResult);
        BigInteger subtractionResult = BigInteger.valueOf(98).subtract(modResult);
        Log.d("asdf", "subtractionResult = " + subtractionResult);

        return String.format("%02d", subtractionResult);
    }

    private long concatenate(long lhs, long rhs) {
        int numDigitsInRHS = (int) Math.log10(rhs) + 1;
        return (long) (lhs * Math.pow(10, numDigitsInRHS)) + rhs;
    }
}
