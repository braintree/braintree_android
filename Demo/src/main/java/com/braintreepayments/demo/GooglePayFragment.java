package com.braintreepayments.demo;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.navigation.NavDirections;
import androidx.navigation.fragment.NavHostFragment;

import com.braintreepayments.api.GooglePayClient;
import com.braintreepayments.api.GooglePayLauncher;
import com.braintreepayments.api.GooglePayPaymentAuthRequest;
import com.braintreepayments.api.GooglePayReadinessResult;
import com.braintreepayments.api.GooglePayRequest;
import com.braintreepayments.api.GooglePayResult;
import com.braintreepayments.api.PaymentMethodNonce;
import com.braintreepayments.api.UserCanceledException;
import com.google.android.gms.wallet.ShippingAddressRequirements;
import com.google.android.gms.wallet.TransactionInfo;
import com.google.android.gms.wallet.WalletConstants;

public class GooglePayFragment extends BaseFragment {

    private ImageButton googlePayButton;
    private GooglePayClient googlePayClient;
    private GooglePayLauncher googlePayLauncher;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_google_pay, container, false);
        googlePayButton = view.findViewById(R.id.google_pay_button);
        googlePayButton.setOnClickListener(this::launchGooglePay);

        googlePayClient = new GooglePayClient(requireContext(), super.getAuthStringArg());
        googlePayLauncher = new GooglePayLauncher(this,
                paymentAuthResult -> googlePayClient.tokenize(paymentAuthResult,
                        (googlePayResult) -> {
                            if (googlePayResult instanceof GooglePayResult.Failure) {
                                handleError(((GooglePayResult.Failure) googlePayResult).getError());
                            } else if (googlePayResult instanceof GooglePayResult.Success){
                                handleGooglePayActivityResult(((GooglePayResult.Success) googlePayResult).getNonce());
                            } else if (googlePayResult instanceof GooglePayResult.Cancel) {
                                handleError(new UserCanceledException("User canceled Google Pay"));
                            }
                        }));

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        googlePayClient.isReadyToPay(requireActivity(), (googlePayReadinessResult) -> {
            if (googlePayReadinessResult instanceof GooglePayReadinessResult.ReadyToPay) {
                googlePayButton.setVisibility(View.VISIBLE);
            } else {
                showDialog(
                        "Google Payments are not available. The following issues could be the cause:\n\n" +
                                "No user is logged in to the device.\n\n" +
                                "Google Play Services is missing or out of date.");
            }
        });
    }

    @Override
    public void onPaymentMethodNonceCreated(PaymentMethodNonce paymentMethodNonce) {
        super.onPaymentMethodNonceCreated(paymentMethodNonce);

        GooglePayFragmentDirections.ActionGooglePayFragmentToDisplayNonceFragment action =
                GooglePayFragmentDirections.actionGooglePayFragmentToDisplayNonceFragment(
                        paymentMethodNonce);

        NavHostFragment.findNavController(this).navigate(action);
    }

    public void launchGooglePay(View v) {
        FragmentActivity activity = getActivity();
        activity.setProgressBarIndeterminateVisibility(true);

        GooglePayRequest googlePayRequest = new GooglePayRequest();
        googlePayRequest.setTransactionInfo(TransactionInfo.newBuilder()
                .setCurrencyCode(Settings.getGooglePayCurrency(activity))
                .setTotalPrice("1.00")
                .setTotalPriceStatus(WalletConstants.TOTAL_PRICE_STATUS_FINAL)
                .build());
        googlePayRequest.setTotalPriceLabel("Braintree Demo Payment");
        googlePayRequest.setAllowPrepaidCards(Settings.areGooglePayPrepaidCardsAllowed(activity));
        googlePayRequest.setBillingAddressFormat(WalletConstants.BILLING_ADDRESS_FORMAT_FULL);
        googlePayRequest.setBillingAddressRequired(
                Settings.isGooglePayBillingAddressRequired(activity));
        googlePayRequest.setEmailRequired(Settings.isGooglePayEmailRequired(activity));
        googlePayRequest.setPhoneNumberRequired(Settings.isGooglePayPhoneNumberRequired(activity));
        googlePayRequest.setShippingAddressRequired(
                Settings.isGooglePayShippingAddressRequired(activity));
        googlePayRequest.setShippingAddressRequirements(ShippingAddressRequirements.newBuilder()
                .addAllowedCountryCodes(Settings.getGooglePayAllowedCountriesForShipping(activity))
                .build());

        googlePayClient.createPaymentAuthRequest(googlePayRequest, (paymentAuthRequest) -> {
            if (paymentAuthRequest instanceof GooglePayPaymentAuthRequest.ReadyToLaunch) {
                googlePayLauncher.launch(
                        ((GooglePayPaymentAuthRequest.ReadyToLaunch) paymentAuthRequest).getRequestParams());
            } else if (paymentAuthRequest instanceof GooglePayPaymentAuthRequest.Failure) {
                handleError(((GooglePayPaymentAuthRequest.Failure) paymentAuthRequest).getError());
            }
        });
    }

    private void handleGooglePayActivityResult(PaymentMethodNonce paymentMethodNonce) {
        super.onPaymentMethodNonceCreated(paymentMethodNonce);

        NavDirections action =
                GooglePayFragmentDirections.actionGooglePayFragmentToDisplayNonceFragment(
                        paymentMethodNonce);
        NavHostFragment.findNavController(this).navigate(action);
    }
}
