package com.braintreepayments.demo;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.navigation.NavDirections;
import androidx.navigation.fragment.NavHostFragment;

import com.braintreepayments.api.PaymentMethodNonce;

/**
 * A simple Android Fragment subclass.
 * create an instance of this fragment.
 */
public class DisplayNonceFragment extends Fragment {

    private String amount;
    private PaymentMethodNonce nonce;

    private TextView nonceString;
    private TextView nonceDetails;
    private TextView deviceData;

    private Button createTransactionButton;

    public DisplayNonceFragment() { }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_display_nonce, container, false);
        nonceString = view.findViewById(R.id.nonce);
        nonceDetails = view.findViewById(R.id.nonce_details);
        deviceData = view.findViewById(R.id.device_data);
        createTransactionButton = view.findViewById(R.id.create_transaction);

        createTransactionButton.setOnClickListener(this::createTransaction);

        DisplayNonceFragmentArgs args = DisplayNonceFragmentArgs.fromBundle(getArguments());
        displayNonce(args.getPaymentMethodNonce(), args.getDeviceData(), args.getTransactionAmount());
        return view;
    }

    private void displayNonce(PaymentMethodNonce paymentMethodNonce, String deviceData, String amount) {
        this.amount = amount;
        nonce = paymentMethodNonce;
        nonceString.setText(getString(R.string.nonce_placeholder, nonce.getString()));

        String details = PaymentMethodNonceFormatter.convertToString(nonce);
        nonceDetails.setText(details);

        this.deviceData.setText(getString(R.string.device_data_placeholder, deviceData));
    }

    public void createTransaction(View v) {
        createTransactionButton.setEnabled(false);

        DisplayNonceFragmentDirections.ActionDisplayNonceFragmentToCreateTransactionFragment action =
            DisplayNonceFragmentDirections.actionDisplayNonceFragmentToCreateTransactionFragment(nonce);
        action.setTransactionAmount(amount);
        NavHostFragment.findNavController(this).navigate(action);
    }
}