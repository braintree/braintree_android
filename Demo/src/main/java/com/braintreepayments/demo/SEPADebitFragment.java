package com.braintreepayments.demo;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

public class SEPADebitFragment extends BaseFragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_sepa_debit, container, false);
        Button button = view.findViewById(R.id.sepa_debit_button);
        button.setOnClickListener(this::launchSEPADebit);

        return view;
    }

    public void launchSEPADebit(View view) {

    }
}