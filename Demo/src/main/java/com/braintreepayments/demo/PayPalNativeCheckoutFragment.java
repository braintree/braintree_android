package com.braintreepayments.demo;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.braintreepayments.api.PayPalNativeCheckoutClient;
import com.braintreepayments.api.PayPalNativeCheckoutRequest;

public class PayPalNativeCheckoutFragment extends Fragment {
    public Button launchPayPalNativeCheckoutButton;
    public PayPalNativeCheckoutFragment() {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_paypal_native_checkout, container, false);
        launchPayPalNativeCheckoutButton = view.findViewById(R.id.paypal_native_checkout_launch);
        launchPayPalNativeCheckoutButton.setOnClickListener(this::launchPayPalNativeCheckout);

        return view;
    }

    private void launchPayPalNativeCheckout(View v) {
        PayPalNativeCheckoutClient client = new PayPalNativeCheckoutClient();
        PayPalNativeCheckoutRequest request = new PayPalNativeCheckoutRequest("100");
        client.tokenize(request);
    }
}