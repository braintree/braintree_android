package com.braintreepayments.demo;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.braintreepayments.InitializeFeatureClientsCallback;
import com.braintreepayments.api.BraintreeClient;
import com.braintreepayments.api.PayPalCheckoutRequest;
import com.braintreepayments.api.PayPalClient;
import com.braintreepayments.api.PayPalVaultRequest;
import com.braintreepayments.api.PreferredPaymentMethodsClient;
import com.braintreepayments.api.VenmoClient;
import com.braintreepayments.api.VenmoPaymentMethodUsage;
import com.braintreepayments.api.VenmoRequest;

import static com.braintreepayments.demo.PayPalRequestFactory.createPayPalCheckoutRequest;
import static com.braintreepayments.demo.PayPalRequestFactory.createPayPalVaultRequest;

public class PreferredPaymentMethodsFragment extends BaseFragment {

    private Button preferredPaymentMethodsButton;
    private TextView preferredPaymentMethodsTextView;
    private Button billingAgreementButton;
    private Button singlePaymentButton;
    private Button venmoButton;

    private BraintreeClient braintreeClient;
    private PayPalClient payPalClient;
    private VenmoClient venmoClient;
    private PreferredPaymentMethodsClient preferredPaymentMethodsClient;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_preferred_payment_methods, container, false);
        preferredPaymentMethodsTextView = view.findViewById(R.id.preferred_payment_methods_text_view);
        preferredPaymentMethodsButton = view.findViewById(R.id.preferred_payment_methods_button);
        billingAgreementButton = view.findViewById(R.id.paypal_billing_agreement_button);
        singlePaymentButton = view.findViewById(R.id.paypal_single_payment_button);
        venmoButton = view.findViewById(R.id.venmo_button);

        preferredPaymentMethodsButton.setOnClickListener(this::launchPreferredPaymentMethods);
        billingAgreementButton.setOnClickListener(this::launchSinglePayment);
        singlePaymentButton.setOnClickListener(this::launchBillingAgreement);
        venmoButton.setOnClickListener(this::launchVenmo);
        return view;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Context context = requireContext();
        braintreeClient = new BraintreeClient(context, new DemoAuthorizationProvider(context));

        payPalClient = new PayPalClient(this, braintreeClient);
        venmoClient = new VenmoClient(this, braintreeClient);
        preferredPaymentMethodsClient = new PreferredPaymentMethodsClient(braintreeClient);
    }

    public void launchPreferredPaymentMethods(View v) {
        preferredPaymentMethodsTextView.setText(getString(R.string.preferred_payment_methods_progress));
        preferredPaymentMethodsClient.fetchPreferredPaymentMethods(getActivity(), result -> {
            String formatString = "PayPal Preferred: %b\nVenmo Preferred: %b";
            preferredPaymentMethodsTextView.setText(
                    String.format(formatString, result.isPayPalPreferred(), result.isVenmoPreferred()));

            billingAgreementButton.setEnabled(result.isPayPalPreferred());
            singlePaymentButton.setEnabled(result.isPayPalPreferred());
            venmoButton.setEnabled(result.isVenmoPreferred());
        });
    }

    public void launchSinglePayment(View v) {
        getActivity().setProgressBarIndeterminateVisibility(true);

        PayPalCheckoutRequest payPalRequest = createPayPalCheckoutRequest(getActivity(), "1.00");
        payPalClient.tokenizePayPalAccount(getActivity(), payPalRequest);
    }

    public void launchBillingAgreement(View v) {
        getActivity().setProgressBarIndeterminateVisibility(true);

        PayPalVaultRequest payPalRequest = createPayPalVaultRequest(getActivity());
        payPalClient.tokenizePayPalAccount(getActivity(), payPalRequest);
    }

    public void launchVenmo(View v) {
        getActivity().setProgressBarIndeterminateVisibility(true);
        VenmoRequest venmoRequest = new VenmoRequest(VenmoPaymentMethodUsage.SINGLE_USE);
        venmoRequest.setProfileId(null);
        venmoRequest.setShouldVault(false);

        venmoClient.tokenizeVenmoAccount(getActivity(), venmoRequest, requestError -> {
            if (requestError != null) {
                handleError(requestError);
            }
        });
    }
}
