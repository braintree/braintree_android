package com.braintreepayments.api;

import android.content.Intent;
import android.os.Bundle;

import com.google.android.gms.wallet.AutoResolveHelper;
import com.google.android.gms.wallet.PaymentDataRequest;
import com.google.android.gms.wallet.PaymentsClient;
import com.google.android.gms.wallet.Wallet;
import com.google.android.gms.wallet.WalletConstants;

import androidx.appcompat.app.AppCompatActivity;

public class GooglePayActivity extends AppCompatActivity {

    protected static final String EXTRA_ENVIRONMENT = "com.braintreepayments.api.EXTRA_ENVIRONMENT";
    protected static final String EXTRA_PAYMENT_DATA_REQUEST = "com.braintreepayments.api.EXTRA_PAYMENT_DATA_REQUEST";

    private static final String EXTRA_RECREATING = "com.braintreepayments.api.EXTRA_RECREATING";

    private static final int REQUEST_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null && savedInstanceState.getBoolean(EXTRA_RECREATING)) {
            return;
        }

        PaymentsClient paymentsClient = Wallet.getPaymentsClient(this, new Wallet.WalletOptions.Builder()
                .setEnvironment(getIntent().getIntExtra(EXTRA_ENVIRONMENT, WalletConstants.ENVIRONMENT_TEST))
                .build());

        PaymentDataRequest request = getIntent().getParcelableExtra(EXTRA_PAYMENT_DATA_REQUEST);
        AutoResolveHelper.resolveTask(paymentsClient.loadPaymentData(request), this, REQUEST_CODE);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(EXTRA_RECREATING, true);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        setResult(resultCode, data);
        finish();
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }
}