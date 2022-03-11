package com.braintreepayments.demo;

import static com.braintreepayments.demo.BraintreeClientFactory.createBraintreeClient;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.navigation.fragment.NavHostFragment;

import com.braintreepayments.api.BraintreeClient;
import com.braintreepayments.api.LocalPaymentClient;
import com.braintreepayments.api.LocalPaymentListener;
import com.braintreepayments.api.LocalPaymentNonce;
import com.braintreepayments.api.LocalPaymentRequest;
import com.braintreepayments.api.PostalAddress;

public class LocalPaymentFragment extends BaseFragment implements LocalPaymentListener {

    private LocalPaymentClient localPaymentClient;

    private BraintreeClient braintreeClient;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_local_payment, container, false);
        Button mIdealButton = view.findViewById(R.id.ideal_button);
        mIdealButton.setOnClickListener(this::launchIdeal);

        braintreeClient = createBraintreeClient(requireContext());
        localPaymentClient = new LocalPaymentClient(this, braintreeClient);
        localPaymentClient.setListener(this);

        return view;
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

        localPaymentClient.startPayment(request, (transaction, error) -> {
            if (transaction != null) {
                localPaymentClient.approveLocalPayment(requireActivity(), transaction);
            }

            if (error != null) {
                handleError(error);
            }
        });
    }

    protected void handleLocalPaymentResult(LocalPaymentNonce localPaymentNonce, Exception error) {
        super.onPaymentMethodNonceCreated(localPaymentNonce);

        if (error != null) {
            handleError(error);
            return;
        }

        LocalPaymentFragmentDirections.ActionLocalPaymentFragmentToDisplayNonceFragment action =
                LocalPaymentFragmentDirections.actionLocalPaymentFragmentToDisplayNonceFragment(localPaymentNonce);
        NavHostFragment.findNavController(this).navigate(action);
    }

    @Override
    public void onLocalPaymentSuccess(@NonNull LocalPaymentNonce localPaymentNonce) {
        handleLocalPaymentResult(localPaymentNonce, null);
    }

    @Override
    public void onLocalPaymentFailure(@NonNull Exception error) {
        handleLocalPaymentResult(null, error);
    }
}
