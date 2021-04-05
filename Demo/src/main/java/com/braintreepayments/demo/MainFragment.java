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
import com.braintreepayments.api.UntypedPaymentMethodNonce;

public class MainFragment extends BaseFragment {

    static final String EXTRA_PAYMENT_RESULT = "payment_result";
    static final String EXTRA_DEVICE_DATA = "device_data";
    static final String EXTRA_COLLECT_DEVICE_DATA = "collect_device_data";

    private static final String KEY_NONCE = "nonce";

    private PaymentMethodNonce mNonce;

    private Button mGooglePayButton;
    private Button mCardsButton;
    private Button mPayPalButton;
    private Button mVenmoButton;
    private Button mVisaCheckoutButton;
    private Button mLocalPaymentsButton;
    private Button mPreferredPaymentMethods;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_main, container, false);

        mGooglePayButton = view.findViewById(R.id.google_pay);
        mCardsButton = view.findViewById(R.id.card);
        mPayPalButton = view.findViewById(R.id.paypal);
        mVenmoButton = view.findViewById(R.id.venmo);
        mVisaCheckoutButton = view.findViewById(R.id.visa_checkout);
        mLocalPaymentsButton = view.findViewById(R.id.local_payment);
        mPreferredPaymentMethods = view.findViewById(R.id.preferred_payment_methods);

        mCardsButton.setOnClickListener(this::launchCards);
        mPayPalButton.setOnClickListener(this::launchPayPal);
        mLocalPaymentsButton.setOnClickListener(this::launchLocalPayment);
        mGooglePayButton.setOnClickListener(this::launchGooglePay);
        mVisaCheckoutButton.setOnClickListener(this::launchVisaCheckout);
        mVenmoButton.setOnClickListener(this::launchVenmo);
        mPreferredPaymentMethods.setOnClickListener(this::launchPreferredPaymentMethods);

        return view;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(KEY_NONCE)) {
                mNonce = savedInstanceState.getParcelable(KEY_NONCE);
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mNonce != null) {
            outState.putParcelable(KEY_NONCE, mNonce);
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
