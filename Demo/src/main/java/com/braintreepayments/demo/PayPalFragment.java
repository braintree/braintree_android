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
import com.braintreepayments.api.PayPalClient;
import com.braintreepayments.api.PayPalRequest;
import com.braintreepayments.api.PaymentMethodNonce;
import com.braintreepayments.api.PostalAddress;

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
                    payPalClient.requestBillingAgreement(activity, getPayPalRequest(null), payPalError -> {
                        if (payPalError != null) {
                            handleError(payPalError);
                        }
                    });
                } else {
                    payPalClient.requestOneTimePayment(activity, getPayPalRequest("1.00"), payPalError -> {
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

    private PayPalRequest getPayPalRequest(@Nullable String amount) {
        FragmentActivity activity = getActivity();
        PayPalRequest request = new PayPalRequest()
               .amount(amount);

        request.displayName(Settings.getPayPalDisplayName(activity));

        String landingPageType = Settings.getPayPalLandingPageType(activity);
        if (getString(R.string.paypal_landing_page_type_billing).equals(landingPageType)) {
            request.landingPageType(PayPalRequest.LANDING_PAGE_TYPE_BILLING);
        } else if (getString(R.string.paypal_landing_page_type_login).equals(landingPageType)) {
            request.landingPageType(PayPalRequest.LANDING_PAGE_TYPE_LOGIN);
        }

        String intentType = Settings.getPayPalIntentType(activity);
        if (intentType.equals(getString(R.string.paypal_intent_authorize))) {
            request.intent(PayPalRequest.INTENT_AUTHORIZE);
        } else if (intentType.equals(getString(R.string.paypal_intent_order))) {
            request.intent(PayPalRequest.INTENT_ORDER);
        } else if (intentType.equals(getString(R.string.paypal_intent_sale))) {
            request.intent(PayPalRequest.INTENT_SALE);
        }

        if (Settings.isPayPalUseractionCommitEnabled(activity)) {
            request.userAction(PayPalRequest.USER_ACTION_COMMIT);
        }

        if (Settings.isPayPalCreditOffered(activity)) {
            request.offerCredit(true);
        }

        if (Settings.usePayPalAddressOverride(activity)) {
            request.shippingAddressOverride(new PostalAddress()
                    .recipientName("Brian Tree")
                    .streetAddress("123 Fake Street")
                    .extendedAddress("Floor A")
                    .locality("San Francisco")
                    .region("CA")
                    .countryCodeAlpha2("US")
            );
        }

        return request;
    }

    public void handlePayPalBrowserSwitchResult(BrowserSwitchResult browserSwitchResult) {
        if (browserSwitchResult != null) {
            payPalClient.onBrowserSwitchResult(browserSwitchResult, (payPalAccountNonce, error) -> handlePayPalResult(payPalAccountNonce, error));
        }
    }
}
