package com.braintreepayments.demo;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.braintreepayments.api.uicomponents.PayPalButton;
import com.google.android.material.button.MaterialButtonToggleGroup;


public class PaymentButtonsFragment extends BaseFragment {

    private PayPalButton payPalButton;
    private MaterialButtonToggleGroup toggleGroup;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_payment_buttons, container, false);
        payPalButton = view.findViewById(R.id.pp_payment_button);
        toggleGroup = view.findViewById(R.id.pp_button_toggle_group);

        toggleGroup.setSingleSelection(true);

        toggleGroup.addOnButtonCheckedListener(
                new MaterialButtonToggleGroup.OnButtonCheckedListener() {
                    @Override
                    public void onButtonChecked(MaterialButtonToggleGroup group, int checkedId, boolean isChecked) {
                        if (!isChecked) return; // ignore uncheck events
                        switch (checkedId) {
                            case R.id.button_pp_blue:
                                payPalButton.setButtonColor("blue");
                                break;
                            case R.id.button_pp_black:
                                payPalButton.setButtonColor("black");
                                break;
                            case R.id.button_pp_white:
                                payPalButton.setButtonColor("white");
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
