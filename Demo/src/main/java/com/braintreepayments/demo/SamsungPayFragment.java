package com.braintreepayments.demo;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;

import com.braintreepayments.api.GooglePayClient;
import com.braintreepayments.api.SamsungPayClient;
import com.braintreepayments.api.SamsungPayRequest;
import com.samsung.android.sdk.samsungpay.v2.payment.CustomSheetPaymentInfo;
import com.samsung.android.sdk.samsungpay.v2.payment.sheet.AddressControl;
import com.samsung.android.sdk.samsungpay.v2.payment.sheet.AmountBoxControl;
import com.samsung.android.sdk.samsungpay.v2.payment.sheet.AmountConstants;
import com.samsung.android.sdk.samsungpay.v2.payment.sheet.CustomSheet;
import com.samsung.android.sdk.samsungpay.v2.payment.sheet.SheetItemType;
import com.samsung.android.sdk.samsungpay.v2.payment.sheet.SheetUpdatedListener;

public class SamsungPayFragment extends BaseFragment {

    private Button samsungPayButton;
    private SamsungPayClient samsungPayClient;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_samsung_pay, container, false);
        samsungPayButton = view.findViewById(R.id.samsung_pay_button);
        samsungPayButton.setOnClickListener(this::launchSamsungPay);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        getBraintreeClient(braintreeClient -> {
            samsungPayClient = new SamsungPayClient(braintreeClient);
            setupSamsungPayButton();
        });
    }

    private void setupSamsungPayButton() {
        samsungPayClient.isReadyToPay((isReadyToPay, error) -> {
            if (isReadyToPay) {
                samsungPayButton.setVisibility(View.VISIBLE);
            } else {
                String dialogMessage = "Samsung Pay is not available";
                if (error != null) {
                    dialogMessage += ": " + error.toString();
                }
                showDialog(dialogMessage);
            }
        });
    }

    public void launchSamsungPay(View v) {

        CustomSheetPaymentInfo paymentInfo = builder
                .setAddressInPaymentSheet(CustomSheetPaymentInfo.AddressInPaymentSheet.NEED_BILLING_AND_SHIPPING)
                .setCustomSheet(getCustomSheet())
                .setOrderNumber("order-number")
                .build();
    }

    private CustomSheet getCustomSheet() {
        CustomSheet sheet = new CustomSheet();

        final AddressControl billingAddressControl = new AddressControl("billingAddressId", SheetItemType.BILLING_ADDRESS);
        billingAddressControl.setAddressTitle("Billing Address");
        billingAddressControl.setSheetUpdatedListener(new SheetUpdatedListener() {
            @Override
            public void onResult(String controlId, final CustomSheet customSheet) {
                Log.d("billing sheet updated", controlId);

                mPaymentManager.updateSheet(customSheet);
            }
        });
        sheet.addControl(billingAddressControl);

        final AddressControl shippingAddressControl = new AddressControl("shippingAddressId", SheetItemType.SHIPPING_ADDRESS);
        shippingAddressControl.setAddressTitle("Shipping Address");
        shippingAddressControl.setSheetUpdatedListener(new SheetUpdatedListener() {
            @Override
            public void onResult(String controlId, final CustomSheet customSheet) {
                Log.d("shipping sheet updated", controlId);

                mPaymentManager.updateSheet(customSheet);
            }
        });
        sheet.addControl(shippingAddressControl);

        AmountBoxControl amountBoxControl = new AmountBoxControl("amountID", "USD");
        amountBoxControl.setAmountTotal(1.0, AmountConstants.FORMAT_TOTAL_PRICE_ONLY);
        sheet.addControl(amountBoxControl);

        return sheet;
    }
}