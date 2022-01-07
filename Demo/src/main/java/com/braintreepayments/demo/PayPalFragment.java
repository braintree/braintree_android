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

import com.braintreepayments.api.BraintreeRequestCodes;
import com.braintreepayments.api.BrowserSwitchListener;
import com.braintreepayments.api.PaymentMethodNonce;
import com.braintreepayments.api.BrowserSwitchResult;
import com.braintreepayments.api.DataCollector;
import com.braintreepayments.api.PayPalClient;

import static com.braintreepayments.demo.PayPalRequestFactory.createPayPalCheckoutRequest;
import static com.braintreepayments.demo.PayPalRequestFactory.createPayPalVaultRequest;

public class PayPalFragment extends BaseFragment implements BrowserSwitchListener {

    private String deviceData;
    private PayPalClient payPalClient;
    private DataCollector dataCollector;

    private Button billingAgreementButton;
    private Button singlePaymentButton;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_paypal, container, false);
        billingAgreementButton = view.findViewById(R.id.paypal_billing_agreement_button);
        singlePaymentButton = view.findViewById(R.id.paypal_single_payment_button);

        billingAgreementButton.setOnClickListener(this::launchBillingAgreement);
        singlePaymentButton.setOnClickListener(this::launchSinglePayment);

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
                    dataCollector.collectDeviceData(activity, (deviceData, dataCollectorError) -> this.deviceData = deviceData);
                }
                if (isBillingAgreement) {
                    payPalClient.tokenizePayPalAccount(activity, createPayPalVaultRequest(activity), payPalError -> {
                        if (payPalError != null) {
                            handleError(payPalError);
                        }
                    });
                } else {
                    payPalClient.tokenizePayPalAccount(activity, createPayPalCheckoutRequest(activity, "1.00"), payPalError -> {
                        if (payPalError != null) {
                            handleError(payPalError);
                        }
                    });
                }
            });
        });
    }

    @Override
    public void onBrowserSwitchResult(BrowserSwitchResult result) {
        if (result.getRequestCode() == BraintreeRequestCodes.PAYPAL) {
            payPalClient.onBrowserSwitchResult(result, (paymentMethodNonce, error) -> {
                if (paymentMethodNonce != null) {
                    super.onPaymentMethodNonceCreated(paymentMethodNonce);

                    PayPalFragmentDirections.ActionPayPalFragmentToDisplayNonceFragment action =
                            PayPalFragmentDirections.actionPayPalFragmentToDisplayNonceFragment(paymentMethodNonce);
                    action.setDeviceData(deviceData);

                    NavHostFragment.findNavController(this).navigate(action);
                }
            });
        }
    }
}
