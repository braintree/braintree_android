package com.braintreepayments.demo;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.braintreepayments.api.BrowserSwitchResult;
import com.braintreepayments.api.DataCollector;
import com.braintreepayments.api.PayPalCheckoutRequest;
import com.braintreepayments.api.PayPalClient;
import com.braintreepayments.api.PayPalRequest;
import com.braintreepayments.api.PayPalVaultRequest;
import com.braintreepayments.api.PaymentMethodNonce;
import com.braintreepayments.api.PostalAddress;

import static com.braintreepayments.demo.PayPalRequestFactory.createPayPalCheckoutRequest;
import static com.braintreepayments.demo.PayPalRequestFactory.createPayPalVaultRequest;

public class PayPalFragment extends BaseFragment {

    private String mDeviceData;
    private PayPalClient payPalClient;
    private DataCollector dataCollector;

    private Button mBillingAgreementButton;
    private Button mSinglePaymentButton;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_paypal, container, false);
        mBillingAgreementButton = view.findViewById(R.id.paypal_billing_agreement_button);
        mSinglePaymentButton = view.findViewById(R.id.paypal_single_payment_button);

        mBillingAgreementButton.setOnClickListener(this::launchBillingAgreement);
        mSinglePaymentButton.setOnClickListener(this::launchSinglePayment);

        DemoViewModel viewModel = new ViewModelProvider(getActivity()).get(DemoViewModel.class);
        viewModel.getPayPalBrowserSwitchResult().observe(getViewLifecycleOwner(), this::handlePayPalBrowserSwitchResult);

        return view;
    }

    public void launchSinglePayment(View v) {
        launchPayPal(false);
    }

    public void launchBillingAgreement(View v) {
        launchPayPal(true);
    }

    private void launchPayPal(boolean isBillingAgreement) {
        FragmentActivity activity = getActivity();
        activity.setProgressBarIndeterminateVisibility(true);

        getBraintreeClient((braintreeClient) -> {
            if (braintreeClient == null) {
                return;
            }

            payPalClient = new PayPalClient(braintreeClient);
            dataCollector = new DataCollector(braintreeClient);

            braintreeClient.getConfiguration((configuration, configError) -> {
                if (getActivity().getIntent().getBooleanExtra(MainFragment.EXTRA_COLLECT_DEVICE_DATA, false)) {
                    dataCollector.collectDeviceData(activity, (deviceData, dataCollectorError) -> mDeviceData = deviceData);
                }
                if (isBillingAgreement) {
                    payPalClient.requestBillingAgreement(activity, createPayPalVaultRequest(activity), payPalError -> {
                        if (payPalError != null) {
                            handleError(payPalError);
                        }
                    });
                } else {
                    payPalClient.requestOneTimePayment(activity, createPayPalCheckoutRequest(activity, "1.00"), payPalError -> {
                        if (payPalError != null) {
                            handleError(payPalError);
                        }
                    });
                }
            });
        });
    }

    private void handlePayPalResult(PaymentMethodNonce paymentMethodNonce, Exception error) {
        if (paymentMethodNonce != null) {
            super.onPaymentMethodNonceCreated(paymentMethodNonce);

            PayPalFragmentDirections.ActionPayPalFragmentToDisplayNonceFragment action =
                PayPalFragmentDirections.actionPayPalFragmentToDisplayNonceFragment(paymentMethodNonce);
            action.setDeviceData(mDeviceData);

            NavHostFragment.findNavController(this).navigate(action);
        }
    }



    public void handlePayPalBrowserSwitchResult(BrowserSwitchResult browserSwitchResult) {
        if (browserSwitchResult != null) {
            payPalClient.onBrowserSwitchResult(browserSwitchResult, (payPalAccountNonce, error) -> handlePayPalResult(payPalAccountNonce, error));
        }
    }
}
