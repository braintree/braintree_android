package com.braintreepayments.demo;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.braintreepayments.api.BraintreeFragment;
import com.braintreepayments.api.BraintreePaymentActivity;
import com.braintreepayments.api.UnionPay;
import com.braintreepayments.api.exceptions.InvalidArgumentException;
import com.braintreepayments.api.interfaces.UnionPayListener;
import com.braintreepayments.api.models.PaymentMethodNonce;
import com.braintreepayments.api.models.UnionPayCapabilities;
import com.braintreepayments.api.models.UnionPayCardBuilder;

public class UnionPayActivity extends BaseActivity implements UnionPayListener, OnFocusChangeListener, OnClickListener {

    private static final String EXTRA_UNIONPAY_CARD_BUILDER = "com.braintreepayments.api.EXTRA_UNIONPAY_CARD_BUILDER";
    private static final String EXTRA_UNIONPAY_CAPABILITIES = "com.braintreepayments.api.EXTRA_UNIONPAY_CAPABILITIES";
    private static final String EXTRA_UNIONPAY_ENROLLMENT_ID = "com.braintreepayments.api.EXTRA_UNIONPAY_ENROLLMENT_ID";

    private EditText mCreditCard;
    private EditText mExpirationMonth;
    private EditText mExpirationYear;
    private EditText mCvv;
    private View mEnrollmentLayout;
    private EditText mCountryCode;
    private EditText mMobilePhoneNumber;
    private EditText mSmsCode;
    private View mSmsLayout;
    private Button mEnroll;
    private Button mSubmit;

    private UnionPayCardBuilder mUnionPayCardBuilder = new UnionPayCardBuilder();
    private UnionPayCapabilities mUnionPayCapabilities;
    private String mEnrollmentId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            mUnionPayCardBuilder = savedInstanceState.getParcelable(EXTRA_UNIONPAY_CARD_BUILDER);
            mUnionPayCapabilities = savedInstanceState.getParcelable(EXTRA_UNIONPAY_CAPABILITIES);
            mEnrollmentId = savedInstanceState.getString(EXTRA_UNIONPAY_ENROLLMENT_ID);
        }

        setContentView(R.layout.unionpay_activity);

        mCreditCard = (EditText)findViewById(R.id.card_number);
        mExpirationMonth = (EditText)findViewById(R.id.expiration_month);
        mExpirationYear = (EditText)findViewById(R.id.expiration_year);
        mCvv = (EditText)findViewById(R.id.cvv);
        mEnrollmentLayout = findViewById(R.id.enrollment_layout);
        mCountryCode = (EditText)findViewById(R.id.country_code);
        mMobilePhoneNumber = (EditText)findViewById(R.id.mobile_phone);
        mSmsCode = (EditText)findViewById(R.id.sms_code);
        mSmsLayout = findViewById(R.id.sms_layout);
        mEnroll = (Button)findViewById(R.id.enroll_button);
        mSubmit = (Button)findViewById(R.id.submit_button);

        EditText[] editTexts = new EditText[]{
                mCreditCard,
                mExpirationMonth,
                mExpirationYear,
                mCvv,
                mCountryCode,
                mMobilePhoneNumber,
                mSmsCode
        };

        for(final EditText editText : editTexts){
            editText.setFocusableInTouchMode(true);
            editText.setFocusable(true);
            editText.setOnFocusChangeListener(new OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if(!hasFocus) {
                        onEntry(editText, editText.getText().toString());
                    }
                }
            });
        }

        mSubmit.setOnClickListener(this);
        mEnroll.setOnClickListener(this);

        if (mUnionPayCapabilities != null) {
            onCapabilitiesFetched(mUnionPayCapabilities);
        }

        if (mEnrollmentId != null) {
            onSmsCodeSent(mEnrollmentId);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(EXTRA_UNIONPAY_CARD_BUILDER, mUnionPayCardBuilder);
        outState.putParcelable(EXTRA_UNIONPAY_CAPABILITIES, mUnionPayCapabilities);
        outState.putString(EXTRA_UNIONPAY_ENROLLMENT_ID, mEnrollmentId);
    }

    @Override
    protected void reset() {
        enableButtons(false);
    }

    @Override
    protected void onAuthorizationFetched() {
        try {
            mBraintreeFragment = BraintreeFragment.newInstance(this, mAuthorization);
        } catch (InvalidArgumentException e) {
            onError(e);
        }
        enableButtons(true);
    }

    private void enableButtons(boolean enabled) {
        mCreditCard.setEnabled(enabled);
        mExpirationMonth.setEnabled(enabled);
        mExpirationYear.setEnabled(enabled);
        mCvv.setEnabled(enabled);
        mCountryCode.setEnabled(enabled);
        mMobilePhoneNumber.setEnabled(enabled);
        mSmsCode.setEnabled(enabled);
        mEnroll.setEnabled(enabled);
        mSubmit.setEnabled(enabled);
    }
    private void show(View view, boolean show) {
        view.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void onEntry(TextView tv, String text) {
        switch(tv.getId()) {
           case R.id.card_number:
               mUnionPayCardBuilder.cardNumber(text);
               UnionPay.fetchCapabilities(mBraintreeFragment, text);
               break;
           case R.id.expiration_month:
               mUnionPayCardBuilder.expirationMonth(text);
               break;
           case R.id.expiration_year:
               mUnionPayCardBuilder.expirationYear(text);
               break;
           case R.id.cvv:
               mUnionPayCardBuilder.cvv(text);
               break;
           case R.id.country_code:
               mUnionPayCardBuilder.mobileCountryCode(text);
               break;
           case R.id.mobile_phone:
               mUnionPayCardBuilder.mobilePhoneNumber(text);
               break;
           case R.id.sms_code:
               mUnionPayCardBuilder.smsCode(text);
               break;
        }
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()) {
            case R.id.enroll_button:
                mUnionPayCardBuilder.mobilePhoneNumber(mMobilePhoneNumber.getText().toString());
                UnionPay.enroll(mBraintreeFragment, mUnionPayCardBuilder);
                break;
            case R.id.submit_button:
                UnionPay.tokenize(mBraintreeFragment, mUnionPayCardBuilder);
                break;
        }
    }

    @Override
    public void onCapabilitiesFetched(UnionPayCapabilities capabilities) {
        mUnionPayCapabilities = capabilities;
        show(mEnrollmentLayout, mUnionPayCapabilities.isUnionPayEnrollmentRequired());
    }

    @Override
    public void onSmsCodeSent(String enrollmentId) {
        mEnrollmentId = enrollmentId;
        mUnionPayCardBuilder.enrollmentId(enrollmentId);
        show(mSmsLayout, true);
    }

    @Override
    public void onPaymentMethodNonceCreated(PaymentMethodNonce paymentMethodNonce) {
        super.onPaymentMethodNonceCreated(paymentMethodNonce);

        Intent intent = new Intent()
                .putExtra(BraintreePaymentActivity.EXTRA_PAYMENT_METHOD_NONCE, paymentMethodNonce);
        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        if(v instanceof EditText) {
            EditText editText = (EditText)v;
            onEntry(editText, editText.getText().toString());
        }
    }
}
