package com.braintreepayments.demo;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.braintreepayments.api.BrowserSwitchResult;
import com.braintreepayments.api.DataCollector;
import com.braintreepayments.api.PayPalNativeCheckoutRequest;
import com.braintreepayments.api.PayPalNativeClient;
import com.braintreepayments.api.PayPalNativeVaultRequest;
import com.braintreepayments.api.PayPalRequest;
import com.braintreepayments.api.PaymentMethodNonce;

import static com.braintreepayments.demo.PayPalRequestFactory.createPayPalNativeVaultRequest;
import static com.braintreepayments.demo.PayPalRequestFactory.createPayPalNativeCheckoutRequest;


public class PayPalNativeFragment extends BaseFragment {

    private PayPalNativeClient payPalNativeClient;

    private String deviceData;
    private DataCollector dataCollector;


    private Button vaultButton;
    private Button checkoutButton;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
       View view = inflater.inflate(R.layout.fragment_paypal_native, container, false);
        vaultButton = view.findViewById(R.id.paypal_native_vault_button);
        checkoutButton = view.findViewById(R.id.paypal_native_checkout_button);

        vaultButton.setOnClickListener(this::launchVault);
        checkoutButton.setOnClickListener(this::launchSinglePayment);
        return view;
    }

    private void launchSinglePayment(View v) {
        launchNativeCheckout(createPayPalNativeCheckoutRequest(getActivity(), "1.0"));
    }

    private void launchVault(View v) {
        launchNativeCheckout(createPayPalNativeVaultRequest(getActivity()));
    }

    private void launchNativeCheckout(PayPalRequest request) {
        getBraintreeClient(braintreeClient -> {
            payPalNativeClient = new PayPalNativeClient(braintreeClient);
            startPayPalNative(request);
        });
    }

    private void startPayPalNative(PayPalRequest request) {
        payPalNativeClient.tokenizePayPalAccount(getActivity(), request, (paymentMethodNonce, error) -> {
            if (error != null) {
                handleError(error);
            }
            if (paymentMethodNonce != null) {
                super.onPaymentMethodNonceCreated(paymentMethodNonce);

                PayPalNativeFragmentDirections.ActionPayPalNativeFragmentToDisplayNonceFragment action =
                        PayPalNativeFragmentDirections.actionPayPalNativeFragmentToDisplayNonceFragment(paymentMethodNonce);
                action.setDeviceData(deviceData);

                NavHostFragment.findNavController(this).navigate(action);
            }
        });
    }
}
