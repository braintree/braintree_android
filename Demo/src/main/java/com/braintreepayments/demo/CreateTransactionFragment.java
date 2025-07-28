package com.braintreepayments.demo;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.braintreepayments.api.core.PaymentMethodNonce;
import com.braintreepayments.demo.models.Transaction;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CreateTransactionFragment extends Fragment {

    public static final String EXTRA_PAYMENT_METHOD_NONCE = "nonce";

    private ProgressBar loadingSpinner;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_create_transaction, container, false);
        loadingSpinner = view.findViewById(R.id.loading_spinner);

        AppCompatActivity activity = (AppCompatActivity) getActivity();
        activity.setTitle(R.string.processing_transaction);

        ActionBar actionBar = activity.getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        CreateTransactionFragmentArgs args = CreateTransactionFragmentArgs.fromBundle(getArguments());
        sendNonceToServer(args.getPaymentMethodNonce(), args.getTransactionAmount());

        return view;
    }

    private void sendNonceToServer(PaymentMethodNonce nonce, String amount) {
        Callback<Transaction> callback = new Callback<>() {
            @Override
            public void onResponse(Call<Transaction> call, Response<Transaction> response) {
                if (response.isSuccessful()) {
                    Transaction transaction = response.body();
                    if (transaction.getMessage() != null &&
                            transaction.getMessage().startsWith("created")) {
                        setStatus(R.string.transaction_complete);
                        setMessage(transaction.getMessage());
                    } else {
                        setStatus(R.string.transaction_failed);
                        if (TextUtils.isEmpty(transaction.getMessage())) {
                            setMessage("Server response was empty or malformed");
                        } else {
                            setMessage(transaction.getMessage());
                        }
                    }
                } else {
                    setStatus(R.string.transaction_failed);
                    if (response.body() != null) {
                        setMessage("Unable to create a transaction. Response Code: " +
                                response.code() + " Response body: " + response.body());
                    } else {
                        setMessage("Unable to create a transaction - no response body");
                    }
                }
            }

            @Override
            public void onFailure(Call<Transaction> call, Throwable throwable) {
                setStatus(R.string.transaction_failed);
                setMessage("Unable to create a transaction. Error: " + throwable.getMessage());
            }
        };

        String nonceString = nonce.getString();
        AppCompatActivity activity = (AppCompatActivity) getActivity();

        if (Settings.isThreeDSecureEnabled(activity) && Settings.isThreeDSecureRequired(activity)) {
            DemoApplication
                    .getApiClient(activity)
                    .createTransaction(
                            nonceString,
                            amount,
                            Settings.getThreeDSecureMerchantAccountId(activity),
                            true
                    )
                    .enqueue(callback);
        } else if (Settings.isThreeDSecureEnabled(activity)) {
            DemoApplication
                    .getApiClient(activity)
                    .createTransaction(
                            nonceString,
                            amount,
                            Settings.getThreeDSecureMerchantAccountId(activity)
                    )
                    .enqueue(callback);
        } else {
            DemoApplication
                    .getApiClient(activity)
                    .createTransaction(
                            nonceString,
                            amount,
                            Settings.getMerchantAccountId(activity)
                    )
                    .enqueue(callback);
        }
    }

    private void setStatus(int message) {
        AppCompatActivity activity = (AppCompatActivity) getActivity();
        loadingSpinner.setVisibility(View.GONE);
        activity.setTitle(message);
        TextView status = activity.findViewById(R.id.transaction_status);
        status.setText(message);
        status.setVisibility(View.VISIBLE);
    }

    private void setMessage(String message) {
        AppCompatActivity activity = (AppCompatActivity) getActivity();
        loadingSpinner.setVisibility(View.GONE);
        TextView textView = activity.findViewById(R.id.transaction_message);
        textView.setText(message);
        textView.setVisibility(View.VISIBLE);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            getActivity().finish();
            return true;
        }

        return false;
    }
}
