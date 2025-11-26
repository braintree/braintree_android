package com.braintreepayments.demo;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.braintreepayments.api.uicomponents.PayPalButton;
import com.braintreepayments.api.uicomponents.PayPalButtonColor;
import com.braintreepayments.api.uicomponents.VenmoButton;
import com.braintreepayments.api.uicomponents.VenmoButtonColor;
import com.google.android.material.button.MaterialButtonToggleGroup;

public class PaymentButtonsFragment extends BaseFragment {

    private PayPalButton payPalButton;
    private MaterialButtonToggleGroup payPalToggleGroup;
    private VenmoButton venmoButton;
    private MaterialButtonToggleGroup venmoToggleGroup;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_payment_buttons, container, false);

        payPalButton = view.findViewById(R.id.pp_payment_button);
        payPalToggleGroup = view.findViewById(R.id.pp_button_toggle_group);

        payPalToggleGroup.addOnButtonCheckedListener(
                new MaterialButtonToggleGroup.OnButtonCheckedListener() {
                    @Override
                    public void onButtonChecked(MaterialButtonToggleGroup group, int checkedId, boolean isChecked) {
                        if (!isChecked) return;
                        switch (checkedId) {
                            case R.id.button_pp_blue:
                                payPalButton.setButtonColor(PayPalButtonColor.BLUE);
                                break;
                            case R.id.button_pp_black:
                                payPalButton.setButtonColor(PayPalButtonColor.BLACK);
                                break;
                            case R.id.button_pp_white:
                                payPalButton.setButtonColor(PayPalButtonColor.WHITE);
                                break;
                        }
                    }
                }
        );

        venmoButton = view.findViewById(R.id.venmo_payment_button);
        venmoToggleGroup = view.findViewById(R.id.venmo_button_toggle_group);

        venmoToggleGroup.addOnButtonCheckedListener(
                new MaterialButtonToggleGroup.OnButtonCheckedListener() {
                    @Override
                    public void onButtonChecked(MaterialButtonToggleGroup group, int checkedId, boolean isChecked) {
                        if (!isChecked) return;
                        switch (checkedId) {
                            case R.id.button_venmo_blue:
                                venmoButton.setButtonColor(VenmoButtonColor.BLUE);
                                break;
                            case R.id.button_venmo_black:
                                venmoButton.setButtonColor(VenmoButtonColor.BLACK);
                                break;
                            case R.id.button_venmo_white:
                                venmoButton.setButtonColor(VenmoButtonColor.WHITE);
                                break;
                        }
                    }
                }
        );

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

}
