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
import com.braintreepayments.api.VisaCheckoutButton;

public class VisaCheckoutFragment extends BaseFragment {

    private VisaCheckoutButton mVisaPaymentButton;


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

                // TODO: fix when visa client is integrated with new flattened core module
//        createVisaCheckoutProfile(new VisaCheckoutCreateProfileBuilderCallback() {
//            @Override
//            public void onResult(ProfileBuilder profileBuilder, Exception e) {
//                PurchaseInfoBuilder purchaseInfo = new PurchaseInfoBuilder(new BigDecimal("1.00"), PurchaseInfo.Currency.USD)
//                        .setDescription("Description");
//
//                mVisaPaymentButton.init(VisaCheckoutActivity.this, profileBuilder,
//                        purchaseInfo, new VisaCheckoutSdk.VisaCheckoutResultListener() {
//                            @Override
//                            public void onButtonClick(LaunchReadyHandler launchReadyHandler) {
//                                launchReadyHandler.launch();
//                            }
//
//                            @Override
//                            public void onResult(VisaPaymentSummary visaPaymentSummary) {
//                                tokenizeVisaCheckout(visaPaymentSummary, new VisaCheckoutTokenizeCallback() {
//                                    @Override
//                                    public void onResult(PaymentMethodNonce paymentMethodNonce, Exception e) {
//                                        handlePaymentMethodNonceCreated(paymentMethodNonce);
//                                    }
//                                });
//                            }
//                        });
//            }
//        });
//    }
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
