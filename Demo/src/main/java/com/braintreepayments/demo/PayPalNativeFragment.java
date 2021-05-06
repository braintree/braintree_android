package com.braintreepayments.demo;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.braintreepayments.api.PayPalNativeClient;

public class PayPalNativeFragment extends BaseFragment {

    PayPalNativeClient payPalNativeClient;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_paypal_native, container, false);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getBraintreeClient(braintreeClient -> {
            payPalNativeClient = new PayPalNativeClient(braintreeClient);
            startPayPalNative();
        });
    }

    private void startPayPalNative() {
        PayPalNativeCheckoutRequest nativeCheckoutRequest = new PayPalNativeCheckoutRequest();

        payPalNativeClient.tokenizePayPalAccount(getActivity(), nativeCheckoutRequest, error -> {
            if (error != null) {
                handleError(error);
            }
        });
    }
}