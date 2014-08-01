package com.braintreepayments.demo;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.braintreepayments.api.Braintree;
import com.braintreepayments.api.Braintree.PaymentMethodNonceListener;
import com.braintreepayments.api.models.CardBuilder;

public class Custom extends BaseActivity implements PaymentMethodNonceListener {

    private EditText mCardNumber;
    private EditText mExpirationDate;
    private Button mPurchaseButton;

    protected void onCreate(Bundle onSaveInstanceState) {
        super.onCreate(onSaveInstanceState);
        setContentView(R.layout.custom);

        mCardNumber = (EditText) findViewById(R.id.card_number);
        mExpirationDate = (EditText) findViewById(R.id.card_expiration_date);
        mPurchaseButton = (Button) findViewById(R.id.purchase_button);
    }

    @Override
    public void ready(String clientToken) {
        mBraintree = Braintree.getInstance(this, clientToken);
        mBraintree.addListener(this);

        mPurchaseButton.setEnabled(true);
    }

    public void onPurchase(View v) {
        CardBuilder cardBuilder = new CardBuilder()
            .cardNumber(mCardNumber.getText().toString())
            .expirationDate(mExpirationDate.getText().toString());

        mBraintree.tokenize(cardBuilder);
    }

    @Override
    public void onPaymentMethodNonce(String nonce) {
        postNonceToServer(nonce);
    }
}
