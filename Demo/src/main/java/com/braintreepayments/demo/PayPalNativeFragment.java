package com.braintreepayments.demo;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

public class PayPalNativeFragment extends Fragment {
    public Button launchPayPalNativeButton;
    public PayPalNativeFragment() {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_pay_pal_native, container, false);
        launchPayPalNativeButton = view.findViewById(R.id.paypal_native);
        launchPayPalNativeButton.setOnClickListener(this::launchPayPalNative);

        return view;
    }

    private void launchPayPalNative(View v) {
        
    }
}