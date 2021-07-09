package com.braintreepayments.demo;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.navigation.NavDirections;
import androidx.navigation.fragment.NavHostFragment;

import com.braintreepayments.api.SamsungPayClient;
import com.braintreepayments.api.SamsungPayError;
import com.braintreepayments.api.SamsungPayException;
import com.braintreepayments.api.SamsungPayNonce;
import com.braintreepayments.api.SamsungPayListener;
import com.samsung.android.sdk.samsungpay.v2.SpaySdk;
import com.samsung.android.sdk.samsungpay.v2.payment.CardInfo;
import com.samsung.android.sdk.samsungpay.v2.payment.CustomSheetPaymentInfo;
import com.samsung.android.sdk.samsungpay.v2.payment.sheet.AddressControl;
import com.samsung.android.sdk.samsungpay.v2.payment.sheet.AmountBoxControl;
import com.samsung.android.sdk.samsungpay.v2.payment.sheet.AmountConstants;
import com.samsung.android.sdk.samsungpay.v2.payment.sheet.CustomSheet;
import com.samsung.android.sdk.samsungpay.v2.payment.sheet.SheetItemType;

public class SamsungPayFragment extends BaseFragment implements SamsungPayListener {

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
                String dialogMessage;
                if (error == null) {
                    dialogMessage = "Samsung Pay is not available";
                } else {
                    if (error instanceof SamsungPayException) {
                        SamsungPayException samsungPayError = (SamsungPayException) error;
                        @SamsungPayError int errorCode = samsungPayError.getErrorCode();
                        switch (errorCode) {
                            case SamsungPayError.SAMSUNG_PAY_APP_NEEDS_UPDATE:
                                dialogMessage = "Need to update Samsung Pay app...";
                                break;
                            case SamsungPayError.SAMSUNG_PAY_NO_SUPPORTED_CARDS_IN_WALLET:
                                dialogMessage = "No supported cards in wallet";
                                break;
                            case SamsungPayError.SAMSUNG_PAY_SETUP_NOT_COMPLETED:
                                dialogMessage = "Samsung Pay setup not completed...";
                                break;
                            case SamsungPayError.SAMSUNG_PAY_NOT_SUPPORTED:
                            case SamsungPayError.SAMSUNG_PAY_NOT_READY:
                            case SamsungPayError.SAMSUNG_PAY_ERROR_UNKNOWN:
                            default:
                                dialogMessage = "Samsung Pay is not supported";
                                break;
                        }
                    } else {
                        dialogMessage =
                                String.format("Samsung Pay is not available: %s", error.toString());
                    }
                }
                showDialog(dialogMessage);
            }
        });
    }

    public void launchSamsungPay(View v) {

        samsungPayClient.buildCustomSheetPaymentInfo((builder, error) -> {
            if (builder != null) {
                CustomSheetPaymentInfo paymentInfo = builder
                        .setAddressInPaymentSheet(CustomSheetPaymentInfo.AddressInPaymentSheet.NEED_BILLING_AND_SHIPPING)
                        .setCustomSheet(getCustomSheet())
                        .setOrderNumber("order-number")
                        .build();
                samsungPayClient.startSamsungPay(paymentInfo, SamsungPayFragment.this);
            } else {
                handleSamsungPayError(error);
            }
        });

    }

    public void handleSamsungPayError(Exception error) {
        if (error instanceof SamsungPayException) {
            SamsungPayException samsungPayException = (SamsungPayException) error;

            switch (samsungPayException.getErrorCode()) {
                case SpaySdk.ERROR_NO_NETWORK:
                    // handle accordingly
                    // ...
                    break;
            }

            showDialog("Samsung Pay failed with error code " + ((SamsungPayException) error).getErrorCode());
        }
    }

    private CustomSheet getCustomSheet() {
        CustomSheet sheet = new CustomSheet();

        final AddressControl billingAddressControl = new AddressControl("billingAddressId", SheetItemType.BILLING_ADDRESS);
        billingAddressControl.setAddressTitle("Billing Address");
        billingAddressControl.setSheetUpdatedListener((controlId, customSheet) -> {
            Log.d("billing sheet updated", controlId);
            samsungPayClient.updateCustomSheet(customSheet);
        });
        sheet.addControl(billingAddressControl);

        final AddressControl shippingAddressControl = new AddressControl("shippingAddressId", SheetItemType.SHIPPING_ADDRESS);
        shippingAddressControl.setAddressTitle("Shipping Address");
        shippingAddressControl.setSheetUpdatedListener((controlId, customSheet) -> {
            Log.d("shipping sheet updated", controlId);
            samsungPayClient.updateCustomSheet(customSheet);
        });
        sheet.addControl(shippingAddressControl);

        AmountBoxControl amountBoxControl = new AmountBoxControl("amountID", "USD");
        amountBoxControl.setAmountTotal(1.0, AmountConstants.FORMAT_TOTAL_PRICE_ONLY);
        sheet.addControl(amountBoxControl);

        return sheet;
    }

    @Override
    public void onSamsungPayStartError(@NonNull Exception error) {
        handleSamsungPayError(error);
    }

    @Override
    public void onSamsungPayStartSuccess(@NonNull SamsungPayNonce samsungPayNonce, CustomSheetPaymentInfo paymentInfo) {
        super.onPaymentMethodNonceCreated(samsungPayNonce);

        NavDirections action =
                SamsungPayFragmentDirections.actionSamsungPayFragmentToDisplayNonceFragment(samsungPayNonce);
        NavHostFragment.findNavController(this).navigate(action);
    }

    @Override
    public void onSamsungPayCardInfoUpdated(CardInfo cardInfo, CustomSheet customSheet) {
        AmountBoxControl amountBoxControl = (AmountBoxControl) customSheet.getSheetControl("amountID");
        amountBoxControl.setAmountTotal(1.0, AmountConstants.FORMAT_TOTAL_PRICE_ONLY);

        customSheet.updateControl(amountBoxControl);
        samsungPayClient.updateCustomSheet(customSheet);
    }
}