package com.braintreepayments.demo;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.braintreepayments.api.BrowserSwitchException;
import com.braintreepayments.api.BrowserSwitchResult;
import com.braintreepayments.api.LocalPaymentClient;
import com.braintreepayments.api.LocalPaymentNonce;
import com.braintreepayments.api.LocalPaymentRequest;
import com.braintreepayments.api.PostalAddress;

import org.json.JSONException;

public class LocalPaymentFragment extends BaseFragment {

    private LocalPaymentClient localPaymentClient;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_local_payment, container, false);
        Button mIdealButton = view.findViewById(R.id.ideal_button);
        mIdealButton.setOnClickListener(this::launchIdeal);

        DemoViewModel viewModel = new ViewModelProvider(getActivity()).get(DemoViewModel.class);
        viewModel.getLocalPaymentBrowserSwitchResult().observe(getViewLifecycleOwner(), this::handleLocalPaymentBrowserSwitchResult);

        return view;
    }

    public void launchIdeal(View v) {
        getBraintreeClient(braintreeClient -> {

            if (braintreeClient != null) {
                if (!Settings.SANDBOX_ENV_NAME.equals(Settings.getEnvironment(getActivity()))) {
                    handleError(new Exception("To use this feature, enable the \"Sandbox\" environment."));
                    return;
                }

                localPaymentClient = new LocalPaymentClient(braintreeClient);
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
                        try {
                            localPaymentClient.approveTransaction(getActivity(), transaction);
                        } catch (JSONException | BrowserSwitchException e) {
                            e.printStackTrace();
                        }
                    }

                    if (error != null) {
                        handleError(error);
                    }
                });
            }
        });

    }

    public void handleLocalPaymentBrowserSwitchResult(BrowserSwitchResult browserSwitchResult) {
        if (browserSwitchResult != null) {
            localPaymentClient.onBrowserSwitchResult(getActivity(), browserSwitchResult, this::handleLocalPaymentResult);
        }
    }

    protected void handleLocalPaymentResult(LocalPaymentNonce localPaymentNonce, Exception error) {
        super.onPaymentMethodNonceCreated(localPaymentNonce);

        LocalPaymentFragmentDirections.ActionLocalPaymentFragmentToDisplayNonceFragment action =
                LocalPaymentFragmentDirections.actionLocalPaymentFragmentToDisplayNonceFragment(localPaymentNonce);
        NavHostFragment.findNavController(this).navigate(action);
    }
}
