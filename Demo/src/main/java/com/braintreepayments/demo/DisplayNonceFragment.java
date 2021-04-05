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
import com.braintreepayments.api.UntypedPaymentMethodNonce;

/**
 * A simple Android Fragment subclass.
 * create an instance of this fragment.
 */
public class DisplayNonceFragment extends Fragment {

    private PaymentMethodNonce mNonce;

    private TextView mNonceString;
    private TextView mNonceDetails;
    private TextView mDeviceData;

    private Button mCreateTransactionButton;

    public DisplayNonceFragment() { }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_display_nonce, container, false);
        mNonceString = view.findViewById(R.id.nonce);
        mNonceDetails = view.findViewById(R.id.nonce_details);
        mDeviceData = view.findViewById(R.id.device_data);
        mCreateTransactionButton = view.findViewById(R.id.create_transaction);

        mCreateTransactionButton.setOnClickListener(this::createTransaction);

        DisplayNonceFragmentArgs args = DisplayNonceFragmentArgs.fromBundle(getArguments());
        displayNonce(args.getPaymentMethodNonce(), args.getDeviceData());
        return view;
    }

    private void displayNonce(PaymentMethodNonce paymentMethodNonce, String deviceData) {
        mNonce = paymentMethodNonce;
        mNonceString.setText(getString(R.string.nonce_placeholder, mNonce.getNonce()));

        String details = PaymentMethodNonceFormatter.convertToString(mNonce);
        mNonceDetails.setText(details);

        mDeviceData.setText(getString(R.string.device_data_placeholder, deviceData));
    }

    public void createTransaction(View v) {
        mCreateTransactionButton.setEnabled(false);

        NavDirections action = DisplayNonceFragmentDirections.actionDisplayNonceFragmentToCreateTransactionFragment(mNonce);
        NavHostFragment.findNavController(this).navigate(action);
    }
}