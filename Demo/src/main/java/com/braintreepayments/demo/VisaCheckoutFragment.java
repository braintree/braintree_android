package com.braintreepayments.demo;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.navigation.fragment.NavHostFragment;

import com.braintreepayments.api.core.PaymentMethodNonce;
import com.braintreepayments.api.visacheckout.VisaCheckoutClient;
import com.braintreepayments.api.visacheckout.VisaCheckoutProfileBuilderResult;
import com.braintreepayments.api.visacheckout.VisaCheckoutResult;
import com.visa.checkout.CheckoutButton;
import com.visa.checkout.Profile;
import com.visa.checkout.PurchaseInfo;
import com.visa.checkout.VisaCheckoutSdk;
import com.visa.checkout.VisaPaymentSummary;

import java.math.BigDecimal;

public class VisaCheckoutFragment extends BaseFragment {

    private CheckoutButton checkoutButton;
    private VisaCheckoutClient visaCheckoutClient;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_visa_checkout, container, false);
        checkoutButton = view.findViewById(R.id.visa_checkout_button);

        visaCheckoutClient = new VisaCheckoutClient(requireContext(), super.getAuthStringArg());
        visaCheckoutClient.createProfileBuilder((profileBuilderResult) -> {
            if (profileBuilderResult instanceof VisaCheckoutProfileBuilderResult.Failure) {
                handleError(((VisaCheckoutProfileBuilderResult.Failure) profileBuilderResult).getError());
            } else if (profileBuilderResult instanceof VisaCheckoutProfileBuilderResult.Success) {
                setupVisaCheckoutButton(((VisaCheckoutProfileBuilderResult.Success) profileBuilderResult).getProfileBuilder());
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
                visaCheckoutClient.tokenize(visaPaymentSummary, (visaCheckoutResult) -> {
                    if (visaCheckoutResult instanceof VisaCheckoutResult.Failure) {
                        handleError(((VisaCheckoutResult.Failure) visaCheckoutResult).getError());
                    } else if (visaCheckoutResult instanceof VisaCheckoutResult.Success) {
                        handlePaymentMethodNonceCreated(((VisaCheckoutResult.Success) visaCheckoutResult).getNonce());
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
