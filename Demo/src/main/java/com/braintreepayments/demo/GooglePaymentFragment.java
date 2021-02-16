package com.braintreepayments.demo;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.navigation.fragment.NavHostFragment;

import com.braintreepayments.api.Configuration;
import com.braintreepayments.api.ConfigurationCallback;
import com.braintreepayments.api.GooglePayCapabilities;
import com.braintreepayments.api.GooglePaymentClient;
import com.braintreepayments.api.GooglePaymentIsReadyToPayCallback;
import com.braintreepayments.api.GooglePaymentRequest;
import com.braintreepayments.api.GooglePaymentRequestPaymentCallback;
import com.braintreepayments.api.PaymentMethodNonce;
import com.google.android.gms.wallet.ShippingAddressRequirements;
import com.google.android.gms.wallet.TransactionInfo;
import com.google.android.gms.wallet.WalletConstants;

import static android.view.View.VISIBLE;

public class GooglePaymentFragment extends BaseFragment {

    private ImageButton mGooglePaymentButton;
    private GooglePaymentClient googlePaymentClient;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_google_payment, container, false);
        mGooglePaymentButton = view.findViewById(R.id.google_payment_button);
        mGooglePaymentButton.setOnClickListener(this::launchGooglePayment);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        getBraintreeClient(braintreeClient -> {

            googlePaymentClient = new GooglePaymentClient(braintreeClient);

            braintreeClient.getConfiguration(new ConfigurationCallback() {
                @Override
                public void onResult(@Nullable Configuration configuration, @Nullable Exception error) {

                    if (GooglePayCapabilities.isGooglePayEnabled(getActivity(), configuration.getGooglePayment())) {

                        googlePaymentClient.isReadyToPay(getActivity(), new GooglePaymentIsReadyToPayCallback() {
                            @Override
                            public void onResult(Boolean isReadyToPay, Exception e) {
                                if (isReadyToPay) {
                                    mGooglePaymentButton.setVisibility(VISIBLE);
                                } else {
                                    showDialog("Google Payments are not available. The following issues could be the cause:\n\n" +
                                            "No user is logged in to the device.\n\n" +
                                            "Google Play Services is missing or out of date.");
                                }
                            }
                        });
                    } else {
                        showDialog("Google Payments are not available. The following issues could be the cause:\n\n" +
                                "Google Payments are not enabled for the current merchant.\n\n" +
                                "Google Play Services is missing or out of date.");
                    }
                }
            });
        });
    }

    @Override
    public void onPaymentMethodNonceCreated(PaymentMethodNonce paymentMethodNonce) {
        super.onPaymentMethodNonceCreated(paymentMethodNonce);

        GooglePaymentFragmentDirections.ActionGooglePaymentFragmentToDisplayNonceFragment action =
            GooglePaymentFragmentDirections.actionGooglePaymentFragmentToDisplayNonceFragment(paymentMethodNonce);

        NavHostFragment.findNavController(this).navigate(action);
    }

    public void launchGooglePayment(View v) {
        FragmentActivity activity = getActivity();
        activity.setProgressBarIndeterminateVisibility(true);
        getBraintreeClient((braintreeClient) -> {
            // TODO: null check
            GooglePaymentRequest googlePaymentRequest = new GooglePaymentRequest()
                    .transactionInfo(TransactionInfo.newBuilder()
                            .setCurrencyCode(Settings.getGooglePaymentCurrency(activity))
                            .setTotalPrice("1.00")
                            .setTotalPriceStatus(WalletConstants.TOTAL_PRICE_STATUS_FINAL)
                            .build())
                    .allowPrepaidCards(Settings.areGooglePaymentPrepaidCardsAllowed(activity))
                    .billingAddressFormat(WalletConstants.BILLING_ADDRESS_FORMAT_FULL)
                    .billingAddressRequired(Settings.isGooglePaymentBillingAddressRequired(activity))
                    .emailRequired(Settings.isGooglePaymentEmailRequired(activity))
                    .phoneNumberRequired(Settings.isGooglePaymentPhoneNumberRequired(activity))
                    .shippingAddressRequired(Settings.isGooglePaymentShippingAddressRequired(activity))
                    .shippingAddressRequirements(ShippingAddressRequirements.newBuilder()
                            .addAllowedCountryCodes(Settings.getGooglePaymentAllowedCountriesForShipping(activity))
                            .build())
                    .googleMerchantId(Settings.getGooglePaymentMerchantId(activity));

            googlePaymentClient.requestPayment(getActivity(), googlePaymentRequest, new GooglePaymentRequestPaymentCallback() {
                @Override
                public void onResult(boolean b, Exception e) {
                    if (e != null) {
                        handleError(e);
                    }
                }
            });
        });
    }
}
