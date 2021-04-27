package com.braintreepayments.demo;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.navigation.NavDirections;
import androidx.navigation.Navigation;

import com.braintreepayments.api.PaymentMethodNonce;

public class MainFragment extends BaseFragment {

    static final String EXTRA_PAYMENT_RESULT = "payment_result";
    static final String EXTRA_DEVICE_DATA = "device_data";
    static final String EXTRA_COLLECT_DEVICE_DATA = "collect_device_data";

    private static final String KEY_NONCE = "nonce";

    private PaymentMethodNonce nonce;

    private Button googlePayButton;
    private Button cardsButton;
    private Button payPalButton;
    private Button venmoButton;
    private Button visaCheckoutButton;
    private Button localPaymentsButton;
    private Button preferredPaymentMethods;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_main, container, false);

        googlePayButton = view.findViewById(R.id.google_pay);
        cardsButton = view.findViewById(R.id.card);
        payPalButton = view.findViewById(R.id.paypal);
        venmoButton = view.findViewById(R.id.venmo);
        visaCheckoutButton = view.findViewById(R.id.visa_checkout);
        localPaymentsButton = view.findViewById(R.id.local_payment);
        preferredPaymentMethods = view.findViewById(R.id.preferred_payment_methods);

        cardsButton.setOnClickListener(this::launchCards);
        payPalButton.setOnClickListener(this::launchPayPal);
        localPaymentsButton.setOnClickListener(this::launchLocalPayment);
        googlePayButton.setOnClickListener(this::launchGooglePay);
        visaCheckoutButton.setOnClickListener(this::launchVisaCheckout);
        venmoButton.setOnClickListener(this::launchVenmo);
        preferredPaymentMethods.setOnClickListener(this::launchPreferredPaymentMethods);

        return view;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(KEY_NONCE)) {
                nonce = savedInstanceState.getParcelable(KEY_NONCE);
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (nonce != null) {
            outState.putParcelable(KEY_NONCE, nonce);
        }
    }

    public void launchGooglePay(View v) {
        NavDirections action =
                MainFragmentDirections.actionMainFragmentToGooglePayFragment();
        Navigation.findNavController(v).navigate(action);
    }

    public void launchCards(View v) {
        Bundle args = new Bundle();
        args.putBoolean(EXTRA_COLLECT_DEVICE_DATA, Settings.shouldCollectDeviceData(getActivity()));

        MainFragmentDirections.ActionMainFragmentToCardFragment action =
                MainFragmentDirections.actionMainFragmentToCardFragment();
        action.setShouldCollectDeviceData(Settings.shouldCollectDeviceData(getActivity()));

        Navigation.findNavController(v).navigate(action);
    }

    public void launchPayPal(View v) {
        MainFragmentDirections.ActionMainFragmentToPayPalFragment action =
                MainFragmentDirections.actionMainFragmentToPayPalFragment();
        action.setShouldCollectDeviceData(Settings.shouldCollectDeviceData(getActivity()));

        Navigation.findNavController(v).navigate(action);
    }

    public void launchVenmo(View v) {
        NavDirections action = MainFragmentDirections.actionMainFragmentToVenmoFragment();
        Navigation.findNavController(v).navigate(action);
    }

    public void launchVisaCheckout(View v) {
        NavDirections action = MainFragmentDirections.actionMainFragmentToVisaCheckoutFragment();
        Navigation.findNavController(v).navigate(action);
    }

    public void launchLocalPayment(View v) {
        NavDirections action =
                MainFragmentDirections.actionMainFragmentToLocalPaymentFragment();
        Navigation.findNavController(v).navigate(action);
    }

    public void launchPreferredPaymentMethods(View v) {
        NavDirections action =
                MainFragmentDirections.actionMainFragmentToPreferredPaymentMethodsFragment();
        Navigation.findNavController(v).navigate(action);
    }
}
