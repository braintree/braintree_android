package com.braintreepayments.demo;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import com.braintreepayments.InitializeFeatureClientsCallback;
import com.braintreepayments.api.PayPalClient;
import com.braintreepayments.api.PayPalRequest;
import com.braintreepayments.api.PostalAddress;
import com.braintreepayments.api.PreferredPaymentMethodsClient;
import com.braintreepayments.api.VenmoClient;
import com.braintreepayments.api.VenmoRequest;

public class PreferredPaymentMethodsFragment extends BaseFragment {

    private Button mPreferredPaymentMethodsButton;
    private TextView mPreferredPaymentMethodsTextView;
    private Button mBillingAgreementButton;
    private Button mSinglePaymentButton;
    private Button mVenmoButton;

    private PayPalClient payPalClient;
    private VenmoClient venmoClient;
    private PreferredPaymentMethodsClient preferredPaymentMethodsClient;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_preferred_payment_methods, container, false);
        mPreferredPaymentMethodsTextView = view.findViewById(R.id.preferred_payment_methods_text_view);
        mPreferredPaymentMethodsButton = view.findViewById(R.id.preferred_payment_methods_button);
        mBillingAgreementButton = view.findViewById(R.id.paypal_billing_agreement_button);
        mSinglePaymentButton = view.findViewById(R.id.paypal_single_payment_button);
        mVenmoButton = view.findViewById(R.id.venmo_button);

        mPreferredPaymentMethodsButton.setOnClickListener(this::launchPreferredPaymentMethods);
        mBillingAgreementButton.setOnClickListener(this::launchSinglePayment);
        mSinglePaymentButton.setOnClickListener(this::launchBillingAgreement);
        mVenmoButton.setOnClickListener(this::launchVenmo);
        return view;
    }

    private void initializeFeatureClients(InitializeFeatureClientsCallback callback) {
        getBraintreeClient(braintreeClient -> {
            payPalClient = new PayPalClient(braintreeClient);
            venmoClient = new VenmoClient(braintreeClient);
            preferredPaymentMethodsClient = new PreferredPaymentMethodsClient(braintreeClient);
            callback.onResult(null);
        });
    }

    public void launchPreferredPaymentMethods(View v) {
        initializeFeatureClients(error -> {
            mPreferredPaymentMethodsTextView.setText(getString(R.string.preferred_payment_methods_progress));
            preferredPaymentMethodsClient.fetchPreferredPaymentMethods(getActivity(), result -> {
                String formatString = "PayPal Preferred: %b\nVenmo Preferred: %b";
                mPreferredPaymentMethodsTextView.setText(
                        String.format(formatString, result.isPayPalPreferred(), result.isVenmoPreferred()));

                mBillingAgreementButton.setEnabled(result.isPayPalPreferred());
                mSinglePaymentButton.setEnabled(result.isPayPalPreferred());
                mVenmoButton.setEnabled(result.isVenmoPreferred());
            });
        });
    }

    public void launchSinglePayment(View v) {
        getActivity().setProgressBarIndeterminateVisibility(true);

        initializeFeatureClients(initError -> {
            PayPalRequest payPalRequest = getPayPalRequest("1.00");
            payPalClient.requestOneTimePayment(getActivity(), payPalRequest, requestError -> {
                if (requestError != null) {
                    handleError(requestError);
                }
            });
        });
    }

    public void launchBillingAgreement(View v) {
        getActivity().setProgressBarIndeterminateVisibility(true);

        initializeFeatureClients(initError -> {
            PayPalRequest payPalRequest = getPayPalRequest(null);
            payPalClient.requestBillingAgreement(getActivity(), payPalRequest, requestError -> {
                if (requestError != null) {
                    handleError(requestError);
                }
            });
        });
    }

    public void launchVenmo(View v) {
        getActivity().setProgressBarIndeterminateVisibility(true);
        initializeFeatureClients(initError -> {
            VenmoRequest venmoRequest = new VenmoRequest()
                    .profileId(null)
                    .shouldVault(false);
            venmoClient.tokenizeVenmoAccount(getActivity(), venmoRequest, requestError -> {
                if (requestError != null) {
                    handleError(requestError);
                }
            });
        });
    }

    // Launching a payment method from the home screen creates a new BraintreeFragment. To maintain sessionID within a
    // fragment, we opted to add PayPal within the PreferredPaymentMethodsActivity.
    private PayPalRequest getPayPalRequest(@Nullable String amount) {
        PayPalRequest request = new PayPalRequest()
                .amount(amount);

        FragmentActivity activity = getActivity();
        request.setDisplayName(Settings.getPayPalDisplayName(activity));

        String landingPageType = Settings.getPayPalLandingPageType(activity);
        if (getString(R.string.paypal_landing_page_type_billing).equals(landingPageType)) {
            request.setLandingPageType(PayPalRequest.LANDING_PAGE_TYPE_BILLING);
        } else if (getString(R.string.paypal_landing_page_type_login).equals(landingPageType)) {
            request.setLandingPageType(PayPalRequest.LANDING_PAGE_TYPE_LOGIN);
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
            request.setUserAction(PayPalRequest.USER_ACTION_COMMIT);
        }

        if (Settings.isPayPalCreditOffered(activity)) {
            request.setOfferCredit(true);
        }

        if (Settings.usePayPalAddressOverride(activity)) {
            request.setShippingAddressOverride(new PostalAddress()
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
}
