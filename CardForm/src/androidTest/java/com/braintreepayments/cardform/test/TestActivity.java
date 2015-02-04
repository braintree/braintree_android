package com.braintreepayments.cardform.test;

import android.app.Activity;
import android.os.Bundle;
import android.widget.FrameLayout;

import com.braintreepayments.cardform.view.CardForm;

public class TestActivity extends Activity {

    public static final String CREDIT_CARD = "credit_card";
    public static final String EXPIRATION = "expiration";
    public static final String CVV = "cvv";
    public static final String POSTAL_CODE = "postal_code";

    private CardForm mCardForm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mCardForm = new CardForm(this);
        mCardForm.setId(android.R.id.custom);
        mCardForm.setRequiredFields(
                getIntent().getBooleanExtra(CREDIT_CARD, true),
                getIntent().getBooleanExtra(EXPIRATION, true),
                getIntent().getBooleanExtra(CVV, true),
                getIntent().getBooleanExtra(POSTAL_CODE, true),
                "Purchase");
        mCardForm.onRestoreInstanceState(savedInstanceState);
        ((FrameLayout) findViewById(android.R.id.content)).addView(mCardForm);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mCardForm.onSaveInstanceState(outState);
    }

}
