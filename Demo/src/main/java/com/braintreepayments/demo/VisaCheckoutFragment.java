package com.braintreepayments.demo;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.navigation.fragment.NavHostFragment;

import com.braintreepayments.api.BraintreeClient;
import com.braintreepayments.api.PaymentMethodNonce;
import com.braintreepayments.api.VisaCheckout;
import com.braintreepayments.api.VisaCheckoutButton;
import com.braintreepayments.api.VisaCheckoutCreateProfileBuilderCallback;
import com.braintreepayments.api.VisaCheckoutTokenizeCallback;
import com.visa.checkout.Profile;
import com.visa.checkout.PurchaseInfo;
import com.visa.checkout.VisaCheckoutSdk;
import com.visa.checkout.VisaPaymentSummary;

import java.math.BigDecimal;

public class VisaCheckoutFragment extends BaseFragment {

    private VisaCheckoutButton mVisaPaymentButton;
    private VisaCheckout visaCheckout;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_visa_checkout, container, false);
        mVisaPaymentButton = view.findViewById(R.id.visa_checkout_button);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        getBraintreeClient(new BraintreeClientCallback() {
            @Override
            public void onResult(@Nullable BraintreeClient braintreeClient) {

                visaCheckout = new VisaCheckout(braintreeClient);

                visaCheckout.createProfileBuilder(new VisaCheckoutCreateProfileBuilderCallback() {
                    @Override
                    public void onResult(Profile.ProfileBuilder profileBuilder, Exception e) {
                        PurchaseInfo.PurchaseInfoBuilder purchaseInfo = new PurchaseInfo.PurchaseInfoBuilder(new BigDecimal("1.00"), PurchaseInfo.Currency.USD)
                                .setDescription("Description");

                        mVisaPaymentButton.init(getActivity(), profileBuilder, purchaseInfo, new VisaCheckoutSdk.VisaCheckoutResultListener() {
                            @Override
                            public void onButtonClick(LaunchReadyHandler launchReadyHandler) {
                                launchReadyHandler.launch();
                            }

                            @Override
                            public void onResult(VisaPaymentSummary visaPaymentSummary) {
                                visaCheckout.tokenize(visaPaymentSummary, new VisaCheckoutTokenizeCallback() {
                                    @Override
                                    public void onResult(PaymentMethodNonce paymentMethodNonce, Exception e) {
                                        handlePaymentMethodNonceCreated(paymentMethodNonce);
                                    }
                                });
                            }
                        });
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
