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

import com.braintreepayments.api.CardNonce;
import com.braintreepayments.api.PaymentMethodNonce;
import com.braintreepayments.demo.models.Transaction;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class CreateTransactionFragment extends Fragment {

    public static final String EXTRA_PAYMENT_METHOD_NONCE = "nonce";

    private ProgressBar mLoadingSpinner;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_create_transaction, container, false);
        mLoadingSpinner = view.findViewById(R.id.loading_spinner);

        AppCompatActivity activity = (AppCompatActivity) getActivity();
        activity.setTitle(R.string.processing_transaction);

        ActionBar actionBar = activity.getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        CreateTransactionFragmentArgs args = CreateTransactionFragmentArgs.fromBundle(getArguments());
        sendNonceToServer(args.getPaymentMethodNonce());

        return view;
    }

    private void sendNonceToServer(PaymentMethodNonce nonce) {
        Callback<Transaction> callback = new Callback<Transaction>() {
            @Override
            public void success(Transaction transaction, Response response) {
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
            }

            @Override
            public void failure(RetrofitError error) {
                setStatus(R.string.transaction_failed);
                setMessage("Unable to create a transaction. Response Code: " +
                        error.getResponse().getStatus() + " Response body: " +
                        error.getResponse().getBody());
            }
        };

        AppCompatActivity activity = (AppCompatActivity) getActivity();
        if (Settings.isThreeDSecureEnabled(activity) && Settings.isThreeDSecureRequired(activity)) {
            DemoApplication.getApiClient(activity).createTransaction(nonce.getNonce(),
                    Settings.getThreeDSecureMerchantAccountId(activity), true, callback);
        } else if (Settings.isThreeDSecureEnabled(activity)) {
            DemoApplication.getApiClient(activity).createTransaction(nonce.getNonce(),
                    Settings.getThreeDSecureMerchantAccountId(activity), callback);
        } else if (nonce instanceof CardNonce && ((CardNonce) nonce).getCardType().equals("UnionPay")) {
            DemoApplication.getApiClient(activity).createTransaction(nonce.getNonce(),
                    Settings.getUnionPayMerchantAccountId(activity), callback);
        } else {
            DemoApplication.getApiClient(activity).createTransaction(nonce.getNonce(), Settings.getMerchantAccountId(activity),
                    callback);
        }
    }

    private void setStatus(int message) {
        AppCompatActivity activity = (AppCompatActivity) getActivity();
        mLoadingSpinner.setVisibility(View.GONE);
        activity.setTitle(message);
        TextView status = activity.findViewById(R.id.transaction_status);
        status.setText(message);
        status.setVisibility(View.VISIBLE);
    }

    private void setMessage(String message) {
        AppCompatActivity activity = (AppCompatActivity) getActivity();
        mLoadingSpinner.setVisibility(View.GONE);
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
