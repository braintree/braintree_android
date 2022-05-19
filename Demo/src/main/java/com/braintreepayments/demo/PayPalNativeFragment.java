package com.braintreepayments.demo;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.braintreepayments.api.PayPalNativeCheckoutClient;
import com.braintreepayments.api.PayPalNativeCheckoutRequest;

public class PayPalNativeFragment extends Fragment {
    public Button launchPayPalNativeButton;
    public PayPalNativeFragment() {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_pay_pal_native, container, false);
        launchPayPalNativeButton = view.findViewById(R.id.paypal_native_launch);
        launchPayPalNativeButton.setOnClickListener(this::launchPayPalNative);

        return view;
    }

    private void launchPayPalNative(View v) {
        PayPalNativeCheckoutClient client = new PayPalNativeCheckoutClient();
        PayPalNativeCheckoutRequest request = new PayPalNativeCheckoutRequest("100");
        client.tokenize(request);
    }
}