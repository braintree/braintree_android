package com.braintreepayments.demo;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.braintreepayments.api.BrowserSwitchResult;
import com.braintreepayments.api.DataCollector;
import com.braintreepayments.api.PayPalClient;
import com.braintreepayments.api.PayPalNativeCheckoutRequest;
import com.braintreepayments.api.PayPalNativeClient;
import com.braintreepayments.api.PaymentMethodNonce;

import static com.braintreepayments.demo.PayPalRequestFactory.createPayPalCheckoutRequest;
import static com.braintreepayments.demo.PayPalRequestFactory.createPayPalNativeVaultRequest;

public class PayPalNativeFragment extends BaseFragment {

    private PayPalNativeClient payPalNativeClient;

    private String deviceData;
    private DataCollector dataCollector;


    private Button billingAgreementButton;
    private Button singlePaymentButton;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_paypal_native, container, false);
        billingAgreementButton = view.findViewById(R.id.paypal_native_billing_agreement_button);
        singlePaymentButton = view.findViewById(R.id.paypal_native_single_payment_button);

        billingAgreementButton.setOnClickListener(this::launchBillingAgreement);
        singlePaymentButton.setOnClickListener(this::launchSinglePayment);

        DemoViewModel viewModel = new ViewModelProvider(getActivity()).get(DemoViewModel.class);
        viewModel.getPayPalBrowserSwitchResult().observe(getViewLifecycleOwner(), this::handlePayPalBrowserSwitchResult);
        return view;
    }

    public void launchSinglePayment(View v) {
        getBraintreeClient(braintreeClient -> {
            payPalNativeClient = new PayPalNativeClient(braintreeClient);
            startPayPalNative();
        });
    }

    public void launchBillingAgreement(View v) {
        FragmentActivity activity = getActivity();
        activity.setProgressBarIndeterminateVisibility(true);

        getBraintreeClient((braintreeClient) -> {
            if (braintreeClient == null) {
                return;
            }

            payPalNativeClient = new PayPalNativeClient(braintreeClient);
            dataCollector = new DataCollector(braintreeClient);

            braintreeClient.getConfiguration((configuration, configError) -> {
                if (getActivity().getIntent().getBooleanExtra(MainFragment.EXTRA_COLLECT_DEVICE_DATA, false)) {
                    dataCollector.collectDeviceData(activity, (deviceData, dataCollectorError) -> this.deviceData = deviceData);
                }
                payPalNativeClient.tokenizePayPalAccount(activity, createPayPalNativeVaultRequest(activity), payPalError -> {
                    if (payPalError != null) {
                        handleError(payPalError);
                    }
                });
            });
        });
    }

    public void handlePayPalBrowserSwitchResult(BrowserSwitchResult browserSwitchResult) {
        if (browserSwitchResult != null) {
            payPalNativeClient.onActivityResumed(browserSwitchResult, (payPalAccountNonce, error) -> handlePayPalResult(payPalAccountNonce, error));
        }
    }

    private void handlePayPalResult(PaymentMethodNonce paymentMethodNonce, Exception error) {
        if (paymentMethodNonce != null) {
            super.onPaymentMethodNonceCreated(paymentMethodNonce);

            PayPalNativeFragmentDirections.ActionPayPalNativeFragmentToDisplayNonceFragment action =
                    PayPalNativeFragmentDirections.actionPayPalNativeFragmentToDisplayNonceFragment(paymentMethodNonce);
            action.setDeviceData(deviceData);

            NavHostFragment.findNavController(this).navigate(action);
        }
    }

    private void startPayPalNative() {
        PayPalNativeCheckoutRequest nativeCheckoutRequest = new PayPalNativeCheckoutRequest("1.0");

        payPalNativeClient.tokenizePayPalAccount(getActivity(), nativeCheckoutRequest, error -> {
            if (error != null) {
                handleError(error);
            }
        });
    }
}