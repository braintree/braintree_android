package com.braintreepayments.api.dropin;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnKeyListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.braintreepayments.api.Braintree;
import com.braintreepayments.api.dropin.utils.CardType;
import com.braintreepayments.api.dropin.view.BraintreeEditText;
import com.braintreepayments.api.dropin.view.CardEditText;
import com.braintreepayments.api.dropin.view.CardEditText.OnCardTypeChangedListener;
import com.braintreepayments.api.dropin.view.CvvEditText;
import com.braintreepayments.api.dropin.view.LoadingHeader;
import com.braintreepayments.api.dropin.view.MonthYearEditText;
import com.braintreepayments.api.dropin.view.PaymentButton;
import com.braintreepayments.api.dropin.view.PostalCodeEditText;
import com.braintreepayments.api.exceptions.ErrorWithResponse;
import com.braintreepayments.api.exceptions.ErrorWithResponse.BraintreeError;
import com.braintreepayments.api.exceptions.UnexpectedException;
import com.braintreepayments.api.models.CardBuilder;

/**
 * {@link com.braintreepayments.api.dropin.BraintreeViewController} for coordinating the Add Payment Method form.
 * Responsible for managing views and form element bindings associated with adding a payment method.
 */
public class AddPaymentMethodViewController extends BraintreeViewController
        implements OnClickListener, OnCardTypeChangedListener, OnKeyListener, OnFocusChangeListener,
        OnEditorActionListener {

    // @formatter:off
    private static final String EXTRA_CARD_NUMBER_TEXT = "com.braintreepayments.api.dropin.EXTRA_CARD_NUMBER_TEXT";
    private static final String EXTRA_CVV_TEXT = "com.braintreepayments.api.dropin.EXTRA_CVV_TEXT";
    private static final String EXTRA_EXPIRATION_TEXT = "com.braintreepayments.api.dropin.EXTRA_EXPIRATION_TEXT";
    private static final String EXTRA_POSTAL_CODE_TEXT = "com.braintreepayments.api.dropin.EXTRA_POSTAL_CODE_TEXT";
    private static final String EXTRA_FORM_IS_SUBMITTING = "com.braintreepayments.api.dropin.EXTRA_FORM_IS_SUBMITTING";
    private static final String EXTRA_SUBMIT_BUTTON_ENABLED = "com.braintreepayments.api.dropin.EXTRA_SUBMIT_BUTTON_ENABLED";
    // @formatter:on

    private static final String INTEGRATION_METHOD = "dropin";

    /**
     * When adding new views, make sure to update {@link #onSaveInstanceState}, {@link #restoreState(Bundle)}
     * and the proper tests.
     */
    private PaymentButton mPaymentButton;
    private View mDescription;
    private CardEditText mCardNumber;
    private MonthYearEditText mExpirationView;
    private CvvEditText mCvvView;
    private PostalCodeEditText mPostalCode;
    private Button mSubmitButton;

    private LoadingHeader mLoadingHeader;
    private ScrollView mScrollView;

    private boolean mIsSubmitting;

    public AddPaymentMethodViewController(BraintreePaymentActivity activity,
            Bundle savedInstanceState, View root, Braintree braintree, Customization customization) {
        super(activity, root, braintree, customization);
        mIsSubmitting = false;

        initViews();
        restoreState(savedInstanceState);
    }

    private void initViews() {
        mLoadingHeader = findView(R.id.header_container);
        mScrollView = findView(R.id.form_scroll_container);
        mDescription = findView(R.id.description_container);
        mPaymentButton = findView(R.id.payment_button);
        mCardNumber = findView(R.id.card_form_card_number);
        mExpirationView = findView(R.id.card_form_expiration);
        mCvvView = findView(R.id.card_form_cvv);
        mPostalCode = findView(R.id.card_form_postal_code);
        mSubmitButton = findView(R.id.card_form_complete_button);

        mPaymentButton.initialize(getActivity(), getBraintree());

        mCardNumber.setFocusChangeListener(this);
        mExpirationView.setFocusChangeListener(this);
        mCvvView.setFocusChangeListener(this);
        mPostalCode.setFocusChangeListener(this);

        mCardNumber.setOnClickListener(this);
        mExpirationView.setOnClickListener(this);
        mCvvView.setOnClickListener(this);
        mExpirationView.setOnClickListener(this);
        mSubmitButton.setOnClickListener(this);

        mCardNumber.setOnKeyListener(this);
        mExpirationView.setOnKeyListener(this);

        if (getBraintree().isCvvChallenegePresent() || getBraintree().isPostalCodeChallengePresent()) {
            mExpirationView.setImeOptions(EditorInfo.IME_ACTION_NEXT);
        } else {
            mExpirationView.setImeOptions(EditorInfo.IME_ACTION_GO);
            mExpirationView.setImeActionLabel(getCustomizedCallToAction(), EditorInfo.IME_ACTION_GO);
            mExpirationView.setOnEditorActionListener(this);
        }

        if (getBraintree().isCvvChallenegePresent()) {
            mCvvView.setOnKeyListener(this);
            if (getBraintree().isPostalCodeChallengePresent()) {
                mCvvView.setImeOptions(EditorInfo.IME_ACTION_NEXT);
            } else {
                mCvvView.setImeOptions(EditorInfo.IME_ACTION_GO);
                mCvvView.setImeActionLabel(getCustomizedCallToAction(), EditorInfo.IME_ACTION_GO);
                mCvvView.setOnEditorActionListener(this);
            }
        } else {
            mCvvView.setVisibility(View.GONE);
        }

        if (getBraintree().isPostalCodeChallengePresent()) {
            mPostalCode.setOnKeyListener(this);
            mPostalCode.setImeOptions(EditorInfo.IME_ACTION_GO);
            mPostalCode.setImeActionLabel(getCustomizedCallToAction(), EditorInfo.IME_ACTION_GO);
            mPostalCode.setOnEditorActionListener(this);
        } else {
            mPostalCode.setVisibility(View.GONE);
        }

        mCardNumber.setOnCardTypeChangedListener(this);
        mSubmitButton.setText(getSubmitButtonText());
    }

    private void restoreState(Bundle savedInstanceState) {
        restoreText(savedInstanceState, mCardNumber, EXTRA_CARD_NUMBER_TEXT);
        restoreText(savedInstanceState, mCvvView, EXTRA_CVV_TEXT);
        restoreText(savedInstanceState, mExpirationView, EXTRA_EXPIRATION_TEXT);
        restoreText(savedInstanceState, mPostalCode, EXTRA_POSTAL_CODE_TEXT);

        if (savedInstanceState.containsKey(EXTRA_FORM_IS_SUBMITTING)) {
            mIsSubmitting = savedInstanceState.getBoolean(EXTRA_FORM_IS_SUBMITTING);
            if (mIsSubmitting) {
                setUIForSubmit();
            }
        }

        if (savedInstanceState.containsKey(EXTRA_SUBMIT_BUTTON_ENABLED)) {
            mSubmitButton.setEnabled(savedInstanceState.getBoolean(EXTRA_SUBMIT_BUTTON_ENABLED));
        }

        if (areFieldsValid()) {
            mSubmitButton.setBackgroundColor(
                    getActivity().getResources().getColor(R.color.bt_blue));
        }
    }

    private void restoreText(Bundle savedInstanceState, TextView view, String extra) {
        if (savedInstanceState.containsKey(extra)) {
            view.setText(savedInstanceState.getCharSequence(extra));
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putCharSequence(EXTRA_CARD_NUMBER_TEXT, mCardNumber.getText());
        outState.putCharSequence(EXTRA_CVV_TEXT, mCvvView.getText());
        outState.putCharSequence(EXTRA_EXPIRATION_TEXT, mExpirationView.getText());
        outState.putCharSequence(EXTRA_POSTAL_CODE_TEXT, mPostalCode.getText());
        outState.putBoolean(EXTRA_FORM_IS_SUBMITTING, mIsSubmitting);
        outState.putBoolean(EXTRA_SUBMIT_BUTTON_ENABLED, mSubmitButton.isEnabled());
    }

    @Override
    public void onClick(View v) {
        if (v == mSubmitButton) {
            if (areFieldsValid()) {
                startSubmit();
                getBraintree().create(getCardBuilder());
            } else {
                showClientErrors();
            }
        } else {
            scrollToView(v);
        }
    }

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        if (hasFocus) {
            scrollToView(v);
        }
    }

    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        if (!mIsSubmitting) {
            if(areFieldsValid()) {
                mSubmitButton.setBackgroundColor(
                        getActivity().getResources().getColor(R.color.bt_blue));
            }
        }

        return false;
    }

    private CardBuilder getCardBuilder() {
        CardBuilder cardBuilder = new CardBuilder()
                .cardNumber(mCardNumber.getText().toString())
                .expirationMonth(mExpirationView.getMonth())
                .expirationYear(mExpirationView.getYear())
                .integration(INTEGRATION_METHOD);

        if (getBraintree().isCvvChallenegePresent()) {
            cardBuilder.cvv(mCvvView.getText().toString());
        }
        if (getBraintree().isPostalCodeChallengePresent()) {
            cardBuilder.postalCode(mPostalCode.getText().toString());
        }

        return cardBuilder;
    }

    public void onPaymentResult(int requestCode, int resultCode, Intent data) {
        mIsSubmitting = true;
        mPaymentButton.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onCardTypeChanged(CardType cardType) {
        mCvvView.setCardType(cardType);
    }

    private boolean areFieldsValid() {
        boolean valid = mCardNumber.isValid();
        valid = valid && mExpirationView.isValid();

        if (getBraintree().isCvvChallenegePresent()) {
            valid = valid && mCvvView.isValid();
        }

        if (getBraintree().isPostalCodeChallengePresent()) {
            valid = valid && mPostalCode.isValid();
        }

        return valid;
    }

    private void showClientErrors() {
        mCardNumber.validate();
        mExpirationView.validate();

        if (getBraintree().isCvvChallenegePresent()) {
            mCvvView.validate();
        }

        if (getBraintree().isPostalCodeChallengePresent()) {
            mPostalCode.validate();
        }

        mSubmitButton.setBackgroundColor(getActivity().getResources().getColor(R.color.bt_button_disabled_color));
    }

    public void setErrors(ErrorWithResponse error) {
        showErrorUI();

        endSubmit();

        boolean focusSet = false;
        if(error.errorFor("creditCard") != null) {
            getBraintree().sendAnalyticsEvent(
                    BraintreePaymentActivity.ANALYTICS_PREFIX + ".add-card.failed",
                    BraintreePaymentActivity.INTEGRATION_METHOD);

            BraintreeError cardErrors = error.errorFor("creditCard");
            if(cardErrors.errorFor("number") != null) {
                focusSet = setError(mCardNumber, focusSet);
            }
            if(cardErrors.errorFor("expirationYear") != null ||
                    cardErrors.errorFor("expirationMonth") != null ||
                    cardErrors.errorFor("expirationDate") != null ) {
                focusSet = setError(mExpirationView, focusSet);
            }
            if (cardErrors.errorFor("cvv") != null) {
                focusSet = setError(mCvvView, focusSet);
            }
            if(cardErrors.errorFor("billingAddress") != null) {
                setError(mPostalCode, focusSet);
            }
        } else {
            getActivity().onUnrecoverableError(new UnexpectedException(error.getMessage()));
        }
    }

    private void showErrorUI() {
        mLoadingHeader.setError(getActivity().getString(R.string.invalid_card));
    }

    protected boolean isSubmitting() {
        return mIsSubmitting;
    }

    protected void endSubmit() {
        mSubmitButton.setBackgroundColor(getActivity().getResources().getColor(R.color.bt_button_disabled_color));
        mCardNumber.setEnabled(true);
        mExpirationView.setEnabled(true);
        mCvvView.setEnabled(true);
        mPostalCode.setEnabled(true);
        mSubmitButton.setEnabled(true);
        mIsSubmitting = false;
    }

    private void startSubmit() {
        InputMethodManager inputMethodManager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(mCardNumber.getWindowToken(), 0);

        mIsSubmitting = true;
        setUIForSubmit();
    }

    private void setUIForSubmit() {
        mCardNumber.setEnabled(false);
        mExpirationView.setEnabled(false);
        mCvvView.setEnabled(false);
        mPostalCode.setEnabled(false);
        mSubmitButton.setEnabled(false);

        mDescription.setVisibility(View.GONE);
        mLoadingHeader.setLoading();
    }

    protected void showSuccess() {
        mLoadingHeader.setSuccessful();
    }

    private boolean setError(BraintreeEditText editText, boolean focusSet) {
        editText.setError();
        if(!focusSet) {
            editText.requestFocus();
            InputMethodManager inputMethodManager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            inputMethodManager.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT);
        }
        return true;
    }

    private void scrollToView(final View v) {
        mScrollView.postDelayed(new Runnable() {
            public void run() {
                mScrollView.smoothScrollTo(0, v.getTop());
            }
        }, 100);
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (actionId == EditorInfo.IME_ACTION_GO) {
            mSubmitButton.performClick();
            return true;
        }
        return false;
    }
}
