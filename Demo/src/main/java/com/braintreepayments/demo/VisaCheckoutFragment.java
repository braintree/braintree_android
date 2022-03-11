package com.braintreepayments.demo;

import static com.braintreepayments.demo.BraintreeClientFactory.createBraintreeClient;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.navigation.fragment.NavHostFragment;

import com.braintreepayments.api.BraintreeClient;
import com.braintreepayments.api.PaymentMethodNonce;
import com.braintreepayments.api.VisaCheckoutClient;
import com.visa.checkout.CheckoutButton;
import com.visa.checkout.Profile;
import com.visa.checkout.PurchaseInfo;
import com.visa.checkout.VisaCheckoutSdk;
import com.visa.checkout.VisaPaymentSummary;

import java.math.BigDecimal;

public class VisaCheckoutFragment extends BaseFragment {

    private CheckoutButton checkoutButton;
    private VisaCheckoutClient visaCheckoutClient;

    private BraintreeClient braintreeClient;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_visa_checkout, container, false);
        checkoutButton = view.findViewById(R.id.visa_checkout_button);

        braintreeClient = createBraintreeClient(requireContext());
        visaCheckoutClient = new VisaCheckoutClient(braintreeClient);
        visaCheckoutClient.createProfileBuilder((profileBuilder, error) -> {
            if (profileBuilder != null) {
                setupVisaCheckoutButton(profileBuilder);
            } else {
                handleError(error);
            }
        });
        return view;
    }

    private void setupVisaCheckoutButton(Profile.ProfileBuilder profileBuilder) {
        PurchaseInfo purchaseInfo = new PurchaseInfo.PurchaseInfoBuilder(new BigDecimal("1.00"), PurchaseInfo.Currency.USD)
                .setDescription("Description")
                .build();

        checkoutButton.init(getActivity(), profileBuilder.build(), purchaseInfo, new VisaCheckoutSdk.VisaCheckoutResultListener() {
            @Override
            public void onButtonClick(LaunchReadyHandler launchReadyHandler) {
                launchReadyHandler.launch();
            }

            @Override
            public void onResult(VisaPaymentSummary visaPaymentSummary) {
                visaCheckoutClient.tokenize(visaPaymentSummary, (paymentMethodNonce, error) -> {
                    if (paymentMethodNonce != null) {
                        handlePaymentMethodNonceCreated(paymentMethodNonce);
                    } else {
                        handleError(error);
                    }
                });
            }
        });
    }

    private void handlePaymentMethodNonceCreated(PaymentMethodNonce paymentMethodNonce) {
        super.onPaymentMethodNonceCreated(paymentMethodNonce);

        VisaCheckoutFragmentDirections.ActionVisaCheckoutFragmentToDisplayNonceFragment action =
            VisaCheckoutFragmentDirections.actionVisaCheckoutFragmentToDisplayNonceFragment(paymentMethodNonce);

        NavHostFragment.findNavController(this).navigate(action);
    }
}
